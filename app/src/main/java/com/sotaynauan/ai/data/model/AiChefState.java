package com.sotaynauan.ai.data.model;

public class AiChefState {
    private final String activeFeatureId;
    private final long updatedAtMillis;
    private final int openCount;

    public AiChefState(String activeFeatureId, long updatedAtMillis, int openCount) {
        this.activeFeatureId = activeFeatureId;
        this.updatedAtMillis = updatedAtMillis;
        this.openCount = openCount;
    }

    public String getActiveFeatureId() { return activeFeatureId; }
    public long getUpdatedAtMillis() { return updatedAtMillis; }
    public int getOpenCount() { return openCount; }
}
