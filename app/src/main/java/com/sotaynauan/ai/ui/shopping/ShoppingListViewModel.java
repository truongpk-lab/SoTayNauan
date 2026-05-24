package com.sotaynauan.ai.ui.shopping;

import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.repository.ShoppingRepository;

public class ShoppingListViewModel {
    private final ShoppingRepository shoppingRepository;

    public ShoppingListViewModel(ShoppingRepository shoppingRepository) {
        this.shoppingRepository = shoppingRepository;
    }

    public ShoppingPlanState loadList() {
        return shoppingRepository.getShoppingList();
    }

    public ShoppingPlanState toggleBought(String itemId, boolean bought) {
        return shoppingRepository.updateShoppingListItemStatus(itemId,
                bought ? ShoppingItemStatus.BOUGHT : ShoppingItemStatus.NEED_BUY);
    }

    public ShoppingPlanState setStatus(String itemId, ShoppingItemStatus status) {
        return shoppingRepository.updateShoppingListItemStatus(itemId, status);
    }

    public ShoppingPlanState addItem(String name, int amount, String unit, String category) {
        return shoppingRepository.addCustomShoppingListItem(name, amount, unit, category);
    }

    public ShoppingPlanState updateItem(String itemId, String name, int amount, String unit, String category) {
        return shoppingRepository.updateShoppingListItem(itemId, name, amount, unit, category);
    }

    public ShoppingPlanState removeItem(String itemId) {
        return shoppingRepository.removeShoppingListItem(itemId);
    }

    public ShoppingPlanState finishMarketTrip() {
        return shoppingRepository.finishMarketTrip();
    }
}
