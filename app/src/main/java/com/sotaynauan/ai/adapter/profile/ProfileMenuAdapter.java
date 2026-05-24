package com.sotaynauan.ai.adapter.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.ProfileMenuItem;

import java.util.List;

public class ProfileMenuAdapter {
    public interface Listener {
        void onMenuItemClick(ProfileMenuItem item);
    }

    private final Context context;
    private final Listener listener;

    public ProfileMenuAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<ProfileMenuItem> items) {
        container.removeAllViews();
        for (ProfileMenuItem item : items) {
            container.addView(createMenuRow(item));
            if (items.indexOf(item) < items.size() - 1) {
                container.addView(createDivider());
            }
        }
    }

    private View createMenuRow(ProfileMenuItem item) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(8), dp(12), dp(8));
        row.setMinimumHeight(dp(64));
        row.setClickable(true);
        row.setOnClickListener(view -> listener.onMenuItemClick(item));

        TextView icon = new TextView(context);
        icon.setText(item.getIconText());
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Color.parseColor("#944A00"));
        icon.setTextSize(item.getIconText().length() > 2 ? 11 : 16);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setBackground(round(Color.parseColor("#FFF1EC"), dp(22), 0));
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView title = new TextView(context);
        title.setText(item.getTitle());
        title.setTextColor(Color.parseColor("#2E150B"));
        title.setTextSize(17);
        title.setSingleLine(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(14), 0, dp(8), 0);
        row.addView(title, titleParams);

        if (item.hasBadge()) {
            TextView badge = new TextView(context);
            badge.setText(item.getBadge());
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(Color.parseColor("#6F5900"));
            badge.setTextSize(12);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setPadding(dp(8), dp(4), dp(8), dp(4));
            badge.setBackground(round(Color.parseColor("#FED023"), dp(16), 0));
            row.addView(badge);
        }

        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setGravity(Gravity.CENTER);
        chevron.setTextColor(Color.parseColor("#564337"));
        chevron.setTextSize(30);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), dp(44)));
        return row;
    }

    private View createDivider() {
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#EAD7CC"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        params.setMargins(dp(16), 0, dp(16), 0);
        divider.setLayoutParams(params);
        return divider;
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

