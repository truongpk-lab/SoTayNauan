package com.sotaynauan.ai.adapter.profile;

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

public class ProfileSavedRecipeAdapter {
    public interface Listener {
        void onRecipeClick(Recipe recipe);
    }

    private final Context context;
    private final Listener listener;

    public ProfileSavedRecipeAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<Recipe> recipes) {
        container.removeAllViews();
        for (Recipe recipe : recipes) {
            container.addView(createRecipeCard(recipe));
        }
    }

    private View createRecipeCard(Recipe recipe) {
        LinearLayout card = new LinearLayout(context);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(Color.WHITE, dp(22), 0));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setOnClickListener(view -> listener.onRecipeClick(recipe));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(220), dp(112));
        params.setMargins(0, 0, dp(12), dp(8));
        card.setLayoutParams(params);

        TextView colorBlock = new TextView(context);
        colorBlock.setText(recipe.getCategory());
        colorBlock.setTextColor(Color.WHITE);
        colorBlock.setTextSize(11);
        colorBlock.setTypeface(Typeface.DEFAULT_BOLD);
        colorBlock.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        colorBlock.setPadding(dp(6), dp(6), dp(6), dp(8));
        colorBlock.setBackground(round(recipe.getColorArgb(), dp(18), 0));
        card.addView(colorBlock, new LinearLayout.LayoutParams(dp(74), dp(82)));

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(12), 0, 0, 0);
        card.addView(textGroup, textParams);

        TextView title = text(recipe.getName(), 15, Color.parseColor("#2E150B"), true);
        title.setMaxLines(2);
        textGroup.addView(title);
        textGroup.addView(text(String.format(Locale.US, "%d phút", recipe.getTotalMinutes()),
                13, Color.parseColor("#944A00"), false));
        return card;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(sizeSp);
        text.setLineSpacing(0f, 1.08f);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return text;
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

