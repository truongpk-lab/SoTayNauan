package com.sotaynauan.ai.adapter.shopping;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListAdapter {
    public interface Listener {
        void onBoughtChanged(ShoppingPlanItem item, boolean bought);
        void onEdit(ShoppingPlanItem item);
        void onDelete(ShoppingPlanItem item);
    }

    private final Context context;
    private final Listener listener;

    public ShoppingListAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<ShoppingPlanItem> items) {
        container.removeAllViews();
        Map<String, List<ShoppingPlanItem>> groups = groupItems(items);
        for (Map.Entry<String, List<ShoppingPlanItem>> entry : groups.entrySet()) {
            container.addView(createSectionTitle(entry.getKey()));
            container.addView(createGroupCard(entry.getValue()));
        }
    }

    private Map<String, List<ShoppingPlanItem>> groupItems(List<ShoppingPlanItem> items) {
        Map<String, List<ShoppingPlanItem>> groups = new LinkedHashMap<>();
        for (ShoppingPlanItem item : items) {
            String category = displayCategory(item);
            if (!groups.containsKey(category)) {
                groups.put(category, new ArrayList<>());
            }
            groups.get(category).add(item);
        }
        return groups;
    }

    private TextView createSectionTitle(String title) {
        TextView textView = new TextView(context);
        textView.setText(sectionIcon(title) + "  " + title);
        textView.setTextColor(Color.parseColor("#944A00"));
        textView.setTextSize(24);
        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(28), 0, dp(12));
        textView.setLayoutParams(params);
        return textView;
    }

    private View createGroupCard(List<ShoppingPlanItem> items) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(cardBackground());
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);
        for (int index = 0; index < items.size(); index++) {
            card.addView(createItemRow(items.get(index)));
            if (index < items.size() - 1) {
                card.addView(createDivider());
            }
        }
        return card;
    }

    private View createItemRow(ShoppingPlanItem item) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(72));

        CheckBox checkBox = new CheckBox(context);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                item.getStatus() == ShoppingItemStatus.BOUGHT
                        ? Color.parseColor("#E67E22")
                        : Color.parseColor("#897365")));
        checkBox.setChecked(item.getStatus() == ShoppingItemStatus.BOUGHT);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onBoughtChanged(item, isChecked));
        row.addView(checkBox, new LinearLayout.LayoutParams(dp(48), dp(56)));

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(textColumn, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(context);
        name.setText(item.getName());
        name.setTextColor(Color.parseColor("#2E150B"));
        name.setTextSize(21);
        if (item.getStatus() == ShoppingItemStatus.BOUGHT) {
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        textColumn.addView(name);

        TextView detail = new TextView(context);
        detail.setText(item.getQuantityText() + " • " + item.getStatus().getLabel());
        detail.setTextColor(Color.parseColor("#564337"));
        detail.setTextSize(15);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(4), 0, 0);
        textColumn.addView(detail, detailParams);

        TextView edit = action("Sửa");
        edit.setOnClickListener(view -> listener.onEdit(item));
        row.addView(edit);

        TextView delete = action("Xóa");
        delete.setTextColor(Color.parseColor("#BA1A1A"));
        delete.setOnClickListener(view -> listener.onDelete(item));
        row.addView(delete);
        return row;
    }

    private TextView action(String text) {
        TextView action = new TextView(context);
        action.setText(text);
        action.setGravity(Gravity.CENTER);
        action.setTextColor(Color.parseColor("#944A00"));
        action.setTextSize(13);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(48));
        params.setMargins(dp(4), 0, 0, 0);
        action.setLayoutParams(params);
        return action;
    }

    private View createDivider() {
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#F1DDD3"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return divider;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(30));
        return drawable;
    }

    private String displayCategory(ShoppingPlanItem item) {
        String category = item.getCategory();
        if (category.contains("chính")) {
            return "Thịt cá";
        }
        if (item.getName().toLowerCase().contains("rau")
                || item.getName().toLowerCase().contains("cà")
                || item.getName().toLowerCase().contains("ca chua")) {
            return "Rau củ";
        }
        return "Gia vị";
    }

    private String sectionIcon(String title) {
        if ("Rau củ".equals(title)) {
            return "⌘";
        }
        if ("Thịt cá".equals(title)) {
            return "▣";
        }
        return "⚑";
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
