package com.sotaynauan.ai.ui.shopping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.shopping.ShoppingListAdapter;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanItem;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.community.CommunityActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingListActivity extends Activity {
    private static final String PREFS_NAME = "shopping_list_mode";
    private static final String KEY_MARKET_MODE = "market_mode";
    private static final String FILTER_ALL = "Tất cả";
    private static final String FILTER_NEED = "Cần mua";
    private static final String FILTER_BOUGHT = "Đã mua";
    private static final String FILTER_HOME = "Đã có ở nhà";

    private ShoppingListViewModel viewModel;
    private ShoppingListAdapter adapter;
    private SharedPreferences modePreferences;
    private ShoppingPlanState currentState;
    private String activeFilter = FILTER_ALL;

    private TextView listSummary;
    private TextView emptyListText;
    private LinearLayout filterContainer;
    private LinearLayout listContainer;
    private Button shoppingModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        viewModel = new ShoppingListViewModel(
                new ShoppingRepository(new ShoppingLocalDataSource(this)));
        adapter = new ShoppingListAdapter(this, new ShoppingListAdapter.Listener() {
            @Override
            public void onBoughtChanged(ShoppingPlanItem item, boolean bought) {
                bindState(viewModel.toggleBought(item.getId(), bought));
            }

            @Override
            public void onEdit(ShoppingPlanItem item) {
                showItemDialog(item);
            }

            @Override
            public void onDelete(ShoppingPlanItem item) {
                confirmDelete(item);
            }
        });
        modePreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        bindViews();
        bindActions();
        bindState(viewModel.loadList());
    }

    private void bindViews() {
        listSummary = findViewById(R.id.listSummary);
        emptyListText = findViewById(R.id.emptyListText);
        filterContainer = findViewById(R.id.listFilterContainer);
        listContainer = findViewById(R.id.shoppingListContainer);
        shoppingModeButton = findViewById(R.id.shoppingModeButton);
    }

    private void bindActions() {
        findViewById(R.id.menuButton).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingPlanActivity.class)));
        findViewById(R.id.addMemberButton).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.addItemButton).setOnClickListener(view -> showItemDialog(null));
        shoppingModeButton.setOnClickListener(view -> toggleMarketMode());
        findViewById(R.id.homeTab).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.searchTab).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        findViewById(R.id.shoppingTab).setOnClickListener(view -> bindState(viewModel.loadList()));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void bindState(ShoppingPlanState state) {
        currentState = state;
        bindMarketModeButton();
        bindFilters();
        List<ShoppingPlanItem> items = filteredItems(state);
        emptyListText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.bind(listContainer, items);
        listSummary.setText(createSummary(state));
    }

    private void bindMarketModeButton() {
        boolean active = modePreferences.getBoolean(KEY_MARKET_MODE, false);
        shoppingModeButton.setText(active ? "✓  Đang đi chợ" : "🏃  Bật Chế độ đi chợ");
        shoppingModeButton.setAlpha(active ? 0.88f : 1f);
    }

    private void toggleMarketMode() {
        boolean nextActive = !modePreferences.getBoolean(KEY_MARKET_MODE, false);
        modePreferences.edit().putBoolean(KEY_MARKET_MODE, nextActive).apply();
        bindState(nextActive ? currentState : viewModel.finishMarketTrip());
    }

    private String createSummary(ShoppingPlanState state) {
        if (state.isEmpty()) {
            return "Chưa có item nào. Nút + sẽ lưu nguyên liệu mới vào Room local.";
        }
        String updatedAt = state.getUpdatedAtMillis() <= 0L
                ? "chưa lưu"
                : DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                .format(new Date(state.getUpdatedAtMillis()));
        return state.getNeedBuyCount() + " cần mua • " + countStatus(state, ShoppingItemStatus.BOUGHT)
                + " đã mua • " + countStatus(state, ShoppingItemStatus.AT_HOME)
                + " đã có ở nhà • cập nhật " + updatedAt;
    }

    private int countStatus(ShoppingPlanState state, ShoppingItemStatus status) {
        int count = 0;
        for (ShoppingPlanItem item : state.getItems()) {
            if (item.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private void bindFilters() {
        filterContainer.removeAllViews();
        addFilterChip(FILTER_ALL, currentState.getItems().size());
        addFilterChip(FILTER_NEED, currentState.getNeedBuyCount());
        addFilterChip(FILTER_BOUGHT, countStatus(currentState, ShoppingItemStatus.BOUGHT));
        addFilterChip(FILTER_HOME, countStatus(currentState, ShoppingItemStatus.AT_HOME));
    }

    private void addFilterChip(String label, int count) {
        TextView chip = new TextView(this);
        boolean active = label.equals(activeFilter);
        chip.setText(label.equals(FILTER_ALL) ? label : label + " (" + count + ")");
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(48));
        chip.setPadding(dp(20), 0, dp(20), 0);
        chip.setTextSize(15);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setTextColor(active ? Color.parseColor("#6F5900") : Color.parseColor("#564337"));
        chip.setBackground(filterBackground(active));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        params.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(params);
        chip.setOnClickListener(view -> {
            activeFilter = label;
            bindState(currentState);
        });
        filterContainer.addView(chip);
    }

    private List<ShoppingPlanItem> filteredItems(ShoppingPlanState state) {
        List<ShoppingPlanItem> result = new ArrayList<>();
        for (ShoppingPlanItem item : state.getItems()) {
            if (FILTER_ALL.equals(activeFilter)
                    || FILTER_NEED.equals(activeFilter) && item.getStatus() == ShoppingItemStatus.NEED_BUY
                    || FILTER_BOUGHT.equals(activeFilter) && item.getStatus() == ShoppingItemStatus.BOUGHT
                    || FILTER_HOME.equals(activeFilter) && item.getStatus() == ShoppingItemStatus.AT_HOME) {
                result.add(item);
            }
        }
        return result;
    }

    private void showItemDialog(ShoppingPlanItem item) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), 0);

        EditText nameInput = field("Tên nguyên liệu", InputType.TYPE_CLASS_TEXT);
        EditText amountInput = field("Số lượng", InputType.TYPE_CLASS_NUMBER);
        EditText unitInput = field("Đơn vị", InputType.TYPE_CLASS_TEXT);
        EditText categoryInput = field("Nhóm: Rau củ / Thịt cá / Gia vị", InputType.TYPE_CLASS_TEXT);
        if (item != null) {
            nameInput.setText(item.getName());
            amountInput.setText(String.valueOf(item.getAmount()));
            unitInput.setText(item.getUnit());
            categoryInput.setText(item.getCategory());
        }
        form.addView(nameInput);
        form.addView(amountInput);
        form.addView(unitInput);
        form.addView(categoryInput);

        new AlertDialog.Builder(this)
                .setTitle(item == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(item == null ? "Thêm" : "Lưu", (dialog, which) -> {
                    int amount = parseAmount(amountInput.getText().toString());
                    String category = categoryInput.getText().toString().trim();
                    if (category.isEmpty()) {
                        category = "Gia vị & Khác";
                    }
                    if (item == null) {
                        bindState(viewModel.addItem(nameInput.getText().toString(), amount,
                                unitInput.getText().toString(), category));
                    } else {
                        bindState(viewModel.updateItem(item.getId(), nameInput.getText().toString(),
                                amount, unitInput.getText().toString(), category));
                    }
                })
                .show();
    }

    private EditText field(String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        editText.setTextColor(Color.parseColor("#2E150B"));
        editText.setHintTextColor(Color.parseColor("#897365"));
        return editText;
    }

    private void confirmDelete(ShoppingPlanItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nguyên liệu?")
                .setMessage(item.getName())
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) ->
                        bindState(viewModel.removeItem(item.getId())))
                .show();
    }

    private int parseAmount(String value) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private GradientDrawable filterBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(18));
        drawable.setColor(active ? Color.parseColor("#FED023") : Color.parseColor("#FFE2D9"));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
