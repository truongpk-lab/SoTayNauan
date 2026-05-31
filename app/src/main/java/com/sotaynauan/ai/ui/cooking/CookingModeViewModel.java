package com.sotaynauan.ai.ui.cooking;

import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.repository.CookingRepository;

public class CookingModeViewModel {
    private final CookingRepository cookingRepository;

    public CookingModeViewModel(CookingRepository cookingRepository) {
        this.cookingRepository = cookingRepository;
    }

    public CookingSessionState loadSession(long fallbackRecipeId) {
        return cookingRepository.getActiveSession(fallbackRecipeId);
    }

    public CookingSessionState completeCurrentStep() {
        return cookingRepository.completeCurrentStep();
    }

    public CookingSessionState goToPreviousStep() {
        return cookingRepository.goToPreviousStep();
    }

    public CookingSessionState replayCurrentInstruction() {
        return cookingRepository.replayCurrentInstruction();
    }

    public CookingTimerState getTimerState() {
        return cookingRepository.getTimerState();
    }

    public void saveFinishedPhoto(long recipeId, String photoUri) {
        cookingRepository.saveFinishedPhoto(recipeId, photoUri);
    }

    public String getFinishedPhoto(long recipeId) {
        return cookingRepository.getFinishedPhoto(recipeId);
    }

    public void saveFinishedNote(long recipeId, String note) {
        cookingRepository.saveFinishedNote(recipeId, note);
    }

    public String getFinishedNote(long recipeId) {
        return cookingRepository.getFinishedNote(recipeId);
    }
}
