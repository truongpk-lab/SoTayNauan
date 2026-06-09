package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.BuildConfig;
import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource.GeneratedRecipe;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource.RecipeNameSuggestion;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.util.AppExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddRecipeActivity extends Activity {
    private RecipeRepository recipeRepository;
    private AiBackendRemoteDataSource aiBackendRemoteDataSource;
    private EditText dishNameInput;
    private Button suggestButton;
    private LinearLayout suggestionContainer;
    private TextView statusText;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        aiBackendRemoteDataSource = new AiBackendRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL);
        buildLayout();
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

        root.addView(text("Tạo công thức bằng AI", 30, getColorCompat(R.color.on_surface), true));
        root.addView(text("Nhập tên món bạn muốn thêm. Gemini sẽ gợi ý 5 món liên quan, sau đó tự tổng hợp công thức từ các trang hướng dẫn nấu ăn và lưu vào kho local.",
                16, getColorCompat(R.color.on_surface_variant), false));

        dishNameInput = input("Tên món hoặc ý tưởng, ví dụ gà kho");
        root.addView(dishNameInput);

        suggestButton = button("Tìm 5 món liên quan");
        suggestButton.setOnClickListener(view -> loadRelatedRecipes());
        root.addView(suggestButton);

        TextView sectionTitle = text("Món Gemini gợi ý", 20,
                getColorCompat(R.color.on_surface), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(22), 0, dp(8));
        root.addView(sectionTitle, titleParams);

        suggestionContainer = new LinearLayout(this);
        suggestionContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(suggestionContainer);

        statusText = text("Nhập tên món để bắt đầu. Công thức lưu xong sẽ mở ngay màn chi tiết và dùng được với tìm kiếm, đi chợ, chuẩn bị nấu.",
                15, getColorCompat(R.color.primary), true);
        statusText.setPadding(0, dp(14), 0, 0);
        root.addView(statusText);

        setContentView(scrollView);
    }

    private void loadRelatedRecipes() {
        if (busy) {
            return;
        }
        hideKeyboard();
        String dishName = clean(dishNameInput.getText().toString());
        if (dishName.isEmpty()) {
            statusText.setText("Hãy nhập tên món trước khi hỏi Gemini.");
            return;
        }
        setBusy(true, "Đang hỏi Gemini tìm 5 món liên quan nhất...");
        suggestionContainer.removeAllViews();
        AppExecutors.runOnIo(() -> {
            try {
                List<String> existingNames = new ArrayList<>();
                for (Recipe recipe : recipeRepository.getAllRecipes()) {
                    existingNames.add(recipe.getName());
                }
                List<RecipeNameSuggestion> suggestions =
                        aiBackendRemoteDataSource.suggestRelatedRecipes(dishName, existingNames);
                if (isActive()) {
                    runOnUiThread(() -> bindSuggestions(suggestions));
                }
            } catch (Exception exception) {
                if (isActive()) {
                    runOnUiThread(() -> setBusy(false,
                            "Không gọi được Gemini để tìm món: " + safeErrorMessage(exception)));
                }
            }
        });
    }

    private void bindSuggestions(List<RecipeNameSuggestion> suggestions) {
        if (!isActive()) {
            return;
        }
        suggestionContainer.removeAllViews();
        if (suggestions == null || suggestions.isEmpty()) {
            setBusy(false, "Gemini chưa trả về món phù hợp. Hãy thử nhập tên món rõ hơn.");
            return;
        }
        for (RecipeNameSuggestion suggestion : suggestions) {
            suggestionContainer.addView(suggestionRow(suggestion));
        }
        setBusy(false, "Chọn một món bên trên. App sẽ tự tìm công thức, chuẩn hóa dữ liệu và lưu local.");
    }

    private LinearLayout suggestionRow(RecipeNameSuggestion suggestion) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(cardBackground());
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> importSelectedRecipe(suggestion.getName()));

        TextView name = text(suggestion.getName(), 18, getColorCompat(R.color.on_surface), true);
        row.addView(name);
        if (!suggestion.getReason().isEmpty()) {
            TextView reason = text(suggestion.getReason(), 14,
                    getColorCompat(R.color.on_surface_variant), false);
            reason.setPadding(0, dp(4), 0, 0);
            row.addView(reason);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(4));
        row.setLayoutParams(params);
        return row;
    }

    private void importSelectedRecipe(String recipeName) {
        if (busy) {
            return;
        }
        setBusy(true, "Đang tìm công thức " + recipeName
                + " trên các trang hướng dẫn nấu ăn và chuẩn hóa dữ liệu...");
        AppExecutors.runOnIo(() -> {
            try {
                GeneratedRecipe generatedRecipe =
                        aiBackendRemoteDataSource.generateRecipeFromWeb(recipeName);
                String imageName = downloadRecipeImage(generatedRecipe.getImageUrl(),
                        generatedRecipe.getName());
                Recipe recipe = recipeRepository.addRecipe(generatedRecipe.getName(),
                        fallback(generatedRecipe.getDescription(),
                                "Công thức được AI tổng hợp từ các nguồn hướng dẫn nấu ăn."),
                        generatedRecipe.getTotalMinutes(),
                        fallback(generatedRecipe.getDifficulty(), "Trung bình"),
                        normalizeRecipeCategory(generatedRecipe),
                        imageName.isEmpty() ? imageNameForRecipe(generatedRecipe) : imageName,
                        fallback(generatedRecipe.getServing(), "2 người"),
                        generatedRecipe.getCalories(),
                        generatedRecipe.getCost(),
                        generatedRecipe.getIngredients(),
                        generatedRecipe.getSteps());
                if (recipe == null) {
                    if (isActive()) {
                        runOnUiThread(() -> setBusy(false,
                                "Không lưu được công thức. Tên món có thể đã tồn tại trong kho local."));
                    }
                    return;
                }
                if (isActive()) {
                    runOnUiThread(() -> openRecipeDetail(recipe));
                }
            } catch (Exception exception) {
                if (isActive()) {
                    runOnUiThread(() -> setBusy(false,
                            "Không tạo được công thức từ Gemini: " + safeErrorMessage(exception)));
                }
            }
        });
    }

    private void openRecipeDetail(Recipe recipe) {
        if (!isActive()) {
            return;
        }
        statusText.setText("Đã lưu " + recipe.getName() + " vào kho công thức local.");
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
        finish();
    }

    private void setBusy(boolean busy, String message) {
        if (!isActive()) {
            return;
        }
        this.busy = busy;
        suggestButton.setEnabled(!busy);
        dishNameInput.setEnabled(!busy);
        suggestButton.setAlpha(busy ? 0.72f : 1f);
        suggestButton.setText(busy ? "Đang xử lý..." : "Tìm 5 món liên quan");
        for (int index = 0; index < suggestionContainer.getChildCount(); index++) {
            suggestionContainer.getChildAt(index).setEnabled(!busy);
            suggestionContainer.getChildAt(index).setAlpha(busy ? 0.72f : 1f);
        }
        if (message != null && !message.trim().isEmpty()) {
            statusText.setText(message);
        }
    }

    private boolean isActive() {
        return !isFinishing() && !isDestroyed();
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(getColorCompat(R.color.on_surface));
        editText.setHintTextColor(getColorCompat(R.color.on_surface_variant));
        editText.setTextSize(16);
        editText.setSingleLine(true);
        editText.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.setMargins(0, dp(18), 0, 0);
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

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), 0x22C56A2C);
        return drawable;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setLineSpacing(0f, 1.08f);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && dishNameInput != null) {
            manager.hideSoftInputFromWindow(dishNameInput.getWindowToken(), 0);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String fallback(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isEmpty() ? fallback : cleanValue;
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "lỗi không xác định";
        }
        String compact = message.trim().replaceAll("\\s+", " ");
        String lower = compact.toLowerCase();
        if (lower.contains("resource_exhausted") || lower.contains("quota")
                || lower.contains("gemini_quota_exceeded")) {
            return "Gemini đã hết hạn mức tạm thời. Hãy đợi một lúc rồi thử lại, hoặc đổi API key/gói quota trong backend.";
        }
        if (compact.length() > 220) {
            return compact.substring(0, 220) + "...";
        }
        return compact;
    }

    private String normalizeRecipeCategory(GeneratedRecipe recipe) {
        String category = clean(recipe.getCategory());
        if (!category.isEmpty() && !"Công thức của tôi".equalsIgnoreCase(category)
                && !"Món khác".equalsIgnoreCase(category)) {
            return category;
        }
        String text = clean(recipe.getName() + " " + recipe.getDescription() + " "
                + join(recipe.getIngredients())).toLowerCase();
        if (containsAny(text, "canh", "khổ qua nhồi thịt", "kho qua nhoi thit")) {
            return "Canh";
        }
        if (containsAny(text, "kho", "rim")) {
            return "Món kho";
        }
        if (containsAny(text, "xào", "xao")) {
            return "Món xào";
        }
        if (containsAny(text, "chiên", "chien", "rán", "ran")) {
            return "Món chiên";
        }
        if (containsAny(text, "nướng", "nuong")) {
            return "Món nướng";
        }
        if (containsAny(text, "lẩu", "lau")) {
            return "Lẩu";
        }
        if (containsAny(text, "bún", "bun", "phở", "pho", "hủ tiếu", "hu tieu", "mì ", " mi ")) {
            return "Món nước";
        }
        if (containsAny(text, "cơm", "com")) {
            return "Món cơm";
        }
        if (containsAny(text, "gỏi", "goi", "salad")) {
            return "Gỏi & Salad";
        }
        if (containsAny(text, "bánh", "banh")) {
            return "Món bánh";
        }
        return "Món gia đình";
    }

    private String imageNameForRecipe(GeneratedRecipe recipe) {
        String text = clean(recipe.getName() + " " + recipe.getCategory()).toLowerCase();
        if (containsAny(text, "khổ qua", "kho qua")) {
            return "img_kho_qua_nhoi_thit";
        }
        if (containsAny(text, "canh")) {
            return "img_canh_chua";
        }
        if (containsAny(text, "kho")) {
            return "img_thit_kho_tau";
        }
        if (containsAny(text, "xào", "xao")) {
            return "img_mi_xao_bo";
        }
        if (containsAny(text, "chiên", "chien")) {
            return "img_ga_chien_nuoc_mam";
        }
        if (containsAny(text, "lẩu", "lau")) {
            return "img_lau_thai";
        }
        if (containsAny(text, "bún", "bun", "phở", "pho")) {
            return "img_pho_bo";
        }
        return "img_kho_qua_nhoi_thit";
    }

    private String downloadRecipeImage(String imageUrl, String recipeName) {
        String safeUrl = clean(imageUrl);
        if (!safeUrl.startsWith("https://") && !safeUrl.startsWith("http://")) {
            return "";
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(safeUrl).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "SoTayNauAnAI/1.0");
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                return "";
            }
            String contentType = connection.getContentType() == null
                    ? ""
                    : connection.getContentType().toLowerCase(Locale.US);
            if (!contentType.startsWith("image/")) {
                return "";
            }
            File directory = new File(getFilesDir(), "recipe_photos");
            if (!directory.exists() && !directory.mkdirs()) {
                return "";
            }
            File imageFile = new File(directory,
                    "ai_" + slug(recipeName) + "_" + System.currentTimeMillis()
                            + extensionFor(contentType, safeUrl));
            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[8192];
            int read;
            long totalBytes = 0L;
            while ((read = inputStream.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > 6L * 1024L * 1024L) {
                    inputStream.close();
                    outputStream.close();
                    return "";
                }
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.close();
            return Uri.fromFile(imageFile).toString();
        } catch (Exception exception) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extensionFor(String contentType, String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase(Locale.US);
        if (contentType.contains("png") || lowerUrl.contains(".png")) {
            return ".png";
        }
        if (contentType.contains("webp") || lowerUrl.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private String slug(String value) {
        String slug = clean(value).toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "recipe" : slug;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            builder.append(value).append(' ');
        }
        return builder.toString();
    }

    private int getColorCompat(int colorRes) {
        return getResources().getColor(colorRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
