package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeContent {
    private final List<Recipe> todaySuggestions;
    private final List<Recipe> popularRecipes;
    private final List<Recipe> friendShares;

    public HomeContent(List<Recipe> todaySuggestions, List<Recipe> popularRecipes, List<Recipe> friendShares) {
        this.todaySuggestions = new ArrayList<>(todaySuggestions);
        this.popularRecipes = new ArrayList<>(popularRecipes);
        this.friendShares = new ArrayList<>(friendShares);
    }

    public List<Recipe> getTodaySuggestions() {
        return Collections.unmodifiableList(todaySuggestions);
    }

    public List<Recipe> getPopularRecipes() {
        return Collections.unmodifiableList(popularRecipes);
    }

    public List<Recipe> getFriendShares() {
        return Collections.unmodifiableList(friendShares);
    }
}
