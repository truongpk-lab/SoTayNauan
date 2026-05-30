package com.sotaynauan.ai.adapter.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class MatchIngredientAdapter {
    private final Context context;

    public MatchIngredientAdapter(Context context) {
        this.context = context;
    }

    public void bindAvailable(LinearLayout container, List<String> ingredients) {
        bindPills(container, ingredients, Color.parseColor("#FFF1EC"),
                Color.parseColor("#944A00"), Color.parseColor("#FFD0BF"), "✓ ");
    }

    public void bindMissing(LinearLayout container, List<String> ingredients) {
        bindCards(container, ingredients, Color.parseColor("#FFF1F0"),
                Color.parseColor("#BA1A1A"), "Còn thiếu");
    }

    public void bindSubstitutes(LinearLayout container, List<String> substitutes) {
        bindCards(container, substitutes, Color.parseColor("#FFF2B8"),
                Color.parseColor("#735C00"), "Có thể thay");
    }

    private void bindPills(LinearLayout container, List<String> values, int backgroundColor,
                           int textColor, int strokeColor, String prefix) {
        container.removeAllViews();
        for (String value : values) {
            TextView pill = new TextView(context);
            pill.setText(prefix + value);
            pill.setTextColor(textColor);
            pill.setTextSize(15);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setGravity(Gravity.CENTER);
            pill.setMinHeight(dp(48));
            pill.setPadding(dp(14), dp(8), dp(14), dp(8));
            pill.setBackground(createRoundDrawable(backgroundColor, strokeColor, dp(22), dp(1)));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(10), dp(10));
            container.addView(pill, params);
        }
    }

    private void bindCards(LinearLayout container, List<String> values, int backgroundColor,
                           int accentColor, String label) {
        container.removeAllViews();
        for (String value : values) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setMinimumHeight(dp(76));
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            card.setBackground(createRoundDrawable(backgroundColor, accentColor, dp(20), dp(1)));

            TextView labelView = createText(label, 12, accentColor, true);
            labelView.setAllCaps(true);
            card.addView(labelView);

            TextView nameView = createText(value, 20, Color.parseColor("#2E150B"), true);
            nameView.setPadding(0, dp(4), 0, 0);
            card.addView(nameView);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(12));
            container.addView(card, params);
        }
    }

    private TextView createText(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sizeSp);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private GradientDrawable createRoundDrawable(int color, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
