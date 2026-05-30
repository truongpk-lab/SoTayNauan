package com.sotaynauan.ai.adapter.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.AiChefFeature;

import java.util.List;

public class AiChefFeatureAdapter {
    public interface OnFeatureClickListener {
        void onFeatureClick(AiChefFeature feature);
    }

    private final Context context;
    private final OnFeatureClickListener clickListener;

    public AiChefFeatureAdapter(Context context, OnFeatureClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public void bindFeatures(LinearLayout container, List<AiChefFeature> features) {
        container.removeAllViews();
        for (AiChefFeature feature : features) {
            container.addView(createFeatureCard(feature));
        }
    }

    private View createFeatureCard(AiChefFeature feature) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(feature.isPrimary() ? 5 : 3));
        card.setPadding(dp(24), dp(22), dp(22), dp(22));
        card.setBackground(createCardBackground(feature));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                feature.isPrimary() ? dp(164) : dp(118));
        params.setMargins(0, 0, 0, dp(18));
        card.setLayoutParams(params);

        TextView icon = new TextView(context);
        icon.setText(feature.getIconLabel());
        icon.setTextColor(feature.getAccentColor());
        icon.setTextSize(feature.isPrimary() ? 15 : 13);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(createRoundDrawable(lighten(feature.getAccentColor()), dp(32), 0));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                feature.isPrimary() ? dp(62) : dp(54),
                feature.isPrimary() ? dp(62) : dp(54));
        card.addView(icon, iconParams);

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.setPadding(dp(18), 0, 0, 0);
        card.addView(textGroup, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(context);
        title.setText(feature.getTitle());
        title.setTextColor(Color.parseColor("#2E150B"));
        title.setTextSize(feature.isPrimary() ? 23 : 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(false);
        textGroup.addView(title);

        TextView description = new TextView(context);
        description.setText(feature.getDescription());
        description.setTextColor(Color.parseColor("#564337"));
        description.setTextSize(15);
        description.setLineSpacing(0f, 1.12f);
        description.setPadding(0, dp(8), 0, 0);
        textGroup.addView(description);

        card.setOnClickListener(view -> clickListener.onFeatureClick(feature));
        return card;
    }

    private GradientDrawable createCardBackground(AiChefFeature feature) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(30));
        drawable.setColor(feature.isPrimary() ? Color.WHITE : Color.parseColor("#FFFDFB"));
        int strokeColor = feature.isPrimary() ? 0 : Color.parseColor("#FFD0BF");
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
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

    private int lighten(int color) {
        int red = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * 0.78f));
        int green = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * 0.78f));
        int blue = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * 0.78f));
        return Color.rgb(red, green, blue);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
