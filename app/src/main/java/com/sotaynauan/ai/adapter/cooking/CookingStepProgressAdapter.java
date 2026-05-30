package com.sotaynauan.ai.adapter.cooking;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class CookingStepProgressAdapter {
    private final Context context;

    public CookingStepProgressAdapter(Context context) {
        this.context = context;
    }

    public void bind(LinearLayout container, List<String> steps, int currentStepIndex, boolean completed) {
        container.removeAllViews();
        for (int index = 0; index < steps.size(); index++) {
            container.addView(createStepDot(index, currentStepIndex, completed));
        }
    }

    private TextView createStepDot(int index, int currentStepIndex, boolean completed) {
        boolean isDone = completed || index < currentStepIndex;
        boolean isCurrent = !completed && index == currentStepIndex;

        TextView dot = new TextView(context);
        dot.setText(String.valueOf(index + 1));
        dot.setGravity(android.view.Gravity.CENTER);
        dot.setTextSize(13);
        dot.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dot.setTextColor(isDone || isCurrent ? Color.WHITE : Color.parseColor("#944A00"));
        dot.setBackground(createBackground(isDone, isCurrent));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
        params.setMargins(0, 0, dp(8), 0);
        dot.setLayoutParams(params);
        return dot;
    }

    private GradientDrawable createBackground(boolean isDone, boolean isCurrent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        if (isDone) {
            drawable.setColor(Color.parseColor("#6F5900"));
        } else if (isCurrent) {
            drawable.setColor(Color.parseColor("#944A00"));
        } else {
            drawable.setColor(Color.parseColor("#FFF1EC"));
            drawable.setStroke(dp(1), Color.parseColor("#DCC1B1"));
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
