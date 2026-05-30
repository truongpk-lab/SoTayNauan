package com.sotaynauan.ai.adapter.shopping;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanItem;

import java.util.List;

public class ShoppingPlanAdapter {
    private static final ShoppingItemStatus[] PLAN_STATUSES = {
            ShoppingItemStatus.NEED_BUY,
            ShoppingItemStatus.AT_HOME,
            ShoppingItemStatus.SKIPPED
    };

    public interface Listener {
        void onStatusChanged(ShoppingPlanItem item, ShoppingItemStatus status);
        void onIncrease(ShoppingPlanItem item);
        void onDecrease(ShoppingPlanItem item);
    }

    private final Context context;
    private final Listener listener;

    public ShoppingPlanAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout mainContainer, LinearLayout otherContainer,
                     List<ShoppingPlanItem> items) {
        mainContainer.removeAllViews();
        otherContainer.removeAllViews();
        for (ShoppingPlanItem item : items) {
            if ("Nguyên liệu chính".equals(item.getCategory())) {
                mainContainer.addView(createCard(item));
            } else {
                otherContainer.addView(createCard(item));
            }
        }
    }

    private View createCard(ShoppingPlanItem item) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setBackground(cardBackground(item));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView image = new TextView(context);
        image.setText(createInitials(item.getName()));
        image.setGravity(Gravity.CENTER);
        image.setTextColor(Color.WHITE);
        image.setTextSize(19);
        image.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        image.setBackground(circleBackground(item));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        imageParams.setMargins(0, 0, dp(18), 0);
        topRow.addView(image, imageParams);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        topRow.addView(textColumn, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(context);
        name.setText(item.getName());
        name.setTextColor(Color.parseColor("#2E150B"));
        name.setTextSize(18);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textColumn.addView(name);

        TextView quantity = new TextView(context);
        quantity.setText(item.getQuantityText());
        quantity.setTextColor(Color.parseColor("#564337"));
        quantity.setTextSize(16);
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        quantityParams.setMargins(0, dp(6), 0, 0);
        textColumn.addView(quantity, quantityParams);

        LinearLayout quantityActions = new LinearLayout(context);
        quantityActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView minus = createRoundAction("-");
        TextView plus = createRoundAction("+");
        minus.setOnClickListener(view -> listener.onDecrease(item));
        plus.setOnClickListener(view -> listener.onIncrease(item));
        quantityActions.addView(minus);
        quantityActions.addView(plus);
        topRow.addView(quantityActions);

        card.addView(topRow);

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout statuses = new LinearLayout(context);
        statuses.setOrientation(LinearLayout.HORIZONTAL);
        statuses.setPadding(0, dp(16), 0, 0);
        for (ShoppingItemStatus status : PLAN_STATUSES) {
            statuses.addView(createStatusChip(item, status));
        }
        scrollView.addView(statuses);
        card.addView(scrollView);

        return card;
    }

    private TextView createStatusChip(ShoppingPlanItem item, ShoppingItemStatus status) {
        TextView chip = new TextView(context);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(48));
        chip.setPadding(dp(18), 0, dp(18), 0);
        chip.setText(status.getLabel());
        chip.setTextSize(15);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(item.getStatus() == status
                ? activeTextColor(status)
                : Color.parseColor("#564337"));
        chip.setBackground(chipBackground(item.getStatus() == status, status));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        params.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(params);
        chip.setOnClickListener(view -> listener.onStatusChanged(item, status));
        return chip;
    }

    private TextView createRoundAction(String text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Color.parseColor("#944A00"));
        button.setTextSize(20);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(chipBackground(false, ShoppingItemStatus.NEED_BUY));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(38));
        params.setMargins(dp(6), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable cardBackground(ShoppingPlanItem item) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(item.getStatus() == ShoppingItemStatus.SKIPPED
                ? Color.parseColor("#FFF1EC")
                : Color.WHITE);
        drawable.setCornerRadius(dp(28));
        drawable.setStroke(dp(1), Color.parseColor("#F1DDD3"));
        return drawable;
    }

    private GradientDrawable circleBackground(ShoppingPlanItem item) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ingredientColor(item.getName()), Color.parseColor("#46291E")});
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private GradientDrawable chipBackground(boolean active, ShoppingItemStatus status) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(24));
        if (!active) {
            drawable.setColor(Color.parseColor("#FFF8F6"));
            drawable.setStroke(dp(1), Color.parseColor("#DCC1B1"));
            return drawable;
        }
        if (status == ShoppingItemStatus.BOUGHT) {
            drawable.setColor(Color.parseColor("#20AEE5"));
        } else if (status == ShoppingItemStatus.AT_HOME) {
            drawable.setColor(Color.parseColor("#FFD23F"));
        } else if (status == ShoppingItemStatus.SKIPPED) {
            drawable.setColor(Color.parseColor("#FFDBCF"));
        } else {
            drawable.setColor(Color.parseColor("#AD5800"));
        }
        return drawable;
    }

    private int activeTextColor(ShoppingItemStatus status) {
        return status == ShoppingItemStatus.AT_HOME
                ? Color.parseColor("#6F5900")
                : Color.WHITE;
    }

    private int ingredientColor(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("cà") || lower.contains("ca")) {
            return Color.parseColor("#D84F38");
        }
        if (lower.contains("trứng") || lower.contains("trung")) {
            return Color.parseColor("#B98322");
        }
        if (lower.contains("hành") || lower.contains("hanh") || lower.contains("tỏi")) {
            return Color.parseColor("#8B6F51");
        }
        return Color.parseColor("#A83024");
    }

    private String createInitials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0) {
            return "?";
        }
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
