package com.sotaynauan.ai.ui.cooking;

import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.repository.CookingRepository;

public class CookingTimerViewModel {
    private final CookingRepository cookingRepository;

    public CookingTimerViewModel(CookingRepository cookingRepository) {
        this.cookingRepository = cookingRepository;
    }

    public CookingTimerState prepareTimer(long fallbackRecipeId) {
        return cookingRepository.prepareCurrentStepTimer(fallbackRecipeId);
    }

    public CookingTimerState getTimerState() {
        return cookingRepository.getTimerState();
    }

    public CookingTimerState pauseTimer() {
        return cookingRepository.pauseTimer();
    }

    public CookingTimerState resumeTimer() {
        return cookingRepository.resumeTimer();
    }

    public CookingTimerState addOneMinute() {
        return cookingRepository.addOneMinuteToTimer();
    }

    public CookingTimerState addMinutes(int minutes) {
        return cookingRepository.addMinutesToTimer(minutes);
    }

    public CookingTimerState stopAlarm() {
        return cookingRepository.acknowledgeTimerAlarm();
    }

    public CookingSessionState completeStepAfterTimer() {
        return cookingRepository.completeStepAfterTimer();
    }
}
