package com.sotaynauan.ai.ui.profile;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.datasource.ProfileLocalDataSource;

public class AiTasteProfileActivity extends Activity {
    private ProfileLocalDataSource dataSource;
    private RadioGroup styleGroup;
    private RadioGroup spiceGroup;
    private SeekBar timeSeekBar;
    private TextView timeText;
    private CheckBox vegetarianCheck;
    private CheckBox budgetCheck;
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

        root.addView(text("Hồ sơ khẩu vị AI", 30,
                getResources().getColor(R.color.on_surface), true));
        root.addView(text("Chọn khẩu vị để AI ưu tiên món phù hợp hơn khi gợi ý.",
                16, getResources().getColor(R.color.on_surface_variant), false));

        styleGroup = sectionRadio(root, "Khẩu vị chính",
                new String[]{"Đậm đà gia đình", "Thanh nhẹ", "Món chay", "Nhiều đạm"});
        spiceGroup = sectionRadio(root, "Độ cay",
                new String[]{"Không cay", "Vừa phải", "Cay nhiều"});

        root.addView(sectionTitle("Thời gian nấu tối đa"));
        timeText = text("", 15, getResources().getColor(R.color.primary), true);
        root.addView(timeText);
        timeSeekBar = new SeekBar(this);
        timeSeekBar.setMax(110);
        timeSeekBar.setProgress(Math.max(10, dataSource.getAiMaxCookingTime()) - 10);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op.
            }
        });
        root.addView(timeSeekBar);

        vegetarianCheck = new CheckBox(this);
        vegetarianCheck.setText("Ưu tiên món chay khi có thể");
        vegetarianCheck.setTextSize(16);
        vegetarianCheck.setTextColor(getResources().getColor(R.color.on_surface));
        root.addView(vegetarianCheck);

        budgetCheck = new CheckBox(this);
        budgetCheck.setText("Ưu tiên món tiết kiệm chi phí");
        budgetCheck.setTextSize(16);
        budgetCheck.setTextColor(getResources().getColor(R.color.on_surface));
        root.addView(budgetCheck);

        Button saveButton = new Button(this);
        saveButton.setText("Lưu khẩu vị AI");
        saveButton.setAllCaps(false);
        saveButton.setTextColor(getResources().getColor(R.color.on_primary));
        saveButton.setBackgroundResource(R.drawable.bg_primary_button);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        saveParams.setMargins(0, dp(18), 0, 0);
        root.addView(saveButton, saveParams);
        saveButton.setOnClickListener(view -> saveTasteProfile());

        statusText = text("", 15, getResources().getColor(R.color.primary), true);
        statusText.setPadding(0, dp(14), 0, 0);
        root.addView(statusText);

        bindSavedValues();
        setContentView(scrollView);
    }

    private RadioGroup sectionRadio(LinearLayout root, String title, String[] values) {
        root.addView(sectionTitle(title));
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        for (String value : values) {
            RadioButton button = new RadioButton(this);
            button.setText(value);
            button.setTextSize(16);
            button.setTextColor(getResources().getColor(R.color.on_surface));
            group.addView(button);
        }
        root.addView(group);
        return group;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 18, getResources().getColor(R.color.on_surface), true);
        title.setPadding(0, dp(20), 0, dp(8));
        return title;
    }

    private void bindSavedValues() {
        checkRadio(styleGroup, dataSource.getAiTasteStyle());
        checkRadio(spiceGroup, dataSource.getAiSpiceLevel());
        vegetarianCheck.setChecked(dataSource.isAiVegetarianPreferred());
        budgetCheck.setChecked(dataSource.isAiBudgetFriendlyPreferred());
        updateTimeText();
        statusText.setText("Khẩu vị hiện tại: " + dataSource.getAiTasteStyle()
                + ", " + dataSource.getAiSpiceLevel() + ".");
    }

    private void saveTasteProfile() {
        dataSource.setAiTasteStyle(selectedRadioText(styleGroup, "Đậm đà gia đình"));
        dataSource.setAiSpiceLevel(selectedRadioText(spiceGroup, "Vừa phải"));
        dataSource.setAiMaxCookingTime(timeSeekBar.getProgress() + 10);
        dataSource.setAiVegetarianPreferred(vegetarianCheck.isChecked());
        dataSource.setAiBudgetFriendlyPreferred(budgetCheck.isChecked());
        statusText.setText("Đã lưu hồ sơ khẩu vị AI.");
    }

    private void checkRadio(RadioGroup group, String value) {
        for (int index = 0; index < group.getChildCount(); index++) {
            RadioButton button = (RadioButton) group.getChildAt(index);
            if (button.getText().toString().equals(value)) {
                button.setChecked(true);
                return;
            }
        }
        ((RadioButton) group.getChildAt(0)).setChecked(true);
    }

    private String selectedRadioText(RadioGroup group, String fallback) {
        int checkedId = group.getCheckedRadioButtonId();
        RadioButton button = checkedId == -1 ? null : findViewById(checkedId);
        return button == null ? fallback : button.getText().toString();
    }

    private void updateTimeText() {
        timeText.setText((timeSeekBar.getProgress() + 10) + " phút");
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
