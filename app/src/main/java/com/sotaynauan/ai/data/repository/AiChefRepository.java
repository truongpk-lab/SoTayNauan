package com.sotaynauan.ai.data.repository;

import android.graphics.Color;

import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.AiChefFeature;
import com.sotaynauan.ai.data.model.AiRecipeSuggestionState;
import com.sotaynauan.ai.data.model.AiChefState;
import com.sotaynauan.ai.data.model.ConfirmedIngredient;
import com.sotaynauan.ai.data.model.IngredientConfirmState;
import com.sotaynauan.ai.data.model.IngredientInputState;
import com.sotaynauan.ai.data.model.MatchDetailState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AiChefRepository {
    public static final String FLOW_INGREDIENT_MATCH = "ingredient_match";
    public static final String FLOW_SCAN_INGREDIENTS = "scan_ingredients";
    public static final String FLOW_CREATE_RECIPE = "create_recipe";
    public static final String FLOW_WEEKLY_MENU = "weekly_menu";
    public static final String FLOW_COOKING_TIPS = "cooking_tips";

    private final AiChefLocalDataSource localDataSource;
    private final RecipeRepository recipeRepository;
    private final AiBackendRemoteDataSource aiBackendRemoteDataSource;
    private final List<AiChefFeature> features;

    public AiChefRepository(AiChefLocalDataSource localDataSource) {
        this(localDataSource, null);
    }

    public AiChefRepository(AiChefLocalDataSource localDataSource, RecipeRepository recipeRepository) {
        this(localDataSource, recipeRepository, null);
    }

    public AiChefRepository(AiChefLocalDataSource localDataSource,
                            RecipeRepository recipeRepository,
                            AiBackendRemoteDataSource aiBackendRemoteDataSource) {
        this.localDataSource = localDataSource;
        this.recipeRepository = recipeRepository;
        this.aiBackendRemoteDataSource = aiBackendRemoteDataSource;
        this.features = createFeatures();
    }

    public List<AiChefFeature> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    public AiChefFeature findFeature(String featureId) {
        for (AiChefFeature feature : features) {
            if (feature.getId().equals(featureId)) {
                return feature;
            }
        }
        return null;
    }

    public AiChefState getState() {
        return localDataSource.getState();
    }

    public AiChefState selectFeature(String featureId) {
        return localDataSource.selectFeature(featureId);
    }

    public IngredientInputState getIngredientInputState() {
        return localDataSource.getIngredientInputState();
    }

    public List<String> getIngredientSuggestions() {
        return Arrays.asList("Tỏi", "Nước mắm", "Đường", "Hành tây");
    }

    public IngredientInputState addIngredient(String ingredient) {
        String normalized = normalizeIngredient(ingredient);
        List<String> ingredients = new ArrayList<>(localDataSource.getIngredientInputState().getIngredients());
        if (normalized.isEmpty()) {
            return localDataSource.saveIngredients(ingredients,
                    "Nhập tên nguyên liệu rồi bấm + để thêm vào rổ.");
        }
        if (containsIngredient(ingredients, normalized)) {
            return localDataSource.saveIngredients(ingredients,
                    normalized + " đã có trong rổ nguyên liệu.");
        }
        ingredients.add(normalized);
        return localDataSource.saveIngredients(ingredients,
                "Đã thêm " + normalized + " và lưu danh sách trên thiết bị.");
    }

    public IngredientInputState removeIngredient(String ingredient) {
        List<String> ingredients = new ArrayList<>(localDataSource.getIngredientInputState().getIngredients());
        String normalized = normalizeIngredient(ingredient);
        for (int index = 0; index < ingredients.size(); index++) {
            if (ingredients.get(index).equalsIgnoreCase(normalized)) {
                ingredients.remove(index);
                return localDataSource.saveIngredients(ingredients,
                        "Đã xóa " + normalized + " khỏi rổ nguyên liệu local.");
            }
        }
        return localDataSource.saveIngredients(ingredients,
                "Rổ nguyên liệu đã được cập nhật.");
    }

    public IngredientInputState addOfflineScanIngredients() {
        List<String> current = new ArrayList<>(localDataSource.getIngredientInputState().getIngredients());
        Set<String> merged = new LinkedHashSet<>(current);
        merged.add("Trứng gà");
        merged.add("Cà chua");
        merged.add("Hành lá");
        return localDataSource.saveIngredients(new ArrayList<>(merged),
                "Đã quét offline mẫu và thêm nguyên liệu nhận diện vào local storage.");
    }

    public IngredientInputState markIngredientsReadyForSuggestions() {
        List<String> ingredients = new ArrayList<>(localDataSource.getIngredientInputState().getIngredients());
        selectFeature(FLOW_INGREDIENT_MATCH);
        return localDataSource.saveIngredients(ingredients,
                "Đã lưu " + ingredients.size() + " nguyên liệu. Mở màn xác nhận để chỉnh lại trước khi tìm món.");
    }

    public IngredientConfirmState prepareIngredientConfirmation() {
        IngredientConfirmState currentState = localDataSource.getIngredientConfirmState();
        List<String> inputIngredients = localDataSource.getIngredientInputState().getIngredients();
        if (currentState.getTotalCount() > 0 && hasSameIngredientNames(currentState, inputIngredients)) {
            return currentState;
        }
        if (inputIngredients.isEmpty() && currentState.getTotalCount() > 0) {
            return currentState;
        }
        List<ConfirmedIngredient> nextIngredients = new ArrayList<>();
        for (String ingredient : inputIngredients) {
            nextIngredients.add(toConfirmedIngredient(ingredient, nextIngredients.size()));
        }
        if (nextIngredients.isEmpty()) {
            nextIngredients.add(new ConfirmedIngredient(createIngredientId(0),
                    "Trứng gà", "4", ConfirmedIngredient.SOURCE_CAMERA, true));
            nextIngredients.add(new ConfirmedIngredient(createIngredientId(1),
                    "Cà chua", "2", ConfirmedIngredient.SOURCE_KEYBOARD, true));
            nextIngredients.add(new ConfirmedIngredient(createIngredientId(2),
                    "Thịt bò", "300g", ConfirmedIngredient.SOURCE_KITCHEN, true));
        }
        return localDataSource.saveConfirmedIngredients(nextIngredients,
                "Danh sách xác nhận đã sẵn sàng từ nguyên liệu local.");
    }

    public IngredientConfirmState getIngredientConfirmState() {
        return localDataSource.getIngredientConfirmState();
    }

    public IngredientConfirmState toggleConfirmedIngredient(String ingredientId, boolean selected) {
        List<ConfirmedIngredient> ingredients = new ArrayList<>(
                localDataSource.getIngredientConfirmState().getIngredients());
        for (int index = 0; index < ingredients.size(); index++) {
            ConfirmedIngredient ingredient = ingredients.get(index);
            if (ingredient.getId().equals(ingredientId)) {
                ingredients.set(index, ingredient.withSelection(selected));
                return localDataSource.saveConfirmedIngredients(ingredients,
                        (selected ? "Đã chọn " : "Đã bỏ chọn ") + ingredient.getName() + ".");
            }
        }
        return localDataSource.saveConfirmedIngredients(ingredients,
                "Danh sách xác nhận đã được cập nhật.");
    }

    public IngredientConfirmState updateConfirmedIngredient(String ingredientId,
                                                            String name,
                                                            String quantity) {
        String normalizedName = normalizeIngredient(name);
        String normalizedQuantity = normalizeQuantity(quantity);
        List<ConfirmedIngredient> ingredients = new ArrayList<>(
                localDataSource.getIngredientConfirmState().getIngredients());
        if (normalizedName.isEmpty()) {
            return localDataSource.saveConfirmedIngredients(ingredients,
                    "Tên nguyên liệu không được để trống.");
        }
        for (int index = 0; index < ingredients.size(); index++) {
            ConfirmedIngredient ingredient = ingredients.get(index);
            if (ingredient.getId().equals(ingredientId)) {
                ingredients.set(index, ingredient.withContent(normalizedName, normalizedQuantity));
                syncInputIngredients(ingredients);
                return localDataSource.saveConfirmedIngredients(ingredients,
                        "Đã sửa " + normalizedName + " và lưu vào local storage.");
            }
        }
        return localDataSource.saveConfirmedIngredients(ingredients,
                "Danh sách xác nhận đã được cập nhật.");
    }

    public IngredientConfirmState addConfirmedIngredient(String name, String quantity) {
        String normalizedName = normalizeIngredient(name);
        String normalizedQuantity = normalizeQuantity(quantity);
        List<ConfirmedIngredient> ingredients = new ArrayList<>(
                localDataSource.getIngredientConfirmState().getIngredients());
        if (normalizedName.isEmpty()) {
            return localDataSource.saveConfirmedIngredients(ingredients,
                    "Nhập tên nguyên liệu trước khi thêm.");
        }
        for (ConfirmedIngredient ingredient : ingredients) {
            if (ingredient.getName().equalsIgnoreCase(normalizedName)) {
                return localDataSource.saveConfirmedIngredients(ingredients,
                        normalizedName + " đã có trong danh sách xác nhận.");
            }
        }
        ingredients.add(new ConfirmedIngredient(createIngredientId(ingredients.size()),
                normalizedName, normalizedQuantity, ConfirmedIngredient.SOURCE_KEYBOARD, true));
        syncInputIngredients(ingredients);
        return localDataSource.saveConfirmedIngredients(ingredients,
                "Đã thêm " + normalizedName + " vào danh sách xác nhận.");
    }

    public IngredientConfirmState removeConfirmedIngredient(String ingredientId) {
        List<ConfirmedIngredient> ingredients = new ArrayList<>(
                localDataSource.getIngredientConfirmState().getIngredients());
        String removedName = "";
        for (int index = 0; index < ingredients.size(); index++) {
            ConfirmedIngredient ingredient = ingredients.get(index);
            if (ingredient.getId().equals(ingredientId)) {
                removedName = ingredient.getName();
                ingredients.remove(index);
                break;
            }
        }
        syncInputIngredients(ingredients);
        return localDataSource.saveConfirmedIngredients(ingredients,
                removedName.isEmpty()
                        ? "Danh sách xác nhận đã được cập nhật."
                        : "Đã xóa " + removedName + " khỏi danh sách xác nhận.");
    }

    public IngredientConfirmState confirmIngredientsForMatching() {
        IngredientConfirmState state = localDataSource.getIngredientConfirmState();
        List<String> selectedNames = new ArrayList<>();
        for (ConfirmedIngredient ingredient : state.getIngredients()) {
            if (ingredient.isSelected()) {
                selectedNames.add(ingredient.getName());
            }
        }
        localDataSource.saveIngredients(selectedNames,
                "Đã xác nhận " + selectedNames.size() + " nguyên liệu để tìm món gần nhất.");
        selectFeature(FLOW_INGREDIENT_MATCH);
        return localDataSource.saveConfirmedIngredients(state.getIngredients(),
                "Đã xác nhận " + selectedNames.size() + " nguyên liệu. Phase 7 sẽ tính match score local từ danh sách này.");
    }

    public AiRecipeSuggestionState calculateRecipeSuggestions() {
        if (recipeRepository == null) {
            return new AiRecipeSuggestionState(Collections.emptyList(), Collections.emptyList(),
                    "Kho công thức local chưa được nối vào AI Chef.");
        }
        List<String> selectedIngredients = getSelectedIngredientNames();
        if (selectedIngredients.isEmpty()) {
            selectedIngredients = localDataSource.getIngredientInputState().getIngredients();
        }
        if (selectedIngredients.isEmpty()) {
            return new AiRecipeSuggestionState(Collections.emptyList(), Collections.emptyList(),
                    "Chưa có nguyên liệu đã xác nhận. Hãy thêm nguyên liệu trước khi tìm món.");
        }

        List<RecipeMatch> matches = new ArrayList<>();
        for (Recipe recipe : recipeRepository.getAllRecipes()) {
            matches.add(calculateMatch(recipe, selectedIngredients));
        }
        Collections.sort(matches, new Comparator<RecipeMatch>() {
            @Override
            public int compare(RecipeMatch left, RecipeMatch right) {
                return Integer.compare(right.getScorePercent(), left.getScorePercent());
            }
        });
        if (matches.size() > 8) {
            matches = new ArrayList<>(matches.subList(0, 8));
        }
        RecipeMatch bestMatch = matches.isEmpty() ? null : matches.get(0);
        String status = bestMatch == null
                ? "Không tìm thấy công thức trong kho local."
                : "AI local đã so khớp " + selectedIngredients.size()
                + " nguyên liệu với " + recipeRepository.getAllRecipes().size()
                + " công thức trong Room.";
        return new AiRecipeSuggestionState(selectedIngredients, matches, status);
    }

    public AiRecipeSuggestionState calculateRecipeSuggestionsWithAiBackend() {
        AiRecipeSuggestionState localState = calculateRecipeSuggestions();
        if (localState.getMatches().isEmpty()) {
            return localState;
        }
        if (aiBackendRemoteDataSource == null || !aiBackendRemoteDataSource.isConfigured()) {
            String cachedAdvice = localDataSource.getLastAiAdvice();
            String status = cachedAdvice.isEmpty()
                    ? localState.getStatusMessage() + " AI backend chưa được cấu hình."
                    : "Gợi ý AI gần nhất: " + cachedAdvice;
            return new AiRecipeSuggestionState(localState.getSelectedIngredients(),
                    localState.getMatches(), status);
        }
        try {
            String advice = aiBackendRemoteDataSource.generateRecipeAdvice(
                    localState.getSelectedIngredients(), localState.getMatches());
            localDataSource.saveLastAiAdvice(advice);
            return new AiRecipeSuggestionState(localState.getSelectedIngredients(),
                    localState.getMatches(), "AI backend gợi ý: " + advice);
        } catch (Exception exception) {
            String cachedAdvice = localDataSource.getLastAiAdvice();
            String status = cachedAdvice.isEmpty()
                    ? localState.getStatusMessage()
                    + " Không gọi được AI backend, app đang dùng kết quả local. "
                    + exception.getMessage()
                    : "Không gọi được AI backend mới, dùng gợi ý đã lưu: " + cachedAdvice;
            return new AiRecipeSuggestionState(localState.getSelectedIngredients(),
                    localState.getMatches(), status);
        }
    }

    public AiRecipeSuggestionState toggleFavoriteInSuggestions(long recipeId) {
        localDataSource.toggleFavoriteRecipe(recipeId);
        return calculateRecipeSuggestions();
    }

    public MatchDetailState getMatchDetail(long recipeId, ShoppingPlanState shoppingPlanState) {
        AiRecipeSuggestionState suggestionState = calculateRecipeSuggestions();
        RecipeMatch targetMatch = null;
        for (RecipeMatch match : suggestionState.getMatches()) {
            if (recipeId <= 0 || match.getRecipe().getId() == recipeId) {
                targetMatch = match;
                break;
            }
        }
        if (targetMatch == null) {
            return new MatchDetailState(null, Collections.emptyList(),
                    "AI Chef chưa có kết quả để phân tích. Hãy xác nhận nguyên liệu rồi tìm món gần nhất.",
                    suggestionState.getStatusMessage(), shoppingPlanState);
        }
        return new MatchDetailState(targetMatch, createSubstituteSuggestions(targetMatch),
                createAiExplanation(targetMatch), suggestionState.getStatusMessage(), shoppingPlanState);
    }

    private RecipeMatch calculateMatch(Recipe recipe, List<String> selectedIngredients) {
        List<String> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> recipeIngredients = recipe.getIngredients();
        for (String recipeIngredient : recipeIngredients) {
            if (matchesAnyIngredient(recipeIngredient, selectedIngredients)) {
                available.add(recipeIngredient);
            } else {
                missing.add(recipeIngredient);
            }
        }

        int total = Math.max(1, recipeIngredients.size());
        int mainCount = Math.min(2, recipeIngredients.size());
        int matchedMain = 0;
        for (int index = 0; index < mainCount; index++) {
            if (matchesAnyIngredient(recipeIngredients.get(index), selectedIngredients)) {
                matchedMain++;
            }
        }

        int optionalCount = Math.max(1, recipeIngredients.size() - mainCount);
        int matchedOptional = Math.max(0, available.size() - matchedMain);
        int seasoningCount = 0;
        int matchedSeasoning = 0;
        for (String recipeIngredient : recipeIngredients) {
            if (isSeasoning(recipeIngredient)) {
                seasoningCount++;
                if (matchesAnyIngredient(recipeIngredient, selectedIngredients)) {
                    matchedSeasoning++;
                }
            }
        }

        float requiredMatch = available.size() / (float) total;
        float mainMatch = mainCount == 0 ? 0f : matchedMain / (float) mainCount;
        float optionalMatch = matchedOptional / (float) optionalCount;
        float seasoningMatch = seasoningCount == 0 ? 1f : matchedSeasoning / (float) seasoningCount;
        float userPreference = recipe.getPopularityScore() / 100f;
        float missingImportantPenalty = (mainCount - matchedMain) * 0.10f;
        float score = requiredMatch * 0.4f
                + mainMatch * 0.3f
                + optionalMatch * 0.1f
                + seasoningMatch * 0.1f
                + userPreference * 0.1f
                - missingImportantPenalty;
        int percent = Math.max(0, Math.min(99, Math.round(score * 100f)));
        String label;
        if (missing.isEmpty()) {
            label = "Có thể nấu ngay";
        } else if (missing.size() <= 2) {
            label = "Thiếu ít nguyên liệu";
        } else {
            label = "Cần bổ sung thêm";
        }
        return new RecipeMatch(recipe, percent, available, missing, label,
                localDataSource.isFavoriteRecipe(recipe.getId()));
    }

    private String createAiExplanation(RecipeMatch match) {
        if (match.getMissingIngredients().isEmpty()) {
            return "Bạn đã có đủ nguyên liệu chính để nấu món này. Điểm khớp được tính local từ công thức trong Room và rổ nguyên liệu đã xác nhận.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Bạn đã có ")
                .append(match.getAvailableCount())
                .append("/")
                .append(match.getRequiredCount())
                .append(" nguyên liệu cho món này. ");
        builder.append("Còn thiếu ")
                .append(join(match.getMissingIngredients()))
                .append(".");
        List<String> substitutes = createSubstituteSuggestions(match);
        if (!substitutes.isEmpty()) {
            builder.append(" Có thể cân nhắc thay bằng ")
                    .append(join(substitutes))
                    .append(" nếu đang có trong bếp.");
        }
        return builder.toString();
    }

    private List<String> createSubstituteSuggestions(RecipeMatch match) {
        List<String> substitutes = new ArrayList<>();
        List<String> available = match.getAvailableIngredients();
        for (String missing : match.getMissingIngredients()) {
            String substitute = findSubstituteFor(missing, available);
            if (!substitute.isEmpty() && !containsIngredient(substitutes, substitute)) {
                substitutes.add(substitute);
            }
        }
        return substitutes;
    }

    private String findSubstituteFor(String missing, List<String> available) {
        String normalized = normalizeForMatch(missing);
        if (normalized.contains("tỏi")) {
            if (matchesAnyIngredient("Hành tím", available) || matchesAnyIngredient("Hành lá", available)) {
                return matchesAnyIngredient("Hành tím", available) ? "Hành tím" : "Hành lá";
            }
            return "Hành tím";
        }
        if (normalized.contains("hành lá")) {
            return "Hành tím";
        }
        if (normalized.contains("nước mắm")) {
            return "Nước tương";
        }
        if (normalized.contains("tiêu")) {
            return "Ớt";
        }
        if (normalized.contains("cà chua")) {
            return "Cà chua bi";
        }
        if (normalized.contains("thịt bò")) {
            return "Thịt gà";
        }
        return "";
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private List<String> getSelectedIngredientNames() {
        List<String> selectedNames = new ArrayList<>();
        for (ConfirmedIngredient ingredient : localDataSource.getIngredientConfirmState().getIngredients()) {
            if (ingredient.isSelected()) {
                selectedNames.add(ingredient.getName());
            }
        }
        return selectedNames;
    }

    private boolean matchesAnyIngredient(String recipeIngredient, List<String> selectedIngredients) {
        String normalizedRecipe = normalizeForMatch(recipeIngredient);
        for (String selectedIngredient : selectedIngredients) {
            String normalizedSelected = normalizeForMatch(selectedIngredient);
            if (!normalizedSelected.isEmpty()
                    && (normalizedRecipe.contains(normalizedSelected)
                    || normalizedSelected.contains(normalizedRecipe))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.forLanguageTag("vi-VN"))
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isSeasoning(String ingredient) {
        String normalized = normalizeForMatch(ingredient);
        return normalized.contains("nước mắm")
                || normalized.contains("đường")
                || normalized.contains("tiêu")
                || normalized.contains("nước tương")
                || normalized.contains("dầu")
                || normalized.contains("muối");
    }

    private boolean containsIngredient(List<String> ingredients, String target) {
        for (String ingredient : ingredients) {
            if (ingredient.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeIngredient(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.forLanguageTag("vi-VN"));
        return lower.substring(0, 1).toUpperCase(Locale.forLanguageTag("vi-VN")) + lower.substring(1);
    }

    private String normalizeQuantity(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private ConfirmedIngredient toConfirmedIngredient(String value, int index) {
        String name = normalizeIngredient(value);
        return new ConfirmedIngredient(createIngredientId(index), name,
                defaultQuantityFor(name), defaultSourceFor(name), true);
    }

    private String createIngredientId(int index) {
        return "ingredient_" + System.currentTimeMillis() + "_" + index;
    }

    private String defaultQuantityFor(String name) {
        if ("Trứng gà".equalsIgnoreCase(name)) {
            return "4";
        }
        if ("Cà chua".equalsIgnoreCase(name)) {
            return "2";
        }
        if ("Thịt bò".equalsIgnoreCase(name)) {
            return "300g";
        }
        if ("Hành lá".equalsIgnoreCase(name)) {
            return "1 bó";
        }
        return "";
    }

    private String defaultSourceFor(String name) {
        if ("Trứng gà".equalsIgnoreCase(name) || "Hành lá".equalsIgnoreCase(name)) {
            return ConfirmedIngredient.SOURCE_CAMERA;
        }
        if ("Thịt bò".equalsIgnoreCase(name)) {
            return ConfirmedIngredient.SOURCE_KITCHEN;
        }
        return ConfirmedIngredient.SOURCE_KEYBOARD;
    }

    private void syncInputIngredients(List<ConfirmedIngredient> ingredients) {
        List<String> names = new ArrayList<>();
        for (ConfirmedIngredient ingredient : ingredients) {
            names.add(ingredient.getName());
        }
        localDataSource.saveIngredients(names,
                "Danh sách nhập nguyên liệu đã đồng bộ từ màn xác nhận.");
    }

    private boolean hasSameIngredientNames(IngredientConfirmState currentState, List<String> inputIngredients) {
        if (inputIngredients.isEmpty() || currentState.getTotalCount() != inputIngredients.size()) {
            return false;
        }
        for (String inputIngredient : inputIngredients) {
            boolean found = false;
            for (ConfirmedIngredient confirmedIngredient : currentState.getIngredients()) {
                if (confirmedIngredient.getName().equalsIgnoreCase(normalizeIngredient(inputIngredient))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private List<AiChefFeature> createFeatures() {
        List<AiChefFeature> values = new ArrayList<>();
        values.add(new AiChefFeature(FLOW_INGREDIENT_MATCH,
                "Gợi ý món từ nguyên liệu",
                "Bạn nhập những nguyên liệu đang có, AI local sẽ so khớp với kho công thức.",
                "MON", Color.parseColor("#E67E22"), true));
        values.add(new AiChefFeature(FLOW_SCAN_INGREDIENTS,
                "Quét nguyên liệu",
                "Chuẩn bị luồng chụp ảnh tủ lạnh để nhận gợi ý ở các phase sau.",
                "CAM", Color.parseColor("#C46A35"), false));
        values.add(new AiChefFeature(FLOW_CREATE_RECIPE,
                "Tạo công thức bằng AI",
                "Thêm công thức mới, chụp ảnh món ăn và lưu vào kho local.",
                "AI", Color.parseColor("#F0A51A"), false));
        values.add(new AiChefFeature(FLOW_WEEKLY_MENU,
                "Thực đơn tuần",
                "Mở luồng lập thực đơn và kế hoạch đi chợ cho cả gia đình.",
                "7N", Color.parseColor("#8B6B00"), false));
        values.add(new AiChefFeature(FLOW_COOKING_TIPS,
                "Hỏi mẹo nấu ăn",
                "Mở không gian hỏi mẹo nấu ăn, mẹo thay thế nguyên liệu và canh lửa.",
                "HOI", Color.parseColor("#00658F"), false));
        return values;
    }
}
