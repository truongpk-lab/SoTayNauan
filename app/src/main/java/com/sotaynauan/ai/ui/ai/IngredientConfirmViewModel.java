package com.sotaynauan.ai.ui.ai;

import com.sotaynauan.ai.data.model.IngredientConfirmState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

public class IngredientConfirmViewModel {
    private final AiChefRepository repository;

    public IngredientConfirmViewModel(AiChefRepository repository) {
        this.repository = repository;
    }

    public IngredientConfirmState prepareState() {
        return repository.prepareIngredientConfirmation();
    }

    public IngredientConfirmState loadState() {
        return repository.getIngredientConfirmState();
    }

    public IngredientConfirmState toggleIngredient(String ingredientId, boolean selected) {
        return repository.toggleConfirmedIngredient(ingredientId, selected);
    }

    public IngredientConfirmState updateIngredient(String ingredientId, String name, String quantity) {
        return repository.updateConfirmedIngredient(ingredientId, name, quantity);
    }

    public IngredientConfirmState addIngredient(String name, String quantity) {
        return repository.addConfirmedIngredient(name, quantity);
    }

    public IngredientConfirmState removeIngredient(String ingredientId) {
        return repository.removeConfirmedIngredient(ingredientId);
    }

    public IngredientConfirmState confirmForMatching() {
        return repository.confirmIngredientsForMatching();
    }
}
