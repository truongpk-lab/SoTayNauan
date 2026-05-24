package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CookingRepository {
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)\\s*phút", Pattern.CASE_INSENSITIVE);
    private static final int ONE_MINUTE_SECONDS = 60;

    private final CookingLocalDataSource localDataSource;
    private final RecipeRepository recipeRepository;
    private final ShoppingRepository shoppingRepository;

    public CookingRepository(CookingLocalDataSource localDataSource,
                             RecipeRepository recipeRepository) {
        this(localDataSource, recipeRepository, null);
    }

    public CookingRepository(CookingLocalDataSource localDataSource,
                             RecipeRepository recipeRepository,
                             ShoppingRepository shoppingRepository) {
        this.localDataSource = localDataSource;
        this.recipeRepository = recipeRepository;
        this.shoppingRepository = shoppingRepository;
    }

    public CookingSessionState startSession(long recipeId) {
        localDataSource.startSession(recipeId);
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        return createState(recipe, 0, false,
                recipe == null
                        ? "Không tìm thấy công thức để bắt đầu nấu."
                        : "Đã tạo phiên nấu local cho " + recipe.getName() + ".");
    }

    public CookingSessionState getActiveSession(long fallbackRecipeId) {
        long recipeId = localDataSource.getActiveRecipeId();
        if (recipeId <= 0) {
            recipeId = fallbackRecipeId;
        }
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        return createState(recipe, safeStepIndex(recipe, localDataSource.getCurrentStepIndex()),
                localDataSource.isCompleted(),
                recipe == null
                        ? "Chưa có phiên nấu đang hoạt động."
                        : "Phiên nấu local đã sẵn sàng.");
    }

    public CookingSessionState completeCurrentStep() {
        Recipe recipe = recipeRepository.findRecipe(localDataSource.getActiveRecipeId());
        if (recipe == null) {
            return createState(null, 0, false, "Không tìm thấy công thức đang nấu.");
        }
        if (localDataSource.isCompleted()) {
            return createState(recipe, safeStepIndex(recipe, localDataSource.getCurrentStepIndex()),
                    true, "Món này đã hoàn thành, tồn bếp đã được cập nhật trước đó.");
        }
        int currentIndex = safeStepIndex(recipe, localDataSource.getCurrentStepIndex());
        int nextIndex = currentIndex + 1;
        boolean completed = nextIndex >= recipe.getSteps().size();
        if (completed) {
            nextIndex = Math.max(0, recipe.getSteps().size() - 1);
        }
        localDataSource.updateStep(nextIndex, completed);
        String inventoryMessage = "";
        if (completed && shoppingRepository != null) {
            inventoryMessage = " " + shoppingRepository
                    .consumeIngredientsForCookedRecipe(recipe.getIngredients(), recipe.getName())
                    .getStatusMessage();
        }
        return createState(recipe, nextIndex, completed,
                completed
                        ? "Bạn đã hoàn thành tất cả các bước." + inventoryMessage
                        : "Đã chuyển sang bước " + (nextIndex + 1) + ".");
    }

    public CookingSessionState replayCurrentInstruction() {
        Recipe recipe = recipeRepository.findRecipe(localDataSource.getActiveRecipeId());
        if (recipe == null) {
            return createState(null, 0, false, "Không tìm thấy hướng dẫn để đọc lại.");
        }
        int currentIndex = safeStepIndex(recipe, localDataSource.getCurrentStepIndex());
        return createState(recipe, currentIndex, localDataSource.isCompleted(),
                "Đọc lại: " + recipe.getSteps().get(currentIndex));
    }

    public CookingTimerState prepareCurrentStepTimer(long fallbackRecipeId) {
        CookingSessionState sessionState = getActiveSession(fallbackRecipeId);
        if (!sessionState.hasRecipe() || sessionState.isCompleted()) {
            return createTimerState(-1L, 0, 0, 0, false,
                    "Chưa có bước nấu cần hẹn giờ.");
        }
        long recipeId = sessionState.getRecipe().getId();
        int stepIndex = sessionState.getCurrentStepIndex();
        int totalSeconds = Math.max(ONE_MINUTE_SECONDS, sessionState.getCurrentStepSeconds());
        if (!localDataSource.hasTimerFor(recipeId, stepIndex)) {
            localDataSource.saveTimer(recipeId, stepIndex, totalSeconds, totalSeconds,
                    true, System.currentTimeMillis());
        }
        return getTimerState();
    }

    public CookingTimerState getTimerState() {
        long recipeId = localDataSource.getTimerRecipeId();
        int stepIndex = localDataSource.getTimerStepIndex();
        int totalSeconds = localDataSource.getTimerTotalSeconds();
        int remainingSeconds = calculateLiveRemainingSeconds();
        boolean running = localDataSource.isTimerRunning() && remainingSeconds > 0;
        if (localDataSource.isTimerRunning() && remainingSeconds <= 0) {
            localDataSource.markTimerExpired(recipeId, stepIndex, totalSeconds);
        }
        return createTimerState(recipeId, stepIndex, totalSeconds, remainingSeconds, running,
                createTimerMessage(remainingSeconds, running));
    }

    public CookingTimerState pauseTimer() {
        long recipeId = localDataSource.getTimerRecipeId();
        int stepIndex = localDataSource.getTimerStepIndex();
        int totalSeconds = localDataSource.getTimerTotalSeconds();
        int remainingSeconds = calculateLiveRemainingSeconds();
        localDataSource.saveTimer(recipeId, stepIndex, totalSeconds, remainingSeconds, false, 0L);
        return createTimerState(recipeId, stepIndex, totalSeconds, remainingSeconds, false,
                "Timer đã tạm dừng. Bạn có thể tiếp tục khi sẵn sàng.");
    }

    public CookingTimerState resumeTimer() {
        long recipeId = localDataSource.getTimerRecipeId();
        int stepIndex = localDataSource.getTimerStepIndex();
        int totalSeconds = localDataSource.getTimerTotalSeconds();
        int remainingSeconds = Math.max(1, calculateLiveRemainingSeconds());
        localDataSource.saveTimer(recipeId, stepIndex, totalSeconds, remainingSeconds, true,
                System.currentTimeMillis());
        return createTimerState(recipeId, stepIndex, totalSeconds, remainingSeconds, true,
                createTimerMessage(remainingSeconds, true));
    }

    public CookingTimerState addOneMinuteToTimer() {
        return addMinutesToTimer(1);
    }

    public CookingTimerState addMinutesToTimer(int minutesToAdd) {
        int secondsToAdd = Math.max(1, minutesToAdd) * ONE_MINUTE_SECONDS;
        long recipeId = localDataSource.getTimerRecipeId();
        int stepIndex = localDataSource.getTimerStepIndex();
        int totalSeconds = localDataSource.getTimerTotalSeconds() + secondsToAdd;
        int remainingSeconds = calculateLiveRemainingSeconds() + secondsToAdd;
        localDataSource.saveTimer(recipeId, stepIndex, totalSeconds, remainingSeconds, true,
                System.currentTimeMillis());
        return createTimerState(recipeId, stepIndex, totalSeconds, remainingSeconds, true,
                "Đã thêm " + Math.max(1, minutesToAdd) + " phút để bạn kiểm tra món kỹ hơn.");
    }

    public CookingTimerState acknowledgeTimerAlarm() {
        localDataSource.acknowledgeTimerAlarm();
        return getTimerState();
    }

    public CookingSessionState completeStepAfterTimer() {
        localDataSource.acknowledgeTimerAlarm();
        return completeCurrentStep();
    }

    private CookingSessionState createState(Recipe recipe, int stepIndex, boolean completed, String statusMessage) {
        return new CookingSessionState(recipe, stepIndex, estimateStepSeconds(recipe, stepIndex),
                completed, localDataSource.getUpdatedAt(), statusMessage);
    }

    private CookingTimerState createTimerState(long recipeId, int stepIndex, int totalSeconds,
                                               int remainingSeconds, boolean running,
                                               String assistantMessage) {
        return new CookingTimerState(recipeId, stepIndex, Math.max(0, totalSeconds),
                Math.max(0, remainingSeconds), running, remainingSeconds <= 0 && totalSeconds > 0,
                localDataSource.isTimerAlarmAcknowledged(),
                assistantMessage);
    }

    private int calculateLiveRemainingSeconds() {
        int remainingSeconds = localDataSource.getTimerRemainingSeconds();
        if (!localDataSource.isTimerRunning()) {
            return Math.max(0, remainingSeconds);
        }
        long startedAt = localDataSource.getTimerStartedAt();
        if (startedAt <= 0L) {
            return Math.max(0, remainingSeconds);
        }
        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
        return Math.max(0, remainingSeconds - (int) elapsedSeconds);
    }

    private String createTimerMessage(int remainingSeconds, boolean running) {
        if (remainingSeconds <= 0) {
            return "Hết giờ. Hãy kiểm tra món ăn trước khi xác nhận bước tiếp theo.";
        }
        if (remainingSeconds <= ONE_MINUTE_SECONDS) {
            return "Còn 1 phút, hãy chuẩn bị kiểm tra món ăn nhé.";
        }
        if (!running) {
            return "Timer đang tạm dừng. Bấm tiếp tục để chạy lại.";
        }
        return "Timer đang chạy. App sẽ không tự chuyển bước nấu.";
    }

    private int estimateStepSeconds(Recipe recipe, int stepIndex) {
        if (recipe == null || recipe.getSteps().isEmpty()) {
            return 0;
        }
        String step = recipe.getSteps().get(safeStepIndex(recipe, stepIndex));
        Matcher matcher = MINUTE_PATTERN.matcher(step);
        if (matcher.find()) {
            return Math.max(30, Integer.parseInt(matcher.group(1)) * 60);
        }
        int stepCount = Math.max(1, recipe.getSteps().size());
        return Math.max(60, (recipe.getTotalMinutes() * 60) / stepCount);
    }

    private int safeStepIndex(Recipe recipe, int stepIndex) {
        if (recipe == null || recipe.getSteps().isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(stepIndex, recipe.getSteps().size() - 1));
    }
}
