package com.sotaynauan.ai.data.model;

public class CookingSessionState {
    private final Recipe recipe;
    private final int currentStepIndex;
    private final int currentStepSeconds;
    private final boolean completed;
    private final long updatedAt;
    private final String statusMessage;

    public CookingSessionState(Recipe recipe, int currentStepIndex, int currentStepSeconds,
                               boolean completed, long updatedAt, String statusMessage) {
        this.recipe = recipe;
        this.currentStepIndex = currentStepIndex;
        this.currentStepSeconds = currentStepSeconds;
        this.completed = completed;
        this.updatedAt = updatedAt;
        this.statusMessage = statusMessage;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public int getCurrentStepSeconds() {
        return currentStepSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean hasRecipe() {
        return recipe != null;
    }

    public int getStepCount() {
        return recipe == null ? 0 : recipe.getSteps().size();
    }

    public String getCurrentStepText() {
        if (recipe == null || recipe.getSteps().isEmpty()) {
            return "Công thức chưa có bước nấu.";
        }
        int safeIndex = Math.max(0, Math.min(currentStepIndex, recipe.getSteps().size() - 1));
        return recipe.getSteps().get(safeIndex);
    }

    public String getCurrentStepTitle() {
        if (completed) {
            return "Hoàn tất món ăn";
        }
        if (recipe == null) {
            return "Chưa có công thức";
        }
        return "Bước " + (currentStepIndex + 1);
    }
}
