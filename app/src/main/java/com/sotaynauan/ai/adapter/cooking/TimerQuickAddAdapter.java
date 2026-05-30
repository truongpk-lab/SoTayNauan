package com.sotaynauan.ai.adapter.cooking;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import com.sotaynauan.ai.R;

public class TimerQuickAddAdapter {
    public interface OnQuickAddClickListener {
        void onQuickAddClicked(int minutes);
    }

    private final Context context;
    private final int[] quickMinutes = {1, 2, 5};

    public TimerQuickAddAdapter(Context context) {
        this.context = context;
    }

    public void bind(LinearLayout container, OnQuickAddClickListener listener) {
        container.removeAllViews();
        for (int i = 0; i < quickMinutes.length; i++) {
            int minutes = quickMinutes[i];
            Button button = new Button(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dp(56), 1f);
            if (i > 0) {
                params.setMargins(dp(8), 0, 0, 0);
            }
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.bg_timer_done_quick_add);
            button.setText("+ " + minutes + "p");
            button.setAllCaps(false);
            button.setTextColor(0xFFFFFFFF);
            button.setTextSize(17f);
            button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
            button.setOnClickListener(view -> listener.onQuickAddClicked(minutes));
            container.addView(button);
        }
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
