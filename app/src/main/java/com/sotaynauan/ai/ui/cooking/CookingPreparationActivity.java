package com.sotaynauan.ai.ui.cooking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.repository.CookingPreparationRepository;

import java.util.List;
import java.util.Locale;

public class CookingPreparationActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    public static final String EXTRA_PLAN_ID = "extra_plan_id";

    private CookingPreparationRepository repository;
    private LinearLayout itemContainer;
    private TextView titleText;
    private TextView statusText;
    private Button startButton;
    private String planId;
    private long recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new CookingPreparationRepository(AppDatabase.getInstance(this));
        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        planId = getIntent().getStringExtra(EXTRA_PLAN_ID);
        if ((planId == null || planId.trim().isEmpty()) && recipeId > 0L) {
            CookingPlanEntity plan = repository.preparePlanFromRecipe(recipeId, 0);
            planId = plan.id;
        }
        buildLayout();
        bindState();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#FFF8F3"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(24), dp(20), dp(16));
        root.addView(header);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(36);
        back.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        back.setTextColor(Color.parseColor("#2E150B"));
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(44)));

        titleText = new TextView(this);
        titleText.setText("Chuẩn bị nguyên liệu");
        titleText.setTextSize(28);
        titleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleText.setTextColor(Color.parseColor("#2E150B"));
        header.addView(titleText);

        statusText = new TextView(this);
        statusText.setTextSize(15);
        statusText.setTextColor(Color.parseColor("#6E5546"));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(8), 0, 0);
        header.addView(statusText, statusParams);

        ScrollView scrollView = new ScrollView(this);
        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        itemContainer.setPadding(dp(20), 0, dp(20), dp(120));
        scrollView.addView(itemContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(dp(20), dp(12), dp(20), dp(18));
        bottomBar.setBackgroundColor(Color.WHITE);
        root.addView(bottomBar);

        startButton = new Button(this);
        startButton.setText("Bắt đầu nấu");
        startButton.setTextColor(Color.WHITE);
        startButton.setTextSize(16);
        startButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startButton.setBackground(buttonBackground(Color.parseColor("#E67E22")));
        startButton.setOnClickListener(view -> startCooking());
        bottomBar.addView(startButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        Button cancelButton = new Button(this);
        cancelButton.setText("Hủy kế hoạch");
        cancelButton.setTextColor(Color.parseColor("#944A00"));
        cancelButton.setBackground(buttonBackground(Color.parseColor("#FFF1E8")));
        cancelButton.setOnClickListener(view -> {
            repository.cancelPlan(planId);
            finish();
        });
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        cancelParams.setMargins(0, dp(8), 0, 0);
        bottomBar.addView(cancelButton, cancelParams);

        setContentView(root);
    }

    private void bindState() {
        CookingPlanEntity plan = repository.getPlan(planId);
        List<CookingPlanIngredientEntity> items = repository.getPlanIngredients(planId);
        boolean ready = repository.isPlanReadyToCook(planId);
        statusText.setText(plan == null
                ? "Không tìm thấy kế hoạch nấu."
                : ready
                ? "Đã đủ nguyên liệu. Kho chỉ bị trừ khi bạn hoàn tất nấu."
                : "Kiểm tra từng nguyên liệu, phần đã có ở nhà chỉ được giữ chỗ, chưa trừ kho.");
        startButton.setEnabled(ready);
        startButton.setAlpha(ready ? 1f : 0.55f);
        itemContainer.removeAllViews();
        for (CookingPlanIngredientEntity item : items) {
            itemContainer.addView(createItemCard(item));
        }
    }

    private LinearLayout createItemCard(CookingPlanIngredientEntity item) {
        IngredientEntity ingredient = repository.getIngredient(item.ingredientId);
        String name = ingredient == null ? item.ingredientId : ingredient.name;
        double available = repository.getAvailableAmount(item.ingredientId);
        boolean presenceOnly = repository.isPresenceOnly(item);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(cardBackground());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(Color.parseColor("#2E150B"));
        nameText.setTextSize(19);
        nameText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(nameText);

        TextView detailText = new TextView(this);
        detailText.setText(presenceOnly
                ? "Chỉ cần xác nhận là đã có. Kho sẽ không trừ số lượng khi hoàn tất."
                + "\nTrạng thái: " + (item.missingAmount <= 0.0001d ? "Đã đủ" : "Cần mua hoặc xác nhận đã có")
                : "Cần: " + amountText(item.requiredAmount, item.baseUnit)
                + "\nCó thể dùng: " + amountText(available, item.baseUnit)
                + "\nĐã giữ ở nhà: " + amountText(item.homeSelectedAmount, item.baseUnit)
                + " • Đã mua: " + amountText(item.purchasedAmount, item.baseUnit)
                + "\nCần mua thêm: " + amountText(item.missingAmount, item.baseUnit));
        detailText.setTextColor(Color.parseColor("#5F493D"));
        detailText.setTextSize(15);
        detailText.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(8), 0, 0);
        card.addView(detailText, detailParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionParams.setMargins(0, dp(12), 0, 0);
        card.addView(actions, actionParams);

        Button homeButton = smallButton("Đã có ở nhà");
        homeButton.setOnClickListener(view -> {
            if (presenceOnly) {
                repository.markBought(planId, item.ingredientId, 1d, "piece");
                bindState();
                return;
            }
            showAmountDialog(
                    "Số lượng có ở nhà",
                    Math.min(item.requiredAmount, Math.max(available + item.reservedAmount, item.reservedAmount)),
                    item.baseUnit,
                    amount -> {
                        repository.markHaveAtHome(planId, item.ingredientId, amount, item.baseUnit);
                        bindState();
                    });
        });
        actions.addView(homeButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button boughtButton = smallButton("Đã mua");
        boughtButton.setOnClickListener(view -> {
            if (presenceOnly) {
                repository.markBought(planId, item.ingredientId, 1d, "piece");
                bindState();
                return;
            }
            showAmountDialog(
                    "Số lượng đã mua",
                    Math.max(item.missingAmount, 1d),
                    item.baseUnit,
                    amount -> {
                        repository.markBought(planId, item.ingredientId, amount, item.baseUnit);
                        bindState();
                    });
        });
        LinearLayout.LayoutParams boughtParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        boughtParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(boughtButton, boughtParams);
        return card;
    }

    private void showAmountDialog(String title, double defaultAmount, String unit, AmountCallback callback) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        input.setText(trimAmount(defaultAmount));
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(18), 0, dp(18), 0);
        new AlertDialog.Builder(this)
                .setTitle(title + " (" + unit + ")")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) ->
                        callback.onAmount(parseAmount(input.getText().toString())))
                .show();
    }

    private void startCooking() {
        CookingPlanEntity plan = repository.getPlan(planId);
        if (plan == null || !repository.isPlanReadyToCook(planId)) {
            bindState();
            return;
        }
        Intent intent = new Intent(this, CookingModeActivity.class);
        intent.putExtra(CookingModeActivity.EXTRA_RECIPE_ID, plan.recipeId);
        intent.putExtra(EXTRA_PLAN_ID, planId);
        startActivity(intent);
    }

    private Button smallButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.parseColor("#6F3C00"));
        button.setBackground(buttonBackground(Color.parseColor("#FFE8D4")));
        return button;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), Color.parseColor("#E8D2C4"));
        return drawable;
    }

    private GradientDrawable buttonBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private String amountText(double amount, String unit) {
        return trimAmount(amount) + " " + unit;
    }

    private String trimAmount(double amount) {
        if (Math.abs(amount - Math.round(amount)) < 0.0001d) {
            return String.valueOf(Math.round(amount));
        }
        return String.format(Locale.US, "%.1f", amount);
    }

    private double parseAmount(String value) {
        try {
            return Math.max(0d, Double.parseDouble(value.trim().replace(",", ".")));
        } catch (NumberFormatException exception) {
            return 0d;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface AmountCallback {
        void onAmount(double amount);
    }
}
