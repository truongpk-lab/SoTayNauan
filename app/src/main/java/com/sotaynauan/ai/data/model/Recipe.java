package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Recipe {
    private final long id;
    private final String name;
    private final String description;
    private final int totalMinutes;
    private final String difficulty;
    private final String category;
    private final int colorArgb;
    private final int popularityScore;
    private final boolean todaySuggestion;
    private final String friendName;
    private final String friendNote;
    private final List<String> ingredients;
    private final List<String> steps;

    public Recipe(long id, String name, String description, int totalMinutes, String difficulty,
                  String category, int colorArgb, int popularityScore, boolean todaySuggestion,
                  String friendName, String friendNote, List<String> ingredients, List<String> steps) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalMinutes = totalMinutes;
        this.difficulty = difficulty;
        this.category = category;
        this.colorArgb = colorArgb;
        this.popularityScore = popularityScore;
        this.todaySuggestion = todaySuggestion;
        this.friendName = friendName;
        this.friendNote = friendNote;
        this.ingredients = new ArrayList<>(ingredients);
        this.steps = new ArrayList<>(steps);
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTotalMinutes() { return totalMinutes; }
    public String getDifficulty() { return difficulty; }
    public String getCategory() { return category; }
    public int getColorArgb() { return colorArgb; }
    public int getPopularityScore() { return popularityScore; }
    public boolean isTodaySuggestion() { return todaySuggestion; }
    public String getFriendName() { return friendName; }
    public String getFriendNote() { return friendNote; }
    public List<String> getIngredients() { return Collections.unmodifiableList(ingredients); }
    public List<String> getSteps() { return Collections.unmodifiableList(steps); }
}
