package com.sotaynauan.ai.adapter.recipe;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class RecipeStepAdapter {
    private final Context context;

    public RecipeStepAdapter(Context context) {
        this.context = context;
    }

    public void bind(LinearLayout container, List<String> steps) {
        container.removeAllViews();
        for (int index = 0; index < steps.size(); index++) {
            container.addView(createStepRow(index + 1, steps.get(index)));
        }
    }

    private LinearLayout createStepRow(int number, String step) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setBackground(createRowBackground());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        TextView badge = new TextView(context);
        badge.setText(String.valueOf(number));
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(15);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackground(createBadgeBackground());
        row.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView text = new TextView(context);
        text.setText(step);
        text.setTextColor(Color.parseColor("#2E150B"));
        text.setTextSize(18);
        text.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(12), 0, 0, 0);
        row.addView(text, textParams);
        return row;
    }

    private GradientDrawable createRowBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(24));
        return drawable;
    }

    private GradientDrawable createBadgeBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#944A00"));
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
