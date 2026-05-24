package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.IngredientConfirmAdapter;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.ConfirmedIngredient;
import com.sotaynauan.ai.data.model.IngredientConfirmState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

public class IngredientConfirmActivity extends Activity {
    private IngredientConfirmViewModel viewModel;
    private IngredientConfirmAdapter adapter;
    private LinearLayout ingredientList;
    private TextView statusText;
    private TextView selectedCountText;
    private Button findNearestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_confirm);

        viewModel = new IngredientConfirmViewModel(
                new AiChefRepository(new AiChefLocalDataSource(this)));
        adapter = new IngredientConfirmAdapter(this, new IngredientConfirmAdapter.Listener() {
            @Override
            public void onToggle(ConfirmedIngredient ingredient, boolean selected) {
                bindState(viewModel.toggleIngredient(ingredient.getId(), selected));
            }

            @Override
            public void onEdit(ConfirmedIngredient ingredient) {
                showIngredientDialog(ingredient);
            }

            @Override
            public void onDelete(ConfirmedIngredient ingredient) {
                bindState(viewModel.removeIngredient(ingredient.getId()));
            }
        });

        ingredientList = findViewById(R.id.confirmIngredientList);
        statusText = findViewById(R.id.confirmStatus);
        selectedCountText = findViewById(R.id.confirmSelectedCount);
        findNearestButton = findViewById(R.id.findNearestButton);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        findViewById(R.id.addConfirmedIngredientButton).setOnClickListener(view ->
                showIngredientDialog(null));
        findNearestButton.setOnClickListener(view -> continueToLocalMatch());

        bindState(viewModel.prepareState());
    }

    private void bindState(IngredientConfirmState state) {
        adapter.bind(ingredientList, state.getIngredients());
        selectedCountText.setText(state.getSelectedCount() + "/" + state.getTotalCount() + " đã chọn");
        statusText.setText(state.getLastAction());
        boolean hasSelectedIngredient = state.getSelectedCount() > 0;
        findNearestButton.setEnabled(hasSelectedIngredient);
        findNearestButton.setAlpha(hasSelectedIngredient ? 1f : 0.55f);
    }

    private void continueToLocalMatch() {
        IngredientConfirmState state = viewModel.confirmForMatching();
        bindState(state);
        startActivity(new Intent(this, AiRecipeSuggestionActivity.class));
    }

    private void showIngredientDialog(ConfirmedIngredient ingredient) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Tên nguyên liệu");
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        if (ingredient != null) {
            nameInput.setText(ingredient.getName());
        }
        form.addView(nameInput);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Số lượng, ví dụ 300g");
        quantityInput.setSingleLine(true);
        quantityInput.setInputType(InputType.TYPE_CLASS_TEXT);
        if (ingredient != null) {
            quantityInput.setText(ingredient.getQuantity());
        }
        form.addView(quantityInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(ingredient == null ? "Thêm nguyên liệu" : "Sửa nguyên liệu")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = nameInput.getText().toString();
                    String quantity = quantityInput.getText().toString();
                    IngredientConfirmState state = ingredient == null
                            ? viewModel.addIngredient(name, quantity)
                            : viewModel.updateIngredient(ingredient.getId(), name, quantity);
                    bindState(state);
                    if (!name.trim().isEmpty()) {
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
