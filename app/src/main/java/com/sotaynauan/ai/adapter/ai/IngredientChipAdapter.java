package com.sotaynauan.ai.adapter.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class IngredientChipAdapter {
    public interface OnIngredientClickListener {
        void onIngredientClick(String ingredient);
    }

    private static final int[] CHIP_COLORS = {
            Color.parseColor("#FFE084"),
            Color.parseColor("#FFDBCF"),
            Color.parseColor("#C7E7FF"),
            Color.parseColor("#FFF1EC")
    };

    private final Context context;

    public IngredientChipAdapter(Context context) {
        this.context = context;
    }

    public void bindSuggestions(LinearLayout container, List<String> suggestions,
                                OnIngredientClickListener listener) {
        container.removeAllViews();
        for (String suggestion : suggestions) {
            TextView chip = createSuggestionChip(suggestion);
            chip.setOnClickListener(view -> listener.onIngredientClick(suggestion));
            container.addView(chip);
        }
    }

    public void bindBasket(GridLayout container, List<String> ingredients,
                           OnIngredientClickListener removeListener) {
        container.removeAllViews();
        container.setColumnCount(2);
        for (int index = 0; index < ingredients.size(); index++) {
            String ingredient = ingredients.get(index);
            TextView chip = createBasketChip(ingredient, index);
            chip.setOnClickListener(view -> removeListener.onIngredientClick(ingredient));
            container.addView(chip);
        }
    }

    private TextView createSuggestionChip(String value) {
        TextView chip = new TextView(context);
        chip.setText(value);
        chip.setTextColor(Color.parseColor("#564337"));
        chip.setTextSize(14);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(40));
        chip.setPadding(dp(16), 0, dp(16), 0);
        chip.setBackground(round(Color.TRANSPARENT, dp(20), Color.parseColor("#DCC1B1")));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private TextView createBasketChip(String value, int index) {
        TextView chip = new TextView(context);
        chip.setText(value + "  x");
        chip.setTextColor(Color.parseColor("#2E150B"));
        chip.setTextSize(16);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(false);
        chip.setPadding(dp(14), 0, dp(12), 0);
        chip.setBackground(round(CHIP_COLORS[index % CHIP_COLORS.length], dp(16), 0));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(44);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 0, dp(10), dp(10));
        chip.setLayoutParams(params);
        return chip;
    }

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
