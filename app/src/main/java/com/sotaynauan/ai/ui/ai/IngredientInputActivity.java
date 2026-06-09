package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.IngredientChipAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.DetectedIngredient;
import com.sotaynauan.ai.data.model.IngredientInputState;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.repository.AiChefRepository;

import com.sotaynauan.ai.BuildConfig;
import com.sotaynauan.ai.util.AppExecutors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IngredientInputActivity extends Activity {
    private static final int REQUEST_CAPTURE_INGREDIENTS = 42;
    private static final int COLOR_TEXT = Color.parseColor("#2E150B");
    private static final int COLOR_MUTED = Color.parseColor("#564337");
    private static final int COLOR_PRIMARY = Color.parseColor("#944A00");
    private static final int COLOR_OUTLINE = Color.parseColor("#DCC1B1");
    private static final int COLOR_PANEL = Color.parseColor("#FFFFFF");
    private static final int COLOR_SOFT = Color.parseColor("#FFF1EC");
    private static final int COLOR_SELECTED = Color.parseColor("#FFE084");

    private IngredientInputViewModel viewModel;
    private AiBackendRemoteDataSource aiBackendRemoteDataSource;
    private IngredientChipAdapter adapter;
    private EditText input;
    private GridLayout basketContainer;
    private GridLayout ingredientChoiceGrid;
    private GridLayout categoryIngredientGrid;
    private LinearLayout quickPackContainer;
    private LinearLayout categoryTabs;
    private TextView countText;
    private TextView statusText;
    private TextView emptyText;
    private TextView searchResultLabel;
    private Button ctaButton;
    private Button cameraScanButton;
    private boolean detectingIngredients;
    private int activeCategoryIndex;
    private List<IngredientCategory> ingredientCategories;
    private List<QuickPack> quickPacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_input);

        viewModel = new IngredientInputViewModel(
                new AiChefRepository(new AiChefLocalDataSource(this),
                        null, null, AppDatabase.getInstance(this)));
        aiBackendRemoteDataSource = new AiBackendRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL);
        adapter = new IngredientChipAdapter(this);
        ingredientCategories = createIngredientCategories();
        quickPacks = createQuickPacks();

        input = findViewById(R.id.ingredientInput);
        basketContainer = findViewById(R.id.ingredientBasket);
        ingredientChoiceGrid = findViewById(R.id.ingredientChoiceGrid);
        categoryIngredientGrid = findViewById(R.id.categoryIngredientGrid);
        quickPackContainer = findViewById(R.id.quickPackContainer);
        categoryTabs = findViewById(R.id.categoryTabs);
        countText = findViewById(R.id.ingredientCount);
        statusText = findViewById(R.id.ingredientStatus);
        emptyText = findViewById(R.id.emptyIngredientState);
        searchResultLabel = findViewById(R.id.searchResultLabel);
        ctaButton = findViewById(R.id.findRecipesButton);
        cameraScanButton = findViewById(R.id.cameraScanButton);

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
                IngredientInputState state = viewModel.loadState();
                bindIngredientPickers(state);
                updateCtaState(state);
            }

            @Override
            public void afterTextChanged(Editable value) {
                // No-op.
            }
        });
        cameraScanButton.setOnClickListener(view -> openCameraForIngredientDetection());
        ctaButton.setOnClickListener(view -> continueToSuggestions());

        bindQuickPacks();
        bindCategoryTabs();
        bindState(viewModel.loadState());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isActive() && viewModel != null && !detectingIngredients) {
            bindState(viewModel.loadState());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE_INGREDIENTS || resultCode != RESULT_OK) {
            return;
        }
        if (!isActive() || detectingIngredients) {
            return;
        }
        String imagePath = data == null
                ? null
                : data.getStringExtra(IngredientCameraActivity.EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.trim().isEmpty()) {
            statusText.setText("Không lấy được ảnh từ camera.");
            return;
        }
        detectIngredientsFromCamera(imagePath);
    }

    private void addTypedIngredient() {
        IngredientInputState state = addTypedIngredients();
        if (state != null) {
            bindState(state);
        }
    }

    private void openCameraForIngredientDetection() {
        if (detectingIngredients) {
            statusText.setText("Đang nhận diện ảnh trước đó, chờ mình một chút nhé.");
            return;
        }
        Intent intent = new Intent(this, IngredientCameraActivity.class);
        statusText.setText("Đang mở camera trên thiết bị để chụp nguyên liệu.");
        startActivityForResult(intent, REQUEST_CAPTURE_INGREDIENTS);
    }

    private void detectIngredientsFromCamera(String imagePath) {
        setDetectingIngredients(true);
        statusText.setText("Đã chụp ảnh. Đang gửi backend YOLO nhận diện nguyên liệu...");
        AppExecutors.runOnIo(() -> {
            try {
                File imageFile = new File(imagePath);
                byte[] imageBytes;
                try {
                    imageBytes = scaledJpegBytes(imageFile);
                } finally {
                    deleteTempImage(imageFile);
                }
                List<DetectedIngredient> ingredients = aiBackendRemoteDataSource.detectIngredientsFromImage(
                        imageBytes, "image/jpeg");
                if (!isActive()) {
                    return;
                }
                runOnUiThread(() -> addDetectedIngredients(ingredients));
            } catch (Exception exception) {
                if (!isActive()) {
                    return;
                }
                runOnUiThread(() -> {
                    setDetectingIngredients(false);
                    bindState(viewModel.loadState());
                    statusText.setText("Không gọi được backend YOLO: " + exception.getMessage());
                });
            }
        });
    }

    private void addDetectedIngredients(List<DetectedIngredient> ingredients) {
        if (!isActive()) {
            return;
        }
        if (ingredients == null || ingredients.isEmpty()) {
            setDetectingIngredients(false);
            bindState(viewModel.loadState());
            statusText.setText("Backend chưa nhận diện được nguyên liệu nào từ ảnh.");
            return;
        }
        IngredientInputState state = viewModel.addDetectedIngredients(ingredients);
        setDetectingIngredients(false);
        bindState(state);
        statusText.setText("Đã thêm " + ingredients.size()
                + " nguyên liệu nhận diện từ camera thật. Kiểm tra số lượng ở bước xác nhận.");
    }

    private byte[] scaledJpegBytes(File imageFile) throws IOException {
        if (imageFile == null || !imageFile.exists()) {
            throw new IOException("file ảnh không tồn tại.");
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return readBytes(imageFile);
        }

        int maxDimension = 1280;
        int sampleSize = 1;
        int longestSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (longestSide / sampleSize > maxDimension) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), decodeOptions);
        if (bitmap == null) {
            return readBytes(imageFile);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream);
        bitmap.recycle();
        return stream.toByteArray();
    }

    private byte[] readBytes(File imageFile) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(imageFile);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            stream.write(buffer, 0, read);
        }
        inputStream.close();
        return stream.toByteArray();
    }

    private void deleteTempImage(File imageFile) {
        if (imageFile != null && imageFile.exists() && !imageFile.delete()) {
            imageFile.deleteOnExit();
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

    private void addIngredientList(List<String> ingredients) {
        IngredientInputState state = viewModel.loadState();
        for (String ingredient : ingredients) {
            state = viewModel.addIngredient(ingredient);
        }
        bindState(state);
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
        if (!isActive()) {
            return;
        }
        adapter.bindBasket(basketContainer, state.getIngredients(),
                ingredient -> bindState(viewModel.removeIngredient(ingredient)));
        countText.setText(state.getCount() + " món");
        emptyText.setVisibility(state.getCount() == 0 ? TextView.VISIBLE : TextView.GONE);
        bindIngredientPickers(state);
        updateCtaState(state);
        statusText.setText(state.getLastAction());
    }

    private void updateCtaState(IngredientInputState state) {
        boolean hasTypedIngredient = !input.getText().toString().trim().isEmpty();
        boolean canSuggest = !detectingIngredients && (state.getCount() > 0 || hasTypedIngredient);
        ctaButton.setEnabled(canSuggest);
        ctaButton.setAlpha(canSuggest ? 1f : 0.55f);
    }

    private void setDetectingIngredients(boolean detecting) {
        detectingIngredients = detecting;
        if (cameraScanButton != null) {
            cameraScanButton.setEnabled(!detecting);
            cameraScanButton.setAlpha(detecting ? 0.65f : 1f);
        }
        if (ctaButton != null) {
            updateCtaState(viewModel.loadState());
        }
    }

    private boolean isActive() {
        return !isFinishing() && !isDestroyed();
    }

    private void bindIngredientPickers(IngredientInputState state) {
        String query = input.getText().toString().trim();
        List<String> searchChoices = query.isEmpty()
                ? smartSuggestions(state.getIngredients())
                : searchIngredients(query);
        if (!query.isEmpty() && searchChoices.isEmpty()) {
            searchChoices.add(query);
        }
        searchResultLabel.setText(query.isEmpty()
                ? "Gợi ý phù hợp"
                : "Kết quả cho \"" + query + "\"");
        bindChoiceGrid(ingredientChoiceGrid, searchChoices, state.getIngredients(), 2);

        if (activeCategoryIndex < 0 || activeCategoryIndex >= ingredientCategories.size()) {
            activeCategoryIndex = 0;
        }
        bindChoiceGrid(categoryIngredientGrid,
                ingredientCategories.get(activeCategoryIndex).ingredients,
                state.getIngredients(), 2);
    }

    private void bindQuickPacks() {
        quickPackContainer.removeAllViews();
        for (QuickPack pack : quickPacks) {
            TextView chip = new TextView(this);
            chip.setText(pack.title + "\n" + pack.subtitle);
            chip.setTextColor(COLOR_TEXT);
            chip.setTextSize(14);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setMinHeight(dp(58));
            chip.setPadding(dp(16), 0, dp(16), 0);
            chip.setBackground(round(COLOR_PANEL, dp(8), COLOR_OUTLINE, dp(1)));
            chip.setOnClickListener(view -> addIngredientList(pack.ingredients));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(58));
            params.setMargins(0, 0, dp(10), 0);
            quickPackContainer.addView(chip, params);
        }
    }

    private void bindCategoryTabs() {
        categoryTabs.removeAllViews();
        for (int index = 0; index < ingredientCategories.size(); index++) {
            IngredientCategory category = ingredientCategories.get(index);
            TextView tab = new TextView(this);
            tab.setText(category.title);
            tab.setTextSize(14);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(dp(38));
            tab.setPadding(dp(15), 0, dp(15), 0);
            boolean selected = index == activeCategoryIndex;
            tab.setTextColor(selected ? Color.WHITE : COLOR_MUTED);
            tab.setBackground(round(selected ? COLOR_PRIMARY : Color.TRANSPARENT,
                    dp(19), COLOR_OUTLINE, dp(1)));
            int tabIndex = index;
            tab.setOnClickListener(view -> {
                activeCategoryIndex = tabIndex;
                bindCategoryTabs();
                bindIngredientPickers(viewModel.loadState());
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
            params.setMargins(0, 0, dp(8), 0);
            categoryTabs.addView(tab, params);
        }
    }

    private void bindChoiceGrid(GridLayout grid, List<String> choices, List<String> selectedIngredients,
                                int columns) {
        grid.removeAllViews();
        grid.setColumnCount(columns);
        Set<String> selectedKeys = ingredientKeySet(selectedIngredients);
        for (String choice : choices) {
            boolean selected = selectedKeys.contains(ingredientKey(choice));
            TextView chip = new TextView(this);
            chip.setText((selected ? "✓ " : "+ ") + choice);
            chip.setTextColor(selected ? COLOR_TEXT : COLOR_MUTED);
            chip.setTextSize(15);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(false);
            chip.setPadding(dp(10), 0, dp(10), 0);
            chip.setBackground(round(selected ? COLOR_SELECTED : COLOR_SOFT,
                    dp(8), selected ? 0 : COLOR_OUTLINE, selected ? 0 : dp(1)));
            chip.setOnClickListener(view -> bindState(selected
                    ? viewModel.removeIngredient(choice)
                    : viewModel.addIngredient(choice)));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(42);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 0, dp(9), dp(9));
            grid.addView(chip, params);
        }
    }

    private List<String> smartSuggestions(List<String> selectedIngredients) {
        Set<String> selected = ingredientKeySet(selectedIngredients);
        List<String> choices = new ArrayList<>();
        addChoices(choices, selected, viewModel.loadSuggestions());
        addChoices(choices, selected, Arrays.asList("Trứng gà", "Cà chua", "Thịt gà", "Đậu hũ"));
        for (IngredientCategory category : ingredientCategories) {
            addChoices(choices, selected, category.ingredients);
            if (choices.size() >= 8) {
                break;
            }
        }
        return choices.size() > 8 ? new ArrayList<>(choices.subList(0, 8)) : choices;
    }

    private void addChoices(List<String> choices, Set<String> selected, List<String> candidates) {
        Set<String> existing = ingredientKeySet(choices);
        for (String candidate : candidates) {
            String key = ingredientKey(candidate);
            if (!key.isEmpty() && !selected.contains(key) && !existing.contains(key)) {
                choices.add(candidate);
                existing.add(key);
            }
        }
    }

    private List<String> searchIngredients(String query) {
        String queryKey = ingredientKey(query);
        List<String> results = new ArrayList<>();
        Set<String> existing = new HashSet<>();
        for (IngredientCategory category : ingredientCategories) {
            for (String ingredient : category.ingredients) {
                String key = ingredientKey(ingredient);
                if (key.contains(queryKey) && !existing.contains(key)) {
                    results.add(ingredient);
                    existing.add(key);
                }
                if (results.size() >= 8) {
                    return results;
                }
            }
        }
        return results;
    }

    private Set<String> ingredientKeySet(List<String> ingredients) {
        Set<String> keys = new HashSet<>();
        for (String ingredient : ingredients) {
            String key = ingredientKey(ingredient);
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String ingredientKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.US)
                .trim()
                .replaceAll("[^a-z0-9]+", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private List<IngredientCategory> createIngredientCategories() {
        List<IngredientCategory> categories = new ArrayList<>();
        categories.add(new IngredientCategory("Hay dùng", Arrays.asList(
                "Tỏi", "Hành tím", "Hành tây", "Nước mắm", "Đường", "Muối",
                "Tiêu", "Dầu ăn", "Ớt", "Gừng")));
        categories.add(new IngredientCategory("Đạm", Arrays.asList(
                "Trứng gà", "Thịt gà", "Thịt heo", "Thịt bò", "Cá", "Tôm",
                "Mực", "Đậu hũ", "Chả cá", "Sườn non")));
        categories.add(new IngredientCategory("Rau củ", Arrays.asList(
                "Cà chua", "Rau muống", "Cải xanh", "Bắp cải", "Cà rốt",
                "Khoai tây", "Dưa leo", "Bí đỏ", "Nấm", "Hành lá")));
        categories.add(new IngredientCategory("Tinh bột", Arrays.asList(
                "Gạo", "Cơm nguội", "Bún", "Mì", "Phở", "Bánh mì", "Miến",
                "Bột mì", "Bột gạo", "Khoai lang")));
        categories.add(new IngredientCategory("Gia vị", Arrays.asList(
                "Dầu hào", "Nước tương", "Tương ớt", "Sa tế", "Bột ngọt",
                "Hạt nêm", "Mật ong", "Giấm", "Chanh", "Sả")));
        return categories;
    }

    private List<QuickPack> createQuickPacks() {
        List<QuickPack> packs = new ArrayList<>();
        packs.add(new QuickPack("Bữa cơm nhà", "8 món hay có", Arrays.asList(
                "Gạo", "Trứng gà", "Thịt heo", "Cà chua", "Rau muống",
                "Tỏi", "Nước mắm", "Hành lá")));
        packs.add(new QuickPack("Nấu nhanh", "15 phút", Arrays.asList(
                "Trứng gà", "Cà chua", "Hành lá", "Tỏi", "Dầu ăn", "Nước mắm")));
        packs.add(new QuickPack("Tủ lạnh hôm nay", "rau + đạm", Arrays.asList(
                "Thịt gà", "Cà rốt", "Khoai tây", "Hành tây", "Nấm", "Tiêu")));
        packs.add(new QuickPack("Món nước", "bún/phở/mì", Arrays.asList(
                "Bún", "Thịt bò", "Hành tây", "Hành lá", "Gừng", "Nước mắm")));
        return packs;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0 && strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class IngredientCategory {
        final String title;
        final List<String> ingredients;

        IngredientCategory(String title, List<String> ingredients) {
            this.title = title;
            this.ingredients = ingredients;
        }
    }

    private static class QuickPack {
        final String title;
        final String subtitle;
        final List<String> ingredients;

        QuickPack(String title, String subtitle, List<String> ingredients) {
            this.title = title;
            this.subtitle = subtitle;
            this.ingredients = ingredients;
        }
    }
}
