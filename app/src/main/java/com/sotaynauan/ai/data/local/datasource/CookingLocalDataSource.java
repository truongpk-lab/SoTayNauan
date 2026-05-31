package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

public class CookingLocalDataSource {
    private static final String PREFS_NAME = "cooking_session_state";
    private static final String KEY_RECIPE_ID = "recipe_id";
    private static final String KEY_STEP_INDEX = "step_index";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_TIMER_RECIPE_ID = "timer_recipe_id";
    private static final String KEY_TIMER_STEP_INDEX = "timer_step_index";
    private static final String KEY_TIMER_TOTAL_SECONDS = "timer_total_seconds";
    private static final String KEY_TIMER_REMAINING_SECONDS = "timer_remaining_seconds";
    private static final String KEY_TIMER_RUNNING = "timer_running";
    private static final String KEY_TIMER_STARTED_AT = "timer_started_at";
    private static final String KEY_TIMER_ALARM_ACKNOWLEDGED = "timer_alarm_acknowledged";
    private static final String KEY_COOKED_COUNT = "cooked_count";
    private static final String KEY_FINISHED_PHOTO_PREFIX = "finished_photo_";
    private static final String KEY_FINISHED_NOTE_PREFIX = "finished_note_";

    private final SharedPreferences preferences;

    public CookingLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void startSession(long recipeId) {
        preferences.edit()
                .putLong(KEY_RECIPE_ID, recipeId)
                .putInt(KEY_STEP_INDEX, 0)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .putBoolean(KEY_COMPLETED, false)
                .remove(KEY_TIMER_RECIPE_ID)
                .remove(KEY_TIMER_STEP_INDEX)
                .remove(KEY_TIMER_TOTAL_SECONDS)
                .remove(KEY_TIMER_REMAINING_SECONDS)
                .remove(KEY_TIMER_RUNNING)
                .remove(KEY_TIMER_STARTED_AT)
                .remove(KEY_TIMER_ALARM_ACKNOWLEDGED)
                .apply();
    }

    public void updateStep(int stepIndex, boolean completed) {
        boolean shouldIncrementCookedCount = completed && !isCompleted();
        int nextCookedCount = getCookedCount() + (shouldIncrementCookedCount ? 1 : 0);
        preferences.edit()
                .putInt(KEY_STEP_INDEX, stepIndex)
                .putBoolean(KEY_COMPLETED, completed)
                .putInt(KEY_COOKED_COUNT, nextCookedCount)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .remove(KEY_TIMER_RECIPE_ID)
                .remove(KEY_TIMER_STEP_INDEX)
                .remove(KEY_TIMER_TOTAL_SECONDS)
                .remove(KEY_TIMER_REMAINING_SECONDS)
                .remove(KEY_TIMER_RUNNING)
                .remove(KEY_TIMER_STARTED_AT)
                .remove(KEY_TIMER_ALARM_ACKNOWLEDGED)
                .apply();
    }

    public void saveTimer(long recipeId, int stepIndex, int totalSeconds, int remainingSeconds,
                          boolean running, long startedAt) {
        preferences.edit()
                .putLong(KEY_TIMER_RECIPE_ID, recipeId)
                .putInt(KEY_TIMER_STEP_INDEX, stepIndex)
                .putInt(KEY_TIMER_TOTAL_SECONDS, Math.max(0, totalSeconds))
                .putInt(KEY_TIMER_REMAINING_SECONDS, Math.max(0, remainingSeconds))
                .putBoolean(KEY_TIMER_RUNNING, running)
                .putLong(KEY_TIMER_STARTED_AT, startedAt)
                .putBoolean(KEY_TIMER_ALARM_ACKNOWLEDGED, false)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public void markTimerExpired(long recipeId, int stepIndex, int totalSeconds) {
        preferences.edit()
                .putLong(KEY_TIMER_RECIPE_ID, recipeId)
                .putInt(KEY_TIMER_STEP_INDEX, stepIndex)
                .putInt(KEY_TIMER_TOTAL_SECONDS, Math.max(0, totalSeconds))
                .putInt(KEY_TIMER_REMAINING_SECONDS, 0)
                .putBoolean(KEY_TIMER_RUNNING, false)
                .putLong(KEY_TIMER_STARTED_AT, 0L)
                .putBoolean(KEY_TIMER_ALARM_ACKNOWLEDGED, false)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public void acknowledgeTimerAlarm() {
        preferences.edit()
                .putBoolean(KEY_TIMER_ALARM_ACKNOWLEDGED, true)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean hasTimerFor(long recipeId, int stepIndex) {
        return preferences.getLong(KEY_TIMER_RECIPE_ID, -1L) == recipeId
                && preferences.getInt(KEY_TIMER_STEP_INDEX, -1) == stepIndex
                && preferences.contains(KEY_TIMER_REMAINING_SECONDS);
    }

    public long getTimerRecipeId() {
        return preferences.getLong(KEY_TIMER_RECIPE_ID, -1L);
    }

    public int getTimerStepIndex() {
        return preferences.getInt(KEY_TIMER_STEP_INDEX, 0);
    }

    public int getTimerTotalSeconds() {
        return preferences.getInt(KEY_TIMER_TOTAL_SECONDS, 0);
    }

    public int getTimerRemainingSeconds() {
        return preferences.getInt(KEY_TIMER_REMAINING_SECONDS, 0);
    }

    public boolean isTimerRunning() {
        return preferences.getBoolean(KEY_TIMER_RUNNING, false);
    }

    public long getTimerStartedAt() {
        return preferences.getLong(KEY_TIMER_STARTED_AT, 0L);
    }

    public boolean isTimerAlarmAcknowledged() {
        return preferences.getBoolean(KEY_TIMER_ALARM_ACKNOWLEDGED, false);
    }

    public long getActiveRecipeId() {
        return preferences.getLong(KEY_RECIPE_ID, -1L);
    }

    public int getCurrentStepIndex() {
        return preferences.getInt(KEY_STEP_INDEX, 0);
    }

    public boolean isCompleted() {
        return preferences.getBoolean(KEY_COMPLETED, false);
    }

    public long getUpdatedAt() {
        return preferences.getLong(KEY_UPDATED_AT, 0L);
    }

    public int getCookedCount() {
        return preferences.getInt(KEY_COOKED_COUNT, 0);
    }

    public void saveFinishedPhoto(long recipeId, String photoUri) {
        preferences.edit()
                .putString(KEY_FINISHED_PHOTO_PREFIX + recipeId, photoUri == null ? "" : photoUri)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getFinishedPhoto(long recipeId) {
        return preferences.getString(KEY_FINISHED_PHOTO_PREFIX + recipeId, "");
    }

    public void saveFinishedNote(long recipeId, String note) {
        preferences.edit()
                .putString(KEY_FINISHED_NOTE_PREFIX + recipeId, note == null ? "" : note)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getFinishedNote(long recipeId) {
        return preferences.getString(KEY_FINISHED_NOTE_PREFIX + recipeId, "");
    }
}
