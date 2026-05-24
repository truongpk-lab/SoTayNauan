package com.sotaynauan.ai.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class RecipeEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String description;
    public int totalMinutes;
    public String difficulty;
    public String category;
    public int colorArgb;
    public int popularityScore;
    public boolean todaySuggestion;
    public String friendName;
    public String friendNote;
    public String ingredients;
    public String steps;
}
