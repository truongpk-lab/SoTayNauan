package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.net.Uri;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddRecipeActivity extends Activity {
    private static final int REQUEST_CAPTURE_PHOTO = 41;
    private static final int REQUEST_PICK_PHOTO = 42;

    private RecipeRepository recipeRepository;
    private EditText nameInput;
    private EditText descriptionInput;
    private EditText minutesInput;
    private EditText servingInput;
    private EditText ingredientsInput;
    private EditText stepsInput;
    private ImageView photoPreview;
    private TextView statusText;
    private String photoUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        buildLayout();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_PICK_PHOTO) {
            Uri selectedImage = data.getData();
            if (selectedImage == null) {
                statusText.setText("Không lấy được ảnh đã chọn.");
                return;
            }
            photoUri = saveRecipePhoto(selectedImage);
            if (photoUri.isEmpty()) {
                statusText.setText("Không lưu được ảnh món ăn đã chọn.");
                return;
            }
            photoPreview.setImageURI(Uri.parse(photoUri));
            statusText.setText("Đã chọn ảnh món ăn cho công thức mới.");
            return;
        }
        if (requestCode != REQUEST_CAPTURE_PHOTO) {
            return;
        }
        Object rawBitmap = data.getExtras() == null ? null : data.getExtras().get("data");
        if (!(rawBitmap instanceof Bitmap)) {
            statusText.setText("Không lấy được ảnh từ camera.");
            return;
        }
        Bitmap bitmap = (Bitmap) rawBitmap;
        photoUri = saveRecipePhoto(bitmap);
        if (photoUri.isEmpty()) {
            statusText.setText("Không lưu được ảnh món ăn.");
            return;
        }
        photoPreview.setImageURI(Uri.parse(photoUri));
        statusText.setText("Đã chụp ảnh món ăn cho công thức mới.");
    }

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColorCompat(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(28));
        scrollView.addView(root);

        TextView back = text("‹ Quay lại AI Chef", 16, getColorCompat(R.color.primary), true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setMinHeight(dp(48));
        back.setOnClickListener(view -> finish());
        root.addView(back);

        root.addView(text("Thêm công thức mới", 30, getColorCompat(R.color.on_surface), true));
        root.addView(text("Nhập công thức hiện có hoặc món mới của bạn. Có thể chụp ảnh món ăn rồi lưu vào kho local.",
                16, getColorCompat(R.color.on_surface_variant), false));

        photoPreview = new ImageView(this);
        photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoPreview.setImageResource(R.drawable.cooking_step_preview);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190));
        photoParams.setMargins(0, dp(18), 0, dp(12));
        root.addView(photoPreview, photoParams);

        Button captureButton = button("Chụp ảnh món ăn");
        captureButton.setOnClickListener(view -> capturePhoto());
        root.addView(captureButton);

        Button pickButton = button("Chọn ảnh từ thư viện");
        pickButton.setOnClickListener(view -> pickPhoto());
        root.addView(pickButton);

        nameInput = input("Tên món", false);
        descriptionInput = input("Mô tả món ăn", true);
        minutesInput = input("Thời gian nấu, ví dụ 30", false);
        servingInput = input("Khẩu phần, ví dụ 2 người", false);
        ingredientsInput = input("Nguyên liệu, mỗi dòng một nguyên liệu", true);
        stepsInput = input("Các bước nấu, mỗi dòng một bước", true);
        root.addView(nameInput);
        root.addView(descriptionInput);
        root.addView(minutesInput);
        root.addView(servingInput);
        root.addView(ingredientsInput);
        root.addView(stepsInput);

        Button saveButton = button("Lưu công thức");
        saveButton.setOnClickListener(view -> saveRecipe());
        root.addView(saveButton);

        statusText = text("Công thức mới sẽ được lưu local và hiển thị cùng 41 công thức có sẵn.",
                15, getColorCompat(R.color.primary), true);
        statusText.setPadding(0, dp(12), 0, 0);
        root.addView(statusText);

        setContentView(scrollView);
    }

    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            statusText.setText("Thiết bị chưa có ứng dụng camera để chụp ảnh.");
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_PHOTO);
    }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) == null) {
            statusText.setText("Thiết bị chưa có ứng dụng thư viện ảnh.");
            return;
        }
        startActivityForResult(intent, REQUEST_PICK_PHOTO);
    }

    private void saveRecipe() {
        hideKeyboard();
        String name = clean(nameInput.getText().toString());
        String description = clean(descriptionInput.getText().toString());
        List<String> ingredients = splitLines(ingredientsInput.getText().toString());
        List<String> steps = splitLines(stepsInput.getText().toString());
        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            statusText.setText("Hãy nhập tên món, nguyên liệu và ít nhất một bước nấu.");
            return;
        }
        int minutes = parseMinutes(minutesInput.getText().toString());
        Recipe recipe = recipeRepository.addRecipe(name,
                description.isEmpty() ? "Công thức tự thêm từ bếp nhà." : description,
                minutes,
                minutes <= 25 ? "Dễ" : "Trung bình",
                "Công thức của tôi",
                photoUri,
                clean(servingInput.getText().toString()).isEmpty()
                        ? "2 người"
                        : clean(servingInput.getText().toString()),
                "",
                "",
                ingredients,
                steps);
        if (recipe == null) {
            statusText.setText("Không lưu được công thức. Tên món có thể đã tồn tại.");
            return;
        }
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
        finish();
    }

    private String saveRecipePhoto(Bitmap bitmap) {
        try {
            File directory = new File(getFilesDir(), "recipe_photos");
            if (!directory.exists() && !directory.mkdirs()) {
                return "";
            }
            File photoFile = new File(directory,
                    String.format(Locale.US, "recipe_%d.jpg", System.currentTimeMillis()));
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream);
            outputStream.close();
            return Uri.fromFile(photoFile).toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private String saveRecipePhoto(Uri sourceUri) {
        try {
            File directory = new File(getFilesDir(), "recipe_photos");
            if (!directory.exists() && !directory.mkdirs()) {
                return "";
            }
            File photoFile = new File(directory,
                    String.format(Locale.US, "recipe_%d.jpg", System.currentTimeMillis()));
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                return "";
            }
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.close();
            return Uri.fromFile(photoFile).toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private List<String> splitLines(String raw) {
        List<String> values = new ArrayList<>();
        String[] parts = raw == null ? new String[0] : raw.split("[\\n,;]+");
        for (String part : parts) {
            String value = clean(part);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private int parseMinutes(String raw) {
        try {
            return Math.max(5, Integer.parseInt(clean(raw)));
        } catch (NumberFormatException exception) {
            return 30;
        }
    }

    private EditText input(String hint, boolean multiline) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(getColorCompat(R.color.on_surface));
        editText.setHintTextColor(getColorCompat(R.color.on_surface_variant));
        editText.setTextSize(16);
        editText.setSingleLine(!multiline);
        editText.setMinLines(multiline ? 3 : 1);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                multiline ? dp(110) : dp(56));
        params.setMargins(0, dp(12), 0, 0);
        editText.setLayoutParams(params);
        return editText;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setBackgroundResource(R.drawable.bg_primary_button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setLineSpacing(0f, 1.08f);
        if (bold) {
            textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && nameInput != null) {
            manager.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private int getColorCompat(int colorRes) {
        return getResources().getColor(colorRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
