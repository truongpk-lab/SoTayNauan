package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiRecipeSuggestionState {
    private final List<String> selectedIngredients;
    private final List<RecipeMatch> matches;
    private final String statusMessage;

    public AiRecipeSuggestionState(List<String> selectedIngredients,
                                   List<RecipeMatch> matches,
                                   String statusMessage) {
        this.selectedIngredients = new ArrayList<>(selectedIngredients);
        this.matches = new ArrayList<>(matches);
        this.statusMessage = statusMessage;
    }

    public List<String> getSelectedIngredients() {
        return Collections.unmodifiableList(selectedIngredients);
    }

    public List<RecipeMatch> getMatches() {
        return Collections.unmodifiableList(matches);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public RecipeMatch getBestMatch() {
        return matches.isEmpty() ? null : matches.get(0);
    }

    public List<RecipeMatch> getOtherMatches() {
        if (matches.size() <= 1) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(matches.subList(1, matches.size()));
    }
}
