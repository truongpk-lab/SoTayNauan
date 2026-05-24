package com.sotaynauan.ai.adapter.voice;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.model.VoiceSettings;

import java.util.Arrays;
import java.util.List;

public class VoiceProfileAdapter {
    public interface Listener {
        void onVoiceProfileSelected(String voiceProfile);
    }

    private final Context context;
    private final Listener listener;
    private final List<String> profiles = Arrays.asList(
            VoiceSettings.PROFILE_FEMALE_SOUTH,
            VoiceSettings.PROFILE_MALE_NORTH);

    public VoiceProfileAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind(LinearLayout container, String selectedProfile) {
        container.removeAllViews();
        for (String profile : profiles) {
            TextView item = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    (int) (56 * context.getResources().getDisplayMetrics().density + 0.5f),
                    1f);
            item.setLayoutParams(params);
            item.setGravity(android.view.Gravity.CENTER);
            item.setText(getProfileLabel(profile));
            item.setTextSize(17f);
            item.setTextColor(context.getColor(VoiceSettings.PROFILE_MALE_NORTH.equals(profile)
                    ? R.color.on_surface_variant
                    : R.color.primary));
            item.setTypeface(item.getTypeface(), android.graphics.Typeface.BOLD);
            boolean selected = profile.equals(selectedProfile);
            item.setBackgroundResource(selected
                    ? R.drawable.bg_voice_segment_selected
                    : R.drawable.bg_voice_segment_transparent);
            item.setTextColor(context.getColor(selected ? R.color.primary : R.color.on_surface_variant));
            item.setOnClickListener(view -> listener.onVoiceProfileSelected(profile));
            container.addView(item);
        }
    }

    private String getProfileLabel(String profile) {
        if (VoiceSettings.PROFILE_MALE_NORTH.equals(profile)) {
            return "Nam (Miền Bắc)";
        }
        return "Nữ (Miền Nam)";
    }
}
