package com.sotaynauan.ai.adapter.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.List;
import java.util.Locale;

public class RecipeMatchAdapter {
    public interface Listener {
        void onRecipeClick(RecipeMatch match);
    }

    private final Context context;
    private final Listener listener;

    public RecipeMatchAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bindOtherMatches(LinearLayout container, List<RecipeMatch> matches) {
        container.removeAllViews();
        for (RecipeMatch match : matches) {
            container.addView(createMatchRow(match));
        }
    }

    private View createMatchRow(RecipeMatch match) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(createRoundDrawable(Color.WHITE, dp(18)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> listener.onRecipeClick(match));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);
        row.setElevation(dp(2));

        row.addView(createFoodThumbnail(match.getRecipe()),
                new LinearLayout.LayoutParams(dp(84), dp(84)));

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setPadding(dp(16), 0, dp(8), 0);
        row.addView(textGroup, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = createText(match.getRecipe().getName(), 17,
                Color.parseColor("#2E150B"), true);
        textGroup.addView(title);
        textGroup.addView(createText(String.format(Locale.US, "Đủ %d/%d nguyên liệu",
                match.getAvailableCount(), match.getRequiredCount()), 14,
                Color.parseColor("#564337"), false));
        textGroup.addView(createText("Phù hợp " + match.getScorePercent() + "%", 14,
                Color.parseColor("#944A00"), true));

        TextView arrow = createText("›", 32, Color.parseColor("#897365"), false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(48)));
        return row;
    }

    private View createFoodThumbnail(Recipe recipe) {
        FrameLayout panel = new FrameLayout(context);
        panel.setBackground(createGradient(recipe.getColorArgb(), dp(14)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            panel.setClipToOutline(true);
        }

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        RecipeImageResolver.apply(image, recipe);
        image.setContentDescription(recipe.getName());
        panel.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView overlay = new TextView(context);
        overlay.setText(recipe.getCategory());
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(11);
        overlay.setTypeface(Typeface.DEFAULT_BOLD);
        overlay.setGravity(Gravity.BOTTOM | Gravity.START);
        overlay.setPadding(dp(8), dp(6), dp(8), dp(8));
        overlay.setBackgroundColor(Color.parseColor("#66000000"));
        panel.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        return panel;
    }

    private TextView createText(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sizeSp);
        textView.setLineSpacing(0f, 1.08f);
        textView.setPadding(0, dp(2), 0, dp(2));
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private GradientDrawable createRoundDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createGradient(int baseColor, int radius) {
        int darker = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.70f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.70f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.70f)));
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{baseColor, darker});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
