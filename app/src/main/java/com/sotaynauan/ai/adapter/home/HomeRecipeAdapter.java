package com.sotaynauan.ai.adapter.home;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.Recipe;

import java.util.List;
import java.util.Locale;

public class HomeRecipeAdapter {
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final Context context;
    private final OnRecipeClickListener clickListener;

    public HomeRecipeAdapter(Context context, OnRecipeClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public void bindHorizontalCards(LinearLayout container, List<Recipe> recipes) {
        container.removeAllViews();
        for (Recipe recipe : recipes) {
            container.addView(createSuggestionCard(recipe));
        }
    }

    public void bindPopularCards(LinearLayout container, List<Recipe> recipes) {
        container.removeAllViews();
        for (Recipe recipe : recipes) {
            container.addView(createPopularCard(recipe));
        }
    }

    public void bindFriendShares(LinearLayout container, List<Recipe> recipes) {
        container.removeAllViews();
        for (Recipe recipe : recipes) {
            if (recipe.getFriendNote().isEmpty()) {
                continue;
            }
            container.addView(createFriendCard(recipe));
        }
    }

    private View createSuggestionCard(Recipe recipe) {
        LinearLayout card = createCard(recipe, dp(224), dp(220), dp(16));
        card.setOrientation(LinearLayout.VERTICAL);

        TextView image = createFoodPanel(recipe, dp(188), recipe.getCategory());
        card.addView(image);
        card.addView(createTitle(recipe.getName(), 18, Color.parseColor("#2E150B")));
        card.addView(createMeta(recipe));
        return card;
    }

    private View createPopularCard(Recipe recipe) {
        LinearLayout card = createCard(recipe, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, dp(14));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView image = createFoodPanel(recipe, dp(76), recipe.getCategory());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(94), dp(76));
        card.addView(image, imageParams);

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setPadding(dp(14), 0, 0, 0);
        card.addView(textGroup, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textGroup.addView(createTitle(recipe.getName(), 17, Color.parseColor("#2E150B")));
        textGroup.addView(createBody(recipe.getDescription(), 14, Color.parseColor("#564337")));
        textGroup.addView(createMeta(recipe));
        return card;
    }

    private View createFriendCard(Recipe recipe) {
        LinearLayout card = createCard(recipe, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, dp(16));
        card.setOrientation(LinearLayout.VERTICAL);
        card.addView(createTitle(recipe.getFriendName(), 16, Color.parseColor("#2E150B")));
        card.addView(createBody(recipe.getFriendNote(), 15, Color.parseColor("#564337")));
        card.addView(createFoodPanel(recipe, dp(150), recipe.getName()));
        card.addView(createMeta(recipe));
        return card;
    }

    private LinearLayout createCard(Recipe recipe, int width, int height, int padding) {
        LinearLayout card = new LinearLayout(context);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(createRoundDrawable(Color.WHITE, dp(22), 0));
        card.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, 0, dp(14), dp(14));
        card.setLayoutParams(params);
        card.setElevation(dp(3));
        card.setOnClickListener(view -> clickListener.onRecipeClick(recipe));
        return card;
    }

    private TextView createFoodPanel(Recipe recipe, int height, String label) {
        TextView panel = new TextView(context);
        panel.setText(label);
        panel.setTextColor(Color.WHITE);
        panel.setTextSize(15);
        panel.setTypeface(Typeface.DEFAULT_BOLD);
        panel.setGravity(Gravity.BOTTOM | Gravity.START);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(createGradient(recipe.getColorArgb()));
        panel.setMinHeight(height);
        return panel;
    }

    private TextView createTitle(String value, int sizeSp, int color) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sizeSp);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setPadding(0, dp(10), 0, dp(3));
        textView.setSingleLine(false);
        return textView;
    }

    private TextView createMeta(Recipe recipe) {
        String meta = String.format(Locale.US, "%d phút  •  %s", recipe.getTotalMinutes(), recipe.getDifficulty());
        return createBody(meta, 13, Color.parseColor("#944A00"));
    }

    private TextView createBody(String value, int sizeSp, int color) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sizeSp);
        textView.setLineSpacing(0f, 1.08f);
        textView.setPadding(0, dp(4), 0, dp(6));
        return textView;
    }

    private GradientDrawable createRoundDrawable(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable createGradient(int baseColor) {
        int darker = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.72f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.72f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.72f)));
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{baseColor, darker});
        drawable.setCornerRadius(dp(20));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
