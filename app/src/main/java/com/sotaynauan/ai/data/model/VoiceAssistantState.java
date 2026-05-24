package com.sotaynauan.ai.data.model;

import java.util.Collections;
import java.util.List;

public class VoiceAssistantState {
    private final boolean listening;
    private final boolean autoReadEnabled;
    private final String statusTitle;
    private final String statusSubtitle;
    private final String transcript;
    private final String response;
    private final String contextLabel;
    private final List<VoiceCommand> quickCommands;

    public VoiceAssistantState(boolean listening,
                               boolean autoReadEnabled,
                               String statusTitle,
                               String statusSubtitle,
                               String transcript,
                               String response,
                               String contextLabel,
                               List<VoiceCommand> quickCommands) {
        this.listening = listening;
        this.autoReadEnabled = autoReadEnabled;
        this.statusTitle = statusTitle;
        this.statusSubtitle = statusSubtitle;
        this.transcript = transcript;
        this.response = response;
        this.contextLabel = contextLabel;
        this.quickCommands = quickCommands == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(quickCommands);
    }

    public boolean isListening() {
        return listening;
    }

    public boolean isAutoReadEnabled() {
        return autoReadEnabled;
    }

    public String getStatusTitle() {
        return statusTitle;
    }

    public String getStatusSubtitle() {
        return statusSubtitle;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getResponse() {
        return response;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public List<VoiceCommand> getQuickCommands() {
        return quickCommands;
    }
}
