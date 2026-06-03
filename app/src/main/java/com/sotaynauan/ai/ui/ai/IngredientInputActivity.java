package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.model.DetectedIngredient;
import com.sotaynauan.ai.data.model.IngredientInputState;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.repository.AiChefRepository;

import com.sotaynauan.ai.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class IngredientInputActivity extends Activity {
    private static final int REQUEST_CAPTURE_INGREDIENTS = 42;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private IngredientInputViewModel viewModel;
    private AiBackendRemoteDataSource aiBackendRemoteDataSource;
    private IngredientChipAdapter adapter;
    private EditText input;
    private GridLayout basketContainer;
    private TextView countText;
    private TextView statusText;
    private TextView emptyText;
    private Button ctaButton;
    private boolean detectingIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_input);

        viewModel = new IngredientInputViewModel(
                new AiChefRepository(new AiChefLocalDataSource(this),
                        null, null, AppDatabase.getInstance(this)));
        aiBackendRemoteDataSource = new AiBackendRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL);
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
                openCameraForIngredientDetection());
        ctaButton.setOnClickListener(view -> continueToSuggestions());

        adapter.bindSuggestions(findViewById(R.id.suggestionChips),
                viewModel.loadSuggestions(), ingredient -> bindState(viewModel.addIngredient(ingredient)));
        bindState(viewModel.loadState());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null && !detectingIngredients) {
            bindState(viewModel.loadState());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE_INGREDIENTS || resultCode != RESULT_OK) {
            return;
        }
        String imagePath = data == null
                ? null
                : data.getStringExtra(IngredientCameraActivity.EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.trim().isEmpty()) {
            statusText.setText("Không lấy được ảnh từ camera.");
            return;
        }
        try {
            File imageFile = new File(imagePath);
            byte[] imageBytes;
            try {
                imageBytes = scaledJpegBytes(imageFile);
            } finally {
                deleteTempImage(imageFile);
            }
            detectIngredientsFromCamera(imageBytes);
        } catch (IOException exception) {
            statusText.setText("Không đọc được ảnh từ camera: " + exception.getMessage());
        }
    }

    private void addTypedIngredient() {
        IngredientInputState state = addTypedIngredients();
        if (state != null) {
            bindState(state);
        }
    }

    private void openCameraForIngredientDetection() {
        Intent intent = new Intent(this, IngredientCameraActivity.class);
        statusText.setText("Đang mở camera trên thiết bị để chụp nguyên liệu.");
        startActivityForResult(intent, REQUEST_CAPTURE_INGREDIENTS);
    }

    private void detectIngredientsFromCamera(byte[] imageBytes) {
        detectingIngredients = true;
        statusText.setText("Đã chụp ảnh. Đang gửi backend YOLO nhận diện nguyên liệu...");
        ctaButton.setEnabled(false);
        new Thread(() -> {
            try {
                List<DetectedIngredient> ingredients = aiBackendRemoteDataSource.detectIngredientsFromImage(
                        imageBytes, "image/jpeg");
                mainHandler.post(() -> addDetectedIngredients(ingredients));
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    detectingIngredients = false;
                    bindState(viewModel.loadState());
                    statusText.setText("Không gọi được backend YOLO: " + exception.getMessage());
                });
            }
        }).start();
    }

    private void addDetectedIngredients(List<DetectedIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            detectingIngredients = false;
            bindState(viewModel.loadState());
            statusText.setText("Backend chưa nhận diện được nguyên liệu nào từ ảnh.");
            return;
        }
        IngredientInputState state = viewModel.addDetectedIngredients(ingredients);
        detectingIngredients = false;
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
