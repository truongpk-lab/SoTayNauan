package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.AiChefFeature;
import com.sotaynauan.ai.data.model.AiChefState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

public class AiFlowActivity extends Activity {
    public static final String EXTRA_FEATURE_ID = "extra_feature_id";
    public static final String EXTRA_FROM_INGREDIENT_INPUT = "extra_from_ingredient_input";
    public static final String EXTRA_FROM_INGREDIENT_CONFIRM = "extra_from_ingredient_confirm";

    private AiChefViewModel viewModel;
    private AiChefFeature feature;
    private TextView stateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_flow);

        viewModel = new AiChefViewModel(new AiChefRepository(new AiChefLocalDataSource(this)));
        stateText = findViewById(R.id.flowState);
        findViewById(R.id.backButton).setOnClickListener(view -> finish());

        String featureId = getIntent().getStringExtra(EXTRA_FEATURE_ID);
        feature = viewModel.loadFeature(featureId);
        if (feature == null) {
            finish();
            return;
        }

        bindFeature();
        Button actionButton = findViewById(R.id.flowActionButton);
        actionButton.setOnClickListener(view -> continueFlow());
    }

    private void bindFeature() {
        ((TextView) findViewById(R.id.flowIcon)).setText(feature.getIconLabel());
        ((TextView) findViewById(R.id.flowTitle)).setText(feature.getTitle());
        ((TextView) findViewById(R.id.flowDescription)).setText(feature.getDescription());
        bindState(viewModel.selectFeature(feature.getId()));
        if (getIntent().getBooleanExtra(EXTRA_FROM_INGREDIENT_INPUT, false)) {
            stateText.setText("Danh sách nguyên liệu đã được lưu local. Màn xác nhận và gợi ý món sẽ dùng state này ở Phase 6-7.");
        }
        if (getIntent().getBooleanExtra(EXTRA_FROM_INGREDIENT_CONFIRM, false)) {
            stateText.setText("Nguyên liệu đã được xác nhận và lưu local. Phase 7 sẽ dùng state này để tính món phù hợp bằng thuật toán local.");
        }
    }

    private void continueFlow() {
        AiChefState state = viewModel.selectFeature(feature.getId());
        bindState(state);
    }

    private void bindState(AiChefState state) {
        stateText.setText("Đang giữ luồng \"" + feature.getTitle()
                + "\" trong local storage. Lần mở: " + state.getOpenCount()
                + ". Các bước nhập nguyên liệu, match món và đi chợ sẽ tiếp tục dùng state này.");
    }
}
