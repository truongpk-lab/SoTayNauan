package com.sotaynauan.ai.ui.shopping;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.shopping.ShoppingPlanAdapter;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanItem;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingPlanActivity extends Activity {
    private ShoppingPlanViewModel viewModel;
    private ShoppingPlanAdapter adapter;
    private TextView recipeSubtitle;
    private TextView planSummary;
    private TextView emptyPlanText;
    private LinearLayout filterChipContainer;
    private LinearLayout mainIngredientContainer;
    private LinearLayout otherIngredientContainer;
    private Button createShoppingListButton;
    private ShoppingPlanState currentState;
    private String activeFilter = FILTER_ALL;

    private static final String FILTER_ALL = "Tất cả";
    private static final String FILTER_MAIN = "Nguyên liệu chính";
    private static final String FILTER_NEED_BUY = "Cần mua";
    private static final String FILTER_AT_HOME = "Đã có ở nhà";
    private static final String FILTER_SKIPPED = "Bỏ qua";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_plan);

        viewModel = new ShoppingPlanViewModel(
                new ShoppingRepository(new ShoppingLocalDataSource(this)));
        adapter = new ShoppingPlanAdapter(this, new ShoppingPlanAdapter.Listener() {
            @Override
            public void onStatusChanged(com.sotaynauan.ai.data.model.ShoppingPlanItem item,
                                        ShoppingItemStatus status) {
                bindState(viewModel.updateStatus(item.getId(), status));
            }

            @Override
            public void onIncrease(com.sotaynauan.ai.data.model.ShoppingPlanItem item) {
                bindState(viewModel.increaseQuantity(item.getId()));
            }

            @Override
            public void onDecrease(com.sotaynauan.ai.data.model.ShoppingPlanItem item) {
                bindState(viewModel.decreaseQuantity(item.getId()));
            }
        });

        bindViews();
        bindActions();
        bindState(viewModel.loadPlan());
    }

    private void bindViews() {
        recipeSubtitle = findViewById(R.id.recipeSubtitle);
        planSummary = findViewById(R.id.planSummary);
        emptyPlanText = findViewById(R.id.emptyPlanText);
        filterChipContainer = findViewById(R.id.filterChipContainer);
        mainIngredientContainer = findViewById(R.id.mainIngredientContainer);
        otherIngredientContainer = findViewById(R.id.otherIngredientContainer);
        createShoppingListButton = findViewById(R.id.createShoppingListButton);
    }

    private void bindActions() {
        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        createShoppingListButton.setOnClickListener(view -> {
            bindState(viewModel.createShoppingList());
            startActivity(new Intent(this, ShoppingListActivity.class));
        });
        LinearLayout bottomBar = (LinearLayout) createShoppingListButton.getParent();
        LinearLayout tabs = (LinearLayout) bottomBar.getChildAt(1);
        tabs.getChildAt(0).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        tabs.getChildAt(3).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        tabs.getChildAt(2).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        tabs.getChildAt(4).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void bindState(ShoppingPlanState state) {
        currentState = state;
        boolean isEmpty = state.isEmpty();
        emptyPlanText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        createShoppingListButton.setEnabled(!isEmpty);
        createShoppingListButton.setAlpha(isEmpty ? 0.55f : 1f);

        recipeSubtitle.setText(state.getRecipeName().isEmpty()
                ? "Chọn món từ AI Chef để tạo kế hoạch mua nguyên liệu."
                : "Chuẩn bị cho món \"" + state.getRecipeName() + "\"");
        planSummary.setText(createSummary(state));
        bindFilters();
        adapter.bind(mainIngredientContainer, otherIngredientContainer,
                filteredItems(state));
    }

    private String createSummary(ShoppingPlanState state) {
        if (state.isEmpty()) {
            return "Kế hoạch trống. Dữ liệu sẽ được lưu local sau khi bạn thêm nguyên liệu thiếu.";
        }
        String updatedAt = state.getUpdatedAtMillis() <= 0L
                ? "chưa lưu"
                : DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                .format(new Date(state.getUpdatedAtMillis()));
        String action = state.getStatusMessage().isEmpty()
                ? "Chọn trạng thái, chỉnh số lượng rồi tạo danh sách đi chợ."
                : state.getStatusMessage();
        return state.getNeedBuyCount() + " cần mua • "
                + state.getShoppingListCount() + " trong danh sách • cập nhật " + updatedAt
                + "\n" + action;
    }

    private void bindFilters() {
        filterChipContainer.removeAllViews();
        addFilterChip(FILTER_ALL);
        addFilterChip(FILTER_MAIN);
        addFilterChip(FILTER_NEED_BUY);
        addFilterChip(FILTER_AT_HOME);
        addFilterChip(FILTER_SKIPPED);
    }

    private void addFilterChip(String label) {
        TextView chip = new TextView(this);
        boolean active = label.equals(activeFilter);
        chip.setText(label);
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(46));
        chip.setPadding(dp(18), 0, dp(18), 0);
        chip.setTextSize(15);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(active ? Color.WHITE : Color.parseColor("#564337"));
        chip.setBackground(filterBackground(active));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(46));
        params.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(params);
        chip.setOnClickListener(view -> {
            activeFilter = label;
            bindState(currentState);
        });
        filterChipContainer.addView(chip);
    }

    private GradientDrawable filterBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(23));
        drawable.setColor(active ? Color.parseColor("#E67E22") : Color.WHITE);
        drawable.setStroke(dp(1), active ? Color.TRANSPARENT : Color.parseColor("#DCC1B1"));
        return drawable;
    }

    private List<ShoppingPlanItem> filteredItems(ShoppingPlanState state) {
        if (FILTER_ALL.equals(activeFilter)) {
            return state.getItems();
        }
        List<ShoppingPlanItem> filtered = new ArrayList<>();
        for (ShoppingPlanItem item : state.getItems()) {
            if (FILTER_MAIN.equals(activeFilter) && "Nguyên liệu chính".equals(item.getCategory())) {
                filtered.add(item);
            } else if (FILTER_NEED_BUY.equals(activeFilter)
                    && item.getStatus() == ShoppingItemStatus.NEED_BUY) {
                filtered.add(item);
            } else if (FILTER_AT_HOME.equals(activeFilter)
                    && item.getStatus() == ShoppingItemStatus.AT_HOME) {
                filtered.add(item);
            } else if (FILTER_SKIPPED.equals(activeFilter)
                    && item.getStatus() == ShoppingItemStatus.SKIPPED) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
