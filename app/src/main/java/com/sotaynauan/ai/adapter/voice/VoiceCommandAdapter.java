package com.sotaynauan.ai.adapter.voice;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.model.VoiceCommand;

import java.util.List;

public class VoiceCommandAdapter {
    public interface Listener {
        void onVoiceCommandClick(VoiceCommand command);
    }

    private final Context context;
    private final Listener listener;

    public VoiceCommandAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, List<VoiceCommand> commands) {
        container.removeAllViews();
        if (commands == null) {
            return;
        }
        for (VoiceCommand command : commands) {
            Button button = new Button(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(4, 0, 4, 0);
            button.setLayoutParams(params);
            button.setMinHeight(dp(48));
            button.setBackgroundResource(R.drawable.bg_voice_quick_command);
            button.setText(command.getLabel());
            button.setAllCaps(false);
            button.setTextColor(0xFF944A00);
            button.setTextSize(14f);
            button.setOnClickListener(view -> listener.onVoiceCommandClick(command));
            container.addView(button);
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
