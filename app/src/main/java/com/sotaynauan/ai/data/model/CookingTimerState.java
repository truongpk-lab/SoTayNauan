package com.sotaynauan.ai.data.model;

public class CookingTimerState {
    private final long recipeId;
    private final int stepIndex;
    private final int totalSeconds;
    private final int remainingSeconds;
    private final boolean running;
    private final boolean expired;
    private final boolean alarmAcknowledged;
    private final String assistantMessage;

    public CookingTimerState(long recipeId, int stepIndex, int totalSeconds, int remainingSeconds,
                             boolean running, boolean expired, boolean alarmAcknowledged,
                             String assistantMessage) {
        this.recipeId = recipeId;
        this.stepIndex = stepIndex;
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = remainingSeconds;
        this.running = running;
        this.expired = expired;
        this.alarmAcknowledged = alarmAcknowledged;
        this.assistantMessage = assistantMessage;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isAlarmAcknowledged() {
        return alarmAcknowledged;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public boolean isNearEnd() {
        return remainingSeconds > 0 && remainingSeconds <= 60;
    }
}
