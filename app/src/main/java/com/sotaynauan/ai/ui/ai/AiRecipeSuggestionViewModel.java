package com.sotaynauan.ai.ui.ai;

import com.sotaynauan.ai.data.model.AiRecipeSuggestionState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

public class AiRecipeSuggestionViewModel {
    private final AiChefRepository repository;

    public AiRecipeSuggestionViewModel(AiChefRepository repository) {
        this.repository = repository;
    }

    public AiRecipeSuggestionState loadSuggestions() {
        return repository.calculateRecipeSuggestions();
    }

    public AiRecipeSuggestionState loadSuggestionsWithAiBackend() {
        return repository.calculateRecipeSuggestionsWithAiBackend();
    }

    public AiRecipeSuggestionState toggleFavorite(long recipeId) {
        return repository.toggleFavoriteInSuggestions(recipeId);
    }
}
