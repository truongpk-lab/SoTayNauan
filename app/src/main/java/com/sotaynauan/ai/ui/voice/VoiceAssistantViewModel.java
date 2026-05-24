package com.sotaynauan.ai.ui.voice;

import com.sotaynauan.ai.data.model.VoiceAssistantState;
import com.sotaynauan.ai.data.repository.VoiceAssistantRepository;

public class VoiceAssistantViewModel {
    private final VoiceAssistantRepository repository;

    public VoiceAssistantViewModel(VoiceAssistantRepository repository) {
        this.repository = repository;
    }

    public VoiceAssistantState loadState(long fallbackRecipeId) {
        return repository.loadState(fallbackRecipeId);
    }

    public VoiceAssistantState startListening(long fallbackRecipeId) {
        return repository.startListening(fallbackRecipeId);
    }

    public VoiceAssistantState handleCommand(String command, long fallbackRecipeId) {
        return repository.handleCommand(command, fallbackRecipeId);
    }

    public VoiceAssistantState handleCommandWithAiBackend(String command, long fallbackRecipeId) {
        return repository.handleCommandWithAiBackend(command, fallbackRecipeId);
    }

    public VoiceAssistantState handleSpokenCommandWithAiBackend(String spokenText, long fallbackRecipeId) {
        return repository.handleSpokenCommandWithAiBackend(spokenText, fallbackRecipeId);
    }

    public VoiceAssistantState handleRecordedAudioWithAiBackend(byte[] audioBytes, long fallbackRecipeId) {
        return repository.handleRecordedAudioWithAiBackend(audioBytes, fallbackRecipeId);
    }

    public VoiceAssistantState setAutoReadEnabled(boolean enabled, long fallbackRecipeId) {
        return repository.setAutoReadEnabled(enabled, fallbackRecipeId);
    }

    public String createSuggestedCommand(long fallbackRecipeId) {
        return repository.createSuggestedCommand(fallbackRecipeId);
    }

    public boolean isAutoReadEnabled() {
        return repository.isAutoReadEnabled();
    }
}
