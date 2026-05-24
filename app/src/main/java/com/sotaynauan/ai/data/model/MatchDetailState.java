package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchDetailState {
    private final RecipeMatch match;
    private final List<String> substituteSuggestions;
    private final String aiExplanation;
    private final String statusMessage;
    private final ShoppingPlanState shoppingPlanState;

    public MatchDetailState(RecipeMatch match,
                            List<String> substituteSuggestions,
                            String aiExplanation,
                            String statusMessage,
                            ShoppingPlanState shoppingPlanState) {
        this.match = match;
        this.substituteSuggestions = new ArrayList<>(substituteSuggestions);
        this.aiExplanation = aiExplanation;
        this.statusMessage = statusMessage;
        this.shoppingPlanState = shoppingPlanState;
    }

    public RecipeMatch getMatch() {
        return match;
    }

    public List<String> getSubstituteSuggestions() {
        return Collections.unmodifiableList(substituteSuggestions);
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public ShoppingPlanState getShoppingPlanState() {
        return shoppingPlanState;
    }

    public boolean hasMatch() {
        return match != null;
    }
}
