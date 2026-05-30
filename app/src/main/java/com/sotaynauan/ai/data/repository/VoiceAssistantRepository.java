package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.model.VoiceAssistantState;
import com.sotaynauan.ai.data.model.VoiceCommand;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource.VoiceAudioMatchResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceAssistantRepository {
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)\\s*phút", Pattern.CASE_INSENSITIVE);

    private final VoiceLocalDataSource localDataSource;
    private final CookingRepository cookingRepository;
    private final AiBackendRemoteDataSource aiBackendRemoteDataSource;

    public VoiceAssistantRepository(VoiceLocalDataSource localDataSource,
                                    CookingRepository cookingRepository) {
        this(localDataSource, cookingRepository, null);
    }

    public VoiceAssistantRepository(VoiceLocalDataSource localDataSource,
                                    CookingRepository cookingRepository,
                                    AiBackendRemoteDataSource aiBackendRemoteDataSource) {
        this.localDataSource = localDataSource;
        this.cookingRepository = cookingRepository;
        this.aiBackendRemoteDataSource = aiBackendRemoteDataSource;
    }

    public VoiceAssistantState loadState(long fallbackRecipeId) {
        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        return createState(false,
                "Sẵn sàng nghe",
                "AI Chef đang chờ lệnh của bạn",
                localDataSource.getLastTranscript(),
                localDataSource.getLastResponse(),
                createContextLabel(sessionState));
    }

    public VoiceAssistantState startListening(long fallbackRecipeId) {
        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        return createState(true,
                "Đang nghe...",
                "Nói: đọc lại bước này, còn bao lâu, hoặc thêm 5 phút",
                "Đang nhận lệnh...",
                "Tôi đang nghe. Bạn cứ nói chậm rãi nhé.",
                createContextLabel(sessionState));
    }

    public VoiceAssistantState setAutoReadEnabled(boolean enabled, long fallbackRecipeId) {
        localDataSource.setAutoReadEnabled(enabled);
        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        String response = enabled
                ? "Đã bật tự động đọc hướng dẫn."
                : "Đã tắt tự động đọc hướng dẫn.";
        localDataSource.saveExchange(localDataSource.getLastTranscript(), response);
        return createState(false,
                "Đã cập nhật",
                "Thiết lập giọng nói đã được lưu local",
                localDataSource.getLastTranscript(),
                response,
                createContextLabel(sessionState));
    }

    public VoiceAssistantState handleCommand(String rawCommand, long fallbackRecipeId) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.isEmpty()) {
            command = createSuggestedCommand(fallbackRecipeId);
        }
        return handleCanonicalCommand(command, command, fallbackRecipeId);
    }

    private VoiceAssistantState handleCanonicalCommand(String command,
                                                       String transcript,
                                                       long fallbackRecipeId) {
        String normalized = command.toLowerCase(Locale.ROOT);
        String response;

        if (normalized.contains("thêm") && normalized.contains("phút")) {
            int minutes = extractMinutes(command);
            CookingTimerState currentTimer = cookingRepository.getTimerState();
            if (currentTimer.getTotalSeconds() <= 0) {
                cookingRepository.prepareCurrentStepTimer(fallbackRecipeId);
            }
            CookingTimerState timerState = cookingRepository.addMinutesToTimer(minutes);
            response = timerState.getAssistantMessage();
        } else if (normalized.contains("timer") || normalized.contains("bao lâu")
                || normalized.contains("còn")) {
            CookingTimerState timerState = cookingRepository.getTimerState();
            response = timerState.getAssistantMessage();
        } else if (normalized.contains("đọc") || normalized.contains("nhắc lại")) {
            response = cookingRepository.replayCurrentInstruction().getStatusMessage();
        } else if (normalized.contains("bước")) {
            CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
            response = sessionState.hasRecipe()
                    ? "Bạn đang ở bước " + (sessionState.getCurrentStepIndex() + 1)
                    + ": " + sessionState.getCurrentStepText()
                    : "Chưa có công thức đang nấu.";
        } else {
            response = "Tôi có thể đọc lại bước nấu, kiểm tra timer hoặc thêm thời gian cho bạn.";
        }

        localDataSource.saveExchange(transcript, response);
        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        return createState(false,
                "Đã nhận",
                "AI Chef đã xử lý lệnh bằng dữ liệu local",
                transcript,
                response,
                createContextLabel(sessionState));
    }

    public VoiceAssistantState handleSpokenCommandWithAiBackend(String spokenText, long fallbackRecipeId) {
        String transcript = spokenText == null ? "" : spokenText.trim();
        if (transcript.isEmpty()) {
            return createState(false,
                    "Chưa nghe rõ",
                    "Bạn có thể chạm mic và nói lại chậm hơn",
                    "Chưa nhận được giọng nói",
                    "Tôi chưa nghe rõ. Bạn thử nói lại nhé.",
                    createContextLabel(cookingRepository.getActiveSession(fallbackRecipeId)));
        }

        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        String contextLabel = createContextLabel(sessionState);
        String matchedCommand = matchCommandLocally(transcript, fallbackRecipeId);
        boolean usedBackend = false;
        if (aiBackendRemoteDataSource != null && aiBackendRemoteDataSource.isConfigured()) {
            try {
                matchedCommand = aiBackendRemoteDataSource.matchVoiceCommand(
                        transcript,
                        contextLabel,
                        createAllowedCommandTexts(fallbackRecipeId));
                usedBackend = true;
            } catch (Exception ignored) {
                matchedCommand = matchCommandLocally(transcript, fallbackRecipeId);
            }
        }

        VoiceAssistantState localState = handleCanonicalCommand(matchedCommand,
                transcript,
                fallbackRecipeId);
        String responsePrefix = "Tôi hiểu là: " + matchedCommand + ". ";
        String response = responsePrefix + localState.getResponse();
        if (aiBackendRemoteDataSource != null && aiBackendRemoteDataSource.isConfigured()) {
            try {
                String aiResponse = aiBackendRemoteDataSource.generateVoiceReply(
                        matchedCommand,
                        localState.getContextLabel(),
                        localState.getResponse());
                response = responsePrefix + aiResponse;
                usedBackend = true;
            } catch (Exception ignored) {
                // Local response is already available.
            }
        }

        localDataSource.saveExchange(transcript, response);
        return createState(false,
                "Đã nhận",
                usedBackend ? "Gemini đã khớp với lệnh gần nhất" : "Đã khớp lệnh bằng dữ liệu local",
                transcript,
                response,
                localState.getContextLabel());
    }

    public VoiceAssistantState handleRecordedAudioWithAiBackend(byte[] audioBytes, long fallbackRecipeId) {
        if (audioBytes == null || audioBytes.length == 0) {
            return createState(false,
                    "Chưa có âm thanh",
                    "Bấm mic để ghi lại",
                    "Chưa có audio",
                    "Tôi chưa nhận được file âm thanh. Bạn thử bấm mic và nói lại nhé.",
                    createContextLabel(cookingRepository.getActiveSession(fallbackRecipeId)));
        }
        if (aiBackendRemoteDataSource == null || !aiBackendRemoteDataSource.isConfigured()) {
            return createState(false,
                    "Backend chưa sẵn sàng",
                    "App vẫn dùng các lệnh nhanh local",
                    "Đã ghi âm",
                    "Đã thu được âm thanh, nhưng backend AI chưa được cấu hình để nghe file audio.",
                    createContextLabel(cookingRepository.getActiveSession(fallbackRecipeId)));
        }

        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        String contextLabel = createContextLabel(sessionState);
        try {
            VoiceAudioMatchResult matchResult = aiBackendRemoteDataSource.matchVoiceAudioCommand(
                    audioBytes,
                    "audio/mp4",
                    contextLabel,
                    createAllowedCommandTexts(fallbackRecipeId));
            String transcript = matchResult.getTranscript().isEmpty()
                    ? "Đã ghi âm tiếng Việt"
                    : matchResult.getTranscript();
            String matchedCommand = matchResult.getCommand().isEmpty()
                    ? matchCommandLocally(transcript, fallbackRecipeId)
                    : matchResult.getCommand();
            VoiceAssistantState localState = handleCanonicalCommand(matchedCommand, transcript, fallbackRecipeId);
            String response = "Nghe được: \"" + transcript + "\". Tôi hiểu là: "
                    + matchedCommand + ". " + localState.getResponse();
            localDataSource.saveExchange(transcript, response);
            return createState(false,
                    "Đã nhận",
                    "Gemini đã nghe audio tiếng Việt",
                    transcript,
                    response,
                    localState.getContextLabel());
        } catch (Exception exception) {
            String response = "Đã thu được âm thanh nhưng backend AI chưa phân tích được audio: "
                    + exception.getMessage();
            localDataSource.saveExchange("Đã ghi âm", response);
            return createState(false,
                    "Backend chưa phản hồi",
                    "Kiểm tra backend hoặc API key Gemini",
                    "Đã ghi âm",
                    response,
                    contextLabel);
        }
    }

    public VoiceAssistantState handleCommandWithAiBackend(String rawCommand, long fallbackRecipeId) {
        VoiceAssistantState localState = handleCommand(rawCommand, fallbackRecipeId);
        if (aiBackendRemoteDataSource == null || !aiBackendRemoteDataSource.isConfigured()) {
            return localState;
        }
        try {
            String aiResponse = aiBackendRemoteDataSource.generateVoiceReply(
                    localState.getTranscript(),
                    localState.getContextLabel(),
                    localState.getResponse());
            localDataSource.saveExchange(localState.getTranscript(), aiResponse);
            return createState(false,
                    "Đã nhận",
                    "AI backend đã xử lý lệnh",
                    localState.getTranscript(),
                    aiResponse,
                    localState.getContextLabel());
        } catch (Exception exception) {
            String fallback = localState.getResponse()
                    + " AI backend hiện chưa phản hồi, nên app dùng kết quả local.";
            localDataSource.saveExchange(localState.getTranscript(), fallback);
            return createState(false,
                    "Đã nhận",
                    "Đang dùng phản hồi local",
                    localState.getTranscript(),
                    fallback,
                    localState.getContextLabel());
        }
    }

    public String createSuggestedCommand(long fallbackRecipeId) {
        CookingTimerState timerState = cookingRepository.getTimerState();
        if (timerState.getTotalSeconds() > 0) {
            return "Thêm 5 phút vào timer";
        }
        CookingSessionState sessionState = cookingRepository.getActiveSession(fallbackRecipeId);
        if (sessionState.hasRecipe()) {
            return "Đọc lại bước này";
        }
        return "Tôi đang nấu món gì?";
    }

    public boolean isAutoReadEnabled() {
        return localDataSource.isVoiceEnabled() && localDataSource.isAutoReadEnabled();
    }

    private VoiceAssistantState createState(boolean listening,
                                            String statusTitle,
                                            String statusSubtitle,
                                            String transcript,
                                            String response,
                                            String contextLabel) {
        return new VoiceAssistantState(listening,
                localDataSource.isVoiceEnabled() && localDataSource.isAutoReadEnabled(),
                statusTitle,
                statusSubtitle,
                transcript,
                response,
                contextLabel,
                createQuickCommands());
    }

    private List<VoiceCommand> createQuickCommands() {
        return Arrays.asList(
                new VoiceCommand("Đọc lại", "Đọc lại bước này"),
                new VoiceCommand("Còn bao lâu", "Timer còn bao lâu"),
                new VoiceCommand("+5 phút", "Thêm 5 phút vào timer"));
    }

    private List<String> createAllowedCommandTexts(long fallbackRecipeId) {
        return Arrays.asList(
                "Đọc lại bước này",
                "Timer còn bao lâu",
                "Thêm 5 phút vào timer",
                "Bạn đang ở bước mấy");
    }

    private String matchCommandLocally(String spokenText, long fallbackRecipeId) {
        String normalized = spokenText.toLowerCase(Locale.ROOT);
        Matcher minuteMatcher = MINUTE_PATTERN.matcher(spokenText);
        if ((normalized.contains("thêm") || normalized.contains("cộng"))
                && (normalized.contains("phút") || normalized.contains("timer"))) {
            if (minuteMatcher.find()) {
                return "Thêm " + Math.max(1, Integer.parseInt(minuteMatcher.group(1))) + " phút vào timer";
            }
            return "Thêm 5 phút vào timer";
        }
        if (normalized.contains("bao lâu") || normalized.contains("còn")
                || normalized.contains("timer") || normalized.contains("hẹn giờ")) {
            return "Timer còn bao lâu";
        }
        if (normalized.contains("đọc") || normalized.contains("nhắc")
                || normalized.contains("nói lại") || normalized.contains("lặp lại")) {
            return "Đọc lại bước này";
        }
        if (normalized.contains("bước")) {
            return "Bạn đang ở bước mấy";
        }
        return createSuggestedCommand(fallbackRecipeId);
    }

    private String createContextLabel(CookingSessionState sessionState) {
        if (sessionState == null || !sessionState.hasRecipe()) {
            return "Chưa có phiên nấu";
        }
        return sessionState.getRecipe().getName()
                + " • Bước " + (sessionState.getCurrentStepIndex() + 1)
                + "/" + Math.max(1, sessionState.getStepCount());
    }

    private int extractMinutes(String command) {
        Matcher matcher = MINUTE_PATTERN.matcher(command);
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        }
        return 5;
    }
}
