package com.sotaynauan.ai.adapter.profile;

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
import com.sotaynauan.ai.util.RecipeImageResolver;

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
        if (recipes.isEmpty()) {
            TextView empty = text("Chưa có công thức tự lưu. Vào AI Chef > Tạo công thức để thêm món mới.",
                    14, Color.parseColor("#897365"), false);
            empty.setPadding(dp(4), dp(6), dp(4), dp(12));
            container.addView(empty, new LinearLayout.LayoutParams(dp(280),
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
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

        card.addView(createFoodThumbnail(recipe), new LinearLayout.LayoutParams(dp(74), dp(82)));

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

    private View createFoodThumbnail(Recipe recipe) {
        FrameLayout panel = new FrameLayout(context);
        panel.setBackground(round(recipe.getColorArgb(), dp(18), 0));
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

        TextView label = new TextView(context);
        label.setText(recipe.getCategory());
        label.setTextColor(Color.WHITE);
        label.setTextSize(10);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        label.setPadding(dp(5), dp(5), dp(5), dp(7));
        label.setBackgroundColor(Color.parseColor("#66000000"));
        panel.addView(label, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        return panel;
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
