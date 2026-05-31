package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.AiChefFeatureAdapter;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.AiChefFeature;
import com.sotaynauan.ai.data.model.AiChefState;
import com.sotaynauan.ai.data.repository.AiChefRepository;
import com.sotaynauan.ai.ui.community.CommunityActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;

public class AiChefActivity extends Activity {
    private AiChefViewModel viewModel;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chef);

        viewModel = new AiChefViewModel(new AiChefRepository(new AiChefLocalDataSource(this)));
        statusText = findViewById(R.id.aiChefStatus);

        AiChefFeatureAdapter adapter = new AiChefFeatureAdapter(this, this::openFeature);
        adapter.bindFeatures(findViewById(R.id.aiFeatureContainer), viewModel.loadFeatures());
        bindState(viewModel.loadState());
        bindBottomNavigation();
    }

    private void openFeature(AiChefFeature feature) {
        AiChefState state = viewModel.selectFeature(feature.getId());
        bindState(state);
        if (AiChefRepository.FLOW_INGREDIENT_MATCH.equals(feature.getId())) {
            startActivity(new Intent(this, IngredientInputActivity.class));
            return;
        }
        if (AiChefRepository.FLOW_CREATE_RECIPE.equals(feature.getId())) {
            startActivity(new Intent(this, AddRecipeActivity.class));
            return;
        }
        Intent intent = new Intent(this, AiFlowActivity.class);
        intent.putExtra(AiFlowActivity.EXTRA_FEATURE_ID, feature.getId());
        startActivity(intent);
    }

    private void bindState(AiChefState state) {
        AiChefFeature activeFeature = viewModel.loadFeature(state.getActiveFeatureId());
        if (activeFeature == null) {
            statusText.setText(R.string.ai_chef_state_empty);
            return;
        }
        statusText.setText("Luồng gần nhất: " + activeFeature.getTitle()
                + " - đã lưu local " + state.getOpenCount() + " lần mở.");
    }

    private void bindBottomNavigation() {
        findViewById(R.id.homeTab).setOnClickListener(view -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.searchTab).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.aiChefTab).setOnClickListener(view -> bindState(viewModel.loadState()));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}
