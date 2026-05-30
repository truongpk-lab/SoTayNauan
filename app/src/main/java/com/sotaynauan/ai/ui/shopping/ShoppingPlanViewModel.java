package com.sotaynauan.ai.ui.shopping;

import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.repository.ShoppingRepository;

public class ShoppingPlanViewModel {
    private final ShoppingRepository shoppingRepository;

    public ShoppingPlanViewModel(ShoppingRepository shoppingRepository) {
        this.shoppingRepository = shoppingRepository;
    }

    public ShoppingPlanState loadPlan() {
        return shoppingRepository.getCurrentPlan();
    }

    public ShoppingPlanState updateStatus(String itemId, ShoppingItemStatus status) {
        return shoppingRepository.updateItemStatus(itemId, status);
    }

    public ShoppingPlanState increaseQuantity(String itemId) {
        return shoppingRepository.increaseQuantity(itemId);
    }

    public ShoppingPlanState decreaseQuantity(String itemId) {
        return shoppingRepository.decreaseQuantity(itemId);
    }

    public ShoppingPlanState createShoppingList() {
        return shoppingRepository.createShoppingListFromPlan();
    }
}
