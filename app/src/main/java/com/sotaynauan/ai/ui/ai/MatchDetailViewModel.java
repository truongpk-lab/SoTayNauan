package com.sotaynauan.ai.ui.ai;

import com.sotaynauan.ai.data.model.MatchDetailState;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.repository.AiChefRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;

public class MatchDetailViewModel {
    private final AiChefRepository aiChefRepository;
    private final ShoppingRepository shoppingRepository;

    public MatchDetailViewModel(AiChefRepository aiChefRepository,
                                ShoppingRepository shoppingRepository) {
        this.aiChefRepository = aiChefRepository;
        this.shoppingRepository = shoppingRepository;
    }

    public MatchDetailState loadMatchDetail(long recipeId) {
        return aiChefRepository.getMatchDetail(recipeId, shoppingRepository.getCurrentPlan());
    }

    public MatchDetailState saveMissingIngredients(long recipeId) {
        MatchDetailState currentState = loadMatchDetail(recipeId);
        RecipeMatch match = currentState.getMatch();
        if (match == null) {
            return currentState;
        }
        ShoppingPlanState planState = shoppingRepository.saveMissingIngredientsForMatch(match);
        return new MatchDetailState(match, currentState.getSubstituteSuggestions(),
                currentState.getAiExplanation(),
                planState.isEmpty()
                        ? "Món này không thiếu nguyên liệu nào, bạn có thể mở công thức ngay."
                        : "Đã lưu " + planState.getMissingIngredients().size()
                        + " nguyên liệu thiếu vào kế hoạch đi chợ local.",
                planState);
    }

    public MatchDetailState toggleFavorite(long recipeId) {
        aiChefRepository.toggleFavoriteInSuggestions(recipeId);
        MatchDetailState nextState = loadMatchDetail(recipeId);
        RecipeMatch match = nextState.getMatch();
        if (match == null) {
            return nextState;
        }
        return new MatchDetailState(match, nextState.getSubstituteSuggestions(),
                nextState.getAiExplanation(),
                match.isFavorite()
                        ? "Đã lưu món này vào danh sách yêu thích local."
                        : "Đã bỏ món này khỏi danh sách yêu thích local.",
                nextState.getShoppingPlanState());
    }
}
