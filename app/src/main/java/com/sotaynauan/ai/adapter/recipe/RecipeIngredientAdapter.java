package com.sotaynauan.ai.adapter.recipe;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class RecipeIngredientAdapter {
    public interface OnIngredientCheckedListener {
        void onIngredientChecked(String ingredient);
    }

    private final Context context;
    private final OnIngredientCheckedListener listener;

    public RecipeIngredientAdapter(Context context, OnIngredientCheckedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<String> ingredients, List<String> checkedIngredients) {
        container.removeAllViews();
        for (String ingredient : ingredients) {
            container.addView(createIngredientRow(ingredient, contains(checkedIngredients, ingredient)));
        }
    }

    private LinearLayout createIngredientRow(String ingredient, boolean checked) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setMinimumHeight(dp(64));
        row.setBackground(createRowBackground());

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);

        CheckBox checkBox = new CheckBox(context);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#944A00")));
        checkBox.setChecked(checked);
        checkBox.setOnCheckedChangeListener(null);
        row.addView(checkBox, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView name = new TextView(context);
        name.setText(displayName(ingredient));
        name.setTextColor(Color.parseColor("#2E150B"));
        name.setTextSize(18);
        name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(name, nameParams);

        TextView amount = new TextView(context);
        amount.setText(displayAmount(ingredient));
        amount.setTextColor(Color.parseColor("#6B4D3E"));
        amount.setTextSize(14);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setGravity(android.view.Gravity.CENTER);
        amount.setPadding(dp(12), dp(5), dp(12), dp(5));
        amount.setBackground(createAmountBackground());
        row.addView(amount, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        CompoundButton.OnCheckedChangeListener checkedListener = (buttonView, isChecked) ->
                listener.onIngredientChecked(ingredient);
        checkBox.setOnCheckedChangeListener(checkedListener);
        row.setOnClickListener(view -> checkBox.performClick());
        return row;
    }

    private boolean contains(List<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(String ingredient) {
        String lower = ingredient.toLowerCase();
        if (lower.contains("thịt bò")) {
            return "Thịt bò thăn";
        }
        if (lower.contains("tỏi")) {
            return "Hành tím, tỏi";
        }
        return ingredient;
    }

    private String displayAmount(String ingredient) {
        String lower = ingredient.toLowerCase();
        if (lower.contains("thịt bò")) return "300g";
        if (lower.contains("cà chua")) return "3 quả";
        if (lower.contains("trứng")) return "2 quả";
        if (lower.contains("tỏi") || lower.contains("hành tím")) return "1 củ";
        if (lower.contains("hành lá")) return "Vừa đủ";
        if (lower.contains("cơm")) return "2 chén";
        if (lower.contains("gà")) return "400g";
        if (lower.contains("nước mắm") || lower.contains("tiêu")) return "Vừa đủ";
        return "Vừa đủ";
    }

    private GradientDrawable createRowBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(24));
        return drawable;
    }

    private GradientDrawable createAmountBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#FFE2D9"));
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
