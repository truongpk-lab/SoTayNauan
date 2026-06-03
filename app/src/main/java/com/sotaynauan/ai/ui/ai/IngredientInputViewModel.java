package com.sotaynauan.ai.ui.ai;

import com.sotaynauan.ai.data.model.DetectedIngredient;
import com.sotaynauan.ai.data.model.IngredientInputState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

import java.util.List;

public class IngredientInputViewModel {
    private final AiChefRepository repository;

    public IngredientInputViewModel(AiChefRepository repository) {
        this.repository = repository;
    }

    public IngredientInputState loadState() {
        return repository.getIngredientInputState();
    }

    public List<String> loadSuggestions() {
        return repository.getIngredientSuggestions();
    }

    public IngredientInputState addIngredient(String ingredient) {
        return repository.addIngredient(ingredient);
    }

    public IngredientInputState addDetectedIngredients(List<DetectedIngredient> ingredients) {
        return repository.addDetectedIngredients(ingredients);
    }

    public IngredientInputState removeIngredient(String ingredient) {
        return repository.removeIngredient(ingredient);
    }

    public IngredientInputState markReadyForSuggestions() {
        return repository.markIngredientsReadyForSuggestions();
    }
}
