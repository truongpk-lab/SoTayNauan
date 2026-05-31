package com.sotaynauan.ai.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.datasource.ProfileLocalDataSource;
import com.sotaynauan.ai.ui.voice.VoiceSettingsActivity;

public class AppSettingsActivity extends Activity {
    private ProfileLocalDataSource dataSource;
    private Switch notificationSwitch;
    private Switch compactSwitch;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataSource = new ProfileLocalDataSource(this);
        buildLayout();
    }

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getResources().getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(28));
        scrollView.addView(root);

        TextView back = text("‹ Quay lại", 16, getResources().getColor(R.color.primary), true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setMinHeight(dp(48));
        back.setOnClickListener(view -> finish());
        root.addView(back);

        root.addView(text("Cài đặt app", 30,
                getResources().getColor(R.color.on_surface), true));
        root.addView(text("Điều chỉnh thông báo, cách hiển thị và giọng nói trong ứng dụng.",
                16, getResources().getColor(R.color.on_surface_variant), false));

        notificationSwitch = settingSwitch(root, "Thông báo nấu ăn",
                "Bật/tắt nhắc nhở và trạng thái trong app.");
        compactSwitch = settingSwitch(root, "Màn hình gọn",
                "Giảm bớt phần mô tả phụ ở các màn cá nhân.");

        Button voiceButton = actionButton("Cài đặt giọng nói");
        voiceButton.setOnClickListener(view ->
                startActivity(new Intent(this, VoiceSettingsActivity.class)));
        root.addView(voiceButton);

        Button saveButton = actionButton("Lưu cài đặt");
        saveButton.setBackgroundResource(R.drawable.bg_primary_button);
        saveButton.setTextColor(getResources().getColor(R.color.on_primary));
        saveButton.setOnClickListener(view -> saveSettings());
        root.addView(saveButton);

        statusText = text("", 15, getResources().getColor(R.color.primary), true);
        statusText.setPadding(0, dp(14), 0, 0);
        root.addView(statusText);

        bindSavedValues();
        setContentView(scrollView);
    }

    private Switch settingSwitch(LinearLayout root, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(18), 0, dp(6));

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        row.addView(textGroup, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textGroup.addView(text(title, 18, getResources().getColor(R.color.on_surface), true));
        textGroup.addView(text(subtitle, 14, getResources().getColor(R.color.on_surface_variant), false));

        Switch valueSwitch = new Switch(this);
        row.addView(valueSwitch);
        root.addView(row);
        return valueSwitch;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(getResources().getColor(R.color.primary));
        button.setBackgroundResource(R.drawable.bg_auth_secondary_button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.setMargins(0, dp(14), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void bindSavedValues() {
        notificationSwitch.setChecked(dataSource.isNotificationsEnabled());
        compactSwitch.setChecked(dataSource.isCompactModeEnabled());
        statusText.setText("Cài đặt hiện tại: "
                + (dataSource.isNotificationsEnabled() ? "thông báo bật" : "thông báo tắt")
                + ", " + (dataSource.isCompactModeEnabled() ? "màn gọn bật" : "màn gọn tắt")
                + ".");
    }

    private void saveSettings() {
        dataSource.setNotificationsEnabled(notificationSwitch.isChecked());
        dataSource.setCompactModeEnabled(compactSwitch.isChecked());
        statusText.setText("Đã lưu cài đặt ứng dụng.");
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setLineSpacing(0f, 1.08f);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
