package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.IngredientChipAdapter;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.IngredientInputState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

public class IngredientInputActivity extends Activity {
    private IngredientInputViewModel viewModel;
    private IngredientChipAdapter adapter;
    private EditText input;
    private GridLayout basketContainer;
    private TextView countText;
    private TextView statusText;
    private TextView emptyText;
    private Button ctaButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_input);

        viewModel = new IngredientInputViewModel(
                new AiChefRepository(new AiChefLocalDataSource(this)));
        adapter = new IngredientChipAdapter(this);

        input = findViewById(R.id.ingredientInput);
        basketContainer = findViewById(R.id.ingredientBasket);
        countText = findViewById(R.id.ingredientCount);
        statusText = findViewById(R.id.ingredientStatus);
        emptyText = findViewById(R.id.emptyIngredientState);
        ctaButton = findViewById(R.id.findRecipesButton);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        findViewById(R.id.addIngredientButton).setOnClickListener(view -> addTypedIngredient());
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTypedIngredients();
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateCtaState(viewModel.loadState());
            }

            @Override
            public void afterTextChanged(Editable value) {
                // No-op.
            }
        });
        findViewById(R.id.cameraScanButton).setOnClickListener(view ->
                bindState(viewModel.scanOfflineSample()));
        ctaButton.setOnClickListener(view -> continueToSuggestions());

        adapter.bindSuggestions(findViewById(R.id.suggestionChips),
                viewModel.loadSuggestions(), ingredient -> bindState(viewModel.addIngredient(ingredient)));
        bindState(viewModel.loadState());
    }

    private void addTypedIngredient() {
        IngredientInputState state = addTypedIngredients();
        if (state != null) {
            bindState(state);
        }
    }

    private IngredientInputState addTypedIngredients() {
        String raw = input.getText().toString();
        input.setText("");
        IngredientInputState state = viewModel.loadState();
        String[] ingredients = raw.split("[,;\\n]+");
        for (String ingredient : ingredients) {
            if (!ingredient.trim().isEmpty()) {
                state = viewModel.addIngredient(ingredient);
            }
        }
        bindState(state);
        return state;
    }

    private void continueToSuggestions() {
        IngredientInputState state = input.getText().toString().trim().isEmpty()
                ? viewModel.loadState()
                : addTypedIngredients();
        if (state.getCount() == 0) {
            bindState(viewModel.addIngredient(""));
            return;
        }
        state = viewModel.markReadyForSuggestions();
        bindState(state);
        startActivity(new Intent(this, IngredientConfirmActivity.class));
    }

    private void bindState(IngredientInputState state) {
        adapter.bindBasket(basketContainer, state.getIngredients(),
                ingredient -> bindState(viewModel.removeIngredient(ingredient)));
        countText.setText(state.getCount() + " món");
        emptyText.setVisibility(state.getCount() == 0 ? TextView.VISIBLE : TextView.GONE);
        updateCtaState(state);
        statusText.setText(state.getLastAction());
    }

    private void updateCtaState(IngredientInputState state) {
        boolean hasTypedIngredient = !input.getText().toString().trim().isEmpty();
        boolean canSuggest = state.getCount() > 0 || hasTypedIngredient;
        ctaButton.setEnabled(canSuggest);
        ctaButton.setAlpha(canSuggest ? 1f : 0.55f);
    }
}
