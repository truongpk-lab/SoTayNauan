package com.sotaynauan.ai.adapter.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.ConfirmedIngredient;

import java.util.List;

public class IngredientConfirmAdapter {
    public interface Listener {
        void onToggle(ConfirmedIngredient ingredient, boolean selected);

        void onEdit(ConfirmedIngredient ingredient);

        void onDelete(ConfirmedIngredient ingredient);
    }

    private final Context context;
    private final Listener listener;

    public IngredientConfirmAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<ConfirmedIngredient> ingredients) {
        container.removeAllViews();
        for (ConfirmedIngredient ingredient : ingredients) {
            container.addView(createIngredientCard(ingredient));
        }
    }

    private View createIngredientCard(ConfirmedIngredient ingredient) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(14), dp(16));
        card.setMinimumHeight(dp(98));
        card.setElevation(dp(3));
        card.setBackground(round(Color.WHITE, dp(28), 0, 0));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(ingredient.isSelected());
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A64F00")));
        checkBox.setMinWidth(dp(44));
        checkBox.setMinHeight(dp(44));
        card.addView(checkBox, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout infoGroup = new LinearLayout(context);
        infoGroup.setOrientation(LinearLayout.VERTICAL);
        infoGroup.setPadding(dp(12), 0, dp(10), 0);
        card.addView(infoGroup, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(context);
        name.setText(ingredient.getName());
        name.setTextColor(Color.parseColor("#2E150B"));
        name.setTextSize(20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        infoGroup.addView(name);

        LinearLayout metaRow = new LinearLayout(context);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, dp(8), 0, 0);
        infoGroup.addView(metaRow);

        TextView source = new TextView(context);
        source.setText(sourceLabel(ingredient.getSource()));
        source.setTextColor(sourceTextColor(ingredient.getSource()));
        source.setTextSize(13);
        source.setTypeface(Typeface.DEFAULT_BOLD);
        source.setGravity(Gravity.CENTER);
        source.setPadding(dp(10), 0, dp(10), 0);
        source.setMinHeight(dp(34));
        source.setBackground(round(sourceColor(ingredient.getSource()), dp(17), 0, 0));
        metaRow.addView(source);

        TextView quantity = new TextView(context);
        String quantityText = ingredient.getQuantity().isEmpty()
                ? "Số lượng: Chưa rõ"
                : "Số lượng: " + ingredient.getQuantity();
        quantity.setText(quantityText);
        quantity.setTextColor(Color.parseColor("#564337"));
        quantity.setTextSize(16);
        quantity.setPadding(dp(12), 0, 0, 0);
        metaRow.addView(quantity);

        TextView editButton = createIconButton("✎", Color.parseColor("#564337"));
        editButton.setOnClickListener(view -> listener.onEdit(ingredient));
        card.addView(editButton);

        TextView deleteButton = createIconButton("⌫", Color.parseColor("#BA1A1A"));
        deleteButton.setOnClickListener(view -> listener.onDelete(ingredient));
        card.addView(deleteButton);

        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                listener.onToggle(ingredient, isChecked));
        return card;
    }

    private TextView createIconButton(String text, int color) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(color);
        button.setTextSize(25);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(48));
        params.setMargins(dp(2), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private String sourceLabel(String source) {
        if (ConfirmedIngredient.SOURCE_CAMERA.equals(source)) {
            return "▣ Camera";
        }
        if (ConfirmedIngredient.SOURCE_KITCHEN.equals(source)) {
            return "▤ Tủ bếp";
        }
        return "⌨ Nhập tay";
    }

    private int sourceColor(String source) {
        if (ConfirmedIngredient.SOURCE_CAMERA.equals(source)) {
            return Color.parseColor("#FFDBCF");
        }
        if (ConfirmedIngredient.SOURCE_KITCHEN.equals(source)) {
            return Color.parseColor("#FED023");
        }
        return Color.parseColor("#FFE2D9");
    }

    private int sourceTextColor(String source) {
        if (ConfirmedIngredient.SOURCE_KITCHEN.equals(source)) {
            return Color.parseColor("#574500");
        }
        return Color.parseColor("#564337");
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0 && strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
