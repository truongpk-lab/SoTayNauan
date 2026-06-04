package com.sotaynauan.ai.ui.search;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.home.HomeRecipeAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.CommunityRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.community.CommunityActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.util.AppExecutors;
import com.sotaynauan.ai.util.AppNavigator;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchActivity extends Activity {
    private static final String CATEGORY_FRIED = "chiên";
    private static final String CATEGORY_STIR_FRIED = "xào";
    private static final String CATEGORY_GRILLED = "nướng";
    private static final String CATEGORY_SOUP = "món nước";
    private static final String CATEGORY_CANH = "canh";
    private static final String CATEGORY_BRAISED = "kho";
    private static final String CATEGORY_RICE = "cơm";
    private static final String CATEGORY_NOODLE = "bún phở";
    private static final String CATEGORY_HOTPOT = "lẩu";
    private static final String CATEGORY_SALAD = "gỏi salad";
    private static final String CATEGORY_CAKE = "bánh";
    private static final String CATEGORY_STEAMED = "hấp";
    private static final String CATEGORY_BOILED = "luộc";
    private static final String CATEGORY_DESSERT = "tráng miệng";
    private static final String CATEGORY_DRINK = "đồ uống";

    private static final String DIFFICULTY_EASY = "dễ";
    private static final String DIFFICULTY_MEDIUM = "trung bình";
    private static final String DIFFICULTY_HARD = "khó";

    private RecipeRepository recipeRepository;
    private CommunityRepository communityRepository;
    private HomeRecipeAdapter recipeAdapter;

    private final List<Recipe> allRecipes = new ArrayList<>();
    private final List<CommunityFriend> allUsers = new ArrayList<>();

    private String query = "";
    private String selectedCategory = "";
    private String selectedDifficulty = "";

    private TextView statusText;
    private TextView resultTitle;
    private TextView emptyText;
    private TextView peopleTitle;
    private EditText searchInput;
    private LinearLayout categoryContainer;
    private LinearLayout difficultyContainer;
    private LinearLayout resultsContainer;
    private LinearLayout peopleContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        communityRepository = new CommunityRepository(
                new CommunityLocalDataSource(AppDatabase.getInstance(this).communityDao(),
                        new SeedDataProvider()),
                new CommunityMapper());
        recipeAdapter = new HomeRecipeAdapter(this, this::openRecipe);

        buildLayout();
        bindFilters();
        bindResults();
        loadSearchData();
    }

    private void loadSearchData() {
        statusText.setText("Đang tải món ăn và dữ liệu cộng đồng...");
        AppExecutors.runOnIo(
                () -> new SearchData(recipeRepository.getAllRecipes(), loadUsers()),
                data -> {
                    if (!canBindUi()) {
                        return;
                    }
                    allRecipes.clear();
                    allRecipes.addAll(data.recipes);
                    allUsers.clear();
                    allUsers.addAll(data.users);
                    bindResults();
                },
                exception -> {
                    if (canBindUi()) {
                        statusText.setText("Chưa tải được dữ liệu tìm kiếm: "
                                + exception.getMessage());
                    }
                });
    }

    private void buildLayout() {
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(getColor(R.color.background));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, dp(104));
        screen.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scrollView.addView(root);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(headerRow);

        TextView backButton = text("Trang chủ", 15, getColor(R.color.primary), true);
        backButton.setBackground(round(Color.parseColor("#FFF1EC"), dp(18), Color.parseColor("#FFD7C7")));
        backButton.setGravity(Gravity.CENTER);
        backButton.setPadding(dp(14), dp(10), dp(14), dp(10));
        backButton.setOnClickListener(view -> AppNavigator.openTopLevel(this, HomeActivity.class));
        headerRow.addView(backButton);

        TextView title = text("Tìm kiếm món ăn", 28, getColor(R.color.on_surface), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(14), 0, 0, 0);
        headerRow.addView(title, titleParams);

        TextView communityBadge = text("Cộng đồng", 13, Color.parseColor("#944A00"), true);
        communityBadge.setPadding(dp(12), dp(8), dp(12), dp(8));
        communityBadge.setBackground(round(Color.parseColor("#FFF1EC"), dp(18), 0));
        communityBadge.setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        headerRow.addView(communityBadge);

        statusText = text(
                "Chọn danh mục món ăn hoặc nhập từ khóa để xem gợi ý món liên quan.",
                15,
                getColor(R.color.on_surface_variant),
                false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(12), 0, 0);
        root.addView(statusText, statusParams);

        searchInput = new EditText(this);
        searchInput.setHint("Tìm món ăn hoặc tên bạn bè...");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(17);
        searchInput.setTextColor(getColor(R.color.on_surface));
        searchInput.setHintTextColor(getColor(R.color.on_surface_variant));
        searchInput.setBackgroundResource(R.drawable.bg_community_search);
        searchInput.setPadding(dp(20), 0, dp(20), 0);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        searchParams.setMargins(0, dp(18), 0, 0);
        root.addView(searchInput, searchParams);

        addSectionTitle(root, "Danh mục món ăn", dp(22));
        categoryContainer = new LinearLayout(this);
        categoryContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryParams.setMargins(0, dp(12), 0, 0);
        root.addView(categoryContainer, categoryParams);

        addSectionTitle(root, "Độ khó khi tìm", dp(22));
        difficultyContainer = new LinearLayout(this);
        difficultyContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams difficultyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        difficultyParams.setMargins(0, dp(12), 0, 0);
        root.addView(difficultyContainer, difficultyParams);

        resultTitle = text("Gợi ý món ăn", 21, getColor(R.color.on_surface), true);
        LinearLayout.LayoutParams resultTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        resultTitleParams.setMargins(0, dp(22), 0, 0);
        root.addView(resultTitle, resultTitleParams);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resultContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        resultContainerParams.setMargins(0, dp(12), 0, 0);
        root.addView(resultsContainer, resultContainerParams);

        emptyText = text("Không tìm thấy món phù hợp với bộ lọc hiện tại.", 15,
                getColor(R.color.on_surface_variant), false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setVisibility(View.GONE);
        root.addView(emptyText);

        peopleTitle = text("Người dùng liên quan", 21, getColor(R.color.on_surface), true);
        peopleTitle.setVisibility(View.GONE);
        LinearLayout.LayoutParams peopleTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        peopleTitleParams.setMargins(0, dp(22), 0, 0);
        root.addView(peopleTitle, peopleTitleParams);

        peopleContainer = new LinearLayout(this);
        peopleContainer.setOrientation(LinearLayout.VERTICAL);
        peopleContainer.setVisibility(View.GONE);
        LinearLayout.LayoutParams peopleContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        peopleContainerParams.setMargins(0, dp(12), 0, 0);
        root.addView(peopleContainer, peopleContainerParams);

        bindSearch();
        screen.addView(createBottomNavigation(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(76),
                Gravity.BOTTOM));
        setContentView(screen);
    }

    private void bindSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString();
                bindResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void bindFilters() {
        categoryContainer.removeAllViews();
        difficultyContainer.removeAllViews();

        addCategoryRow(
                new FilterItem("Chiên", CATEGORY_FRIED),
                new FilterItem("Xào", CATEGORY_STIR_FRIED),
                new FilterItem("Nướng", CATEGORY_GRILLED));
        addCategoryRow(
                new FilterItem("Món nước", CATEGORY_SOUP),
                new FilterItem("Canh", CATEGORY_CANH),
                new FilterItem("Kho", CATEGORY_BRAISED));
        addCategoryRow(
                new FilterItem("Cơm", CATEGORY_RICE),
                new FilterItem("Bún/Phở", CATEGORY_NOODLE),
                new FilterItem("Lẩu", CATEGORY_HOTPOT));
        addCategoryRow(
                new FilterItem("Gỏi/Salad", CATEGORY_SALAD),
                new FilterItem("Bánh", CATEGORY_CAKE),
                new FilterItem("Hấp", CATEGORY_STEAMED));
        addCategoryRow(
                new FilterItem("Luộc", CATEGORY_BOILED));
        addCategoryRow(
                new FilterItem("Tráng miệng", CATEGORY_DESSERT),
                new FilterItem("Nước uống", CATEGORY_DRINK));

        addFilterChip(difficultyContainer, "Dễ", DIFFICULTY_EASY, selectedDifficulty,
                value -> {
                    selectedDifficulty = value.equals(selectedDifficulty) ? "" : value;
                    bindFilters();
                    bindResults();
                });
        addFilterChip(difficultyContainer, "Trung bình", DIFFICULTY_MEDIUM, selectedDifficulty,
                value -> {
                    selectedDifficulty = value.equals(selectedDifficulty) ? "" : value;
                    bindFilters();
                    bindResults();
                });
        addFilterChip(difficultyContainer, "Khó", DIFFICULTY_HARD, selectedDifficulty,
                value -> {
                    selectedDifficulty = value.equals(selectedDifficulty) ? "" : value;
                    bindFilters();
                    bindResults();
                });
    }

    private void addCategoryRow(FilterItem... items) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, categoryContainer.getChildCount() == 0 ? 0 : dp(10), 0, 0);
        row.setLayoutParams(rowParams);
        categoryContainer.addView(row);

        for (FilterItem item : items) {
            addFilterChip(row, item.label, item.value, selectedCategory,
                    value -> {
                        selectedCategory = value.equals(selectedCategory) ? "" : value;
                        bindFilters();
                        bindResults();
                    });
        }

        for (int index = items.length; index < 3; index++) {
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1f);
            if (index < 2) {
                spacerParams.setMargins(0, 0, dp(10), 0);
            }
            row.addView(spacer, spacerParams);
        }
    }

    private void bindResults() {
        List<Recipe> recipes = filterRecipes();
        List<CommunityFriend> people = filterUsers();
        recipeAdapter.bindPopularCards(resultsContainer, recipes);
        emptyText.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);

        peopleContainer.removeAllViews();
        boolean showPeople = !people.isEmpty() && !normalize(query).isEmpty();
        peopleTitle.setVisibility(showPeople ? View.VISIBLE : View.GONE);
        peopleContainer.setVisibility(showPeople ? View.VISIBLE : View.GONE);
        if (showPeople) {
            for (CommunityFriend friend : people) {
                peopleContainer.addView(createPersonCard(friend));
            }
        }

        boolean exactMatch = hasExactRecipeMatch();
        if (exactMatch) {
            resultTitle.setText("Món trùng khớp chính xác");
            statusText.setText("Đã tìm đúng tên món. Màn hình chỉ hiển thị món bạn vừa nhập.");
        } else if (!normalize(query).isEmpty()) {
            resultTitle.setText("Gợi ý với từ khóa \"" + query.trim() + "\"");
            statusText.setText("Đang gợi ý " + recipes.size()
                    + " món có liên quan đến từ khóa và bộ lọc bạn đang chọn.");
        } else if (!selectedCategory.isEmpty()) {
            resultTitle.setText("Tất cả món " + selectedCategory);
            statusText.setText("Đang hiển thị toàn bộ món thuộc danh mục " + selectedCategory + ".");
        } else if (!selectedDifficulty.isEmpty()) {
            resultTitle.setText("Món " + selectedDifficulty);
            statusText.setText("Đang lọc món theo độ khó " + selectedDifficulty + ".");
        } else {
            resultTitle.setText("Gợi ý món ăn");
            statusText.setText("Chọn danh mục món ăn hoặc nhập từ khóa để xem gợi ý món liên quan.");
        }

        if (showPeople) {
            peopleTitle.setText("Người dùng liên quan (" + people.size() + ")");
        }
    }

    private boolean hasExactRecipeMatch() {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        for (Recipe recipe : allRecipes) {
            if (normalize(recipe.getName()).equals(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private List<Recipe> filterRecipes() {
        String normalizedQuery = normalize(query);
        List<Recipe> exactMatches = new ArrayList<>();
        if (!normalizedQuery.isEmpty()) {
            for (Recipe recipe : allRecipes) {
                if (normalize(recipe.getName()).equals(normalizedQuery)) {
                    exactMatches.add(recipe);
                }
            }
            if (!exactMatches.isEmpty()) {
                return exactMatches;
            }
        }

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (!selectedCategory.isEmpty() && !matchesCategory(recipe, selectedCategory)) {
                continue;
            }
            if (!selectedDifficulty.isEmpty()
                    && !normalize(recipe.getDifficulty()).contains(normalize(selectedDifficulty))) {
                continue;
            }
            if (!normalizedQuery.isEmpty() && !matchesRecipe(recipe, normalizedQuery)) {
                continue;
            }
            filtered.add(recipe);
        }
        sortRecipes(filtered, normalizedQuery);
        return filtered;
    }

    private boolean matchesCategory(Recipe recipe, String categoryFilter) {
        String filter = normalize(categoryFilter);
        if (filter.isEmpty()) {
            return true;
        }
        String searchable = normalize(recipe.getCategory() + " " + recipe.getName() + " "
                + recipe.getDescription());
        if (searchable.contains(filter)) {
            return true;
        }
        if (normalize(CATEGORY_SOUP).equals(filter)) {
            return containsAny(searchable, "mon nuoc", "bun", "pho", "hu tieu", "mi quang", "lau");
        }
        if (normalize(CATEGORY_CANH).equals(filter)) {
            return containsAny(searchable, "canh", "kho qua nhoi thit");
        }
        if (normalize(CATEGORY_BRAISED).equals(filter)) {
            return containsAny(searchable, "kho", "rim");
        }
        if (normalize(CATEGORY_RICE).equals(filter)) {
            return containsAny(searchable, "com");
        }
        if (normalize(CATEGORY_NOODLE).equals(filter)) {
            return containsAny(searchable, "bun", "pho", "hu tieu", "mi quang", "mi xao");
        }
        if (normalize(CATEGORY_HOTPOT).equals(filter)) {
            return containsAny(searchable, "lau");
        }
        if (normalize(CATEGORY_SALAD).equals(filter)) {
            return containsAny(searchable, "goi", "salad");
        }
        if (normalize(CATEGORY_CAKE).equals(filter)) {
            return containsAny(searchable, "banh");
        }
        if (normalize(CATEGORY_DRINK).equals(filter)) {
            return containsAny(searchable, "nuoc uong", "sinh to", "ca phe", "nuoc cam");
        }
        return false;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void sortRecipes(List<Recipe> recipes, String normalizedQuery) {
        Collections.sort(recipes, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe left, Recipe right) {
                int leftScore = scoreRecipe(left, normalizedQuery);
                int rightScore = scoreRecipe(right, normalizedQuery);
                if (leftScore != rightScore) {
                    return Integer.compare(rightScore, leftScore);
                }
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
    }

    private int scoreRecipe(Recipe recipe, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return recipe.getPopularityScore();
        }
        String normalizedName = normalize(recipe.getName());
        if (normalizedName.equals(normalizedQuery)) {
            return 1000;
        }
        if (normalizedName.startsWith(normalizedQuery)) {
            return 800;
        }
        if (normalizedName.contains(normalizedQuery)) {
            return 700;
        }
        return 500 + recipe.getPopularityScore();
    }

    private boolean matchesRecipe(Recipe recipe, String normalizedQuery) {
        StringBuilder builder = new StringBuilder();
        builder.append(recipe.getName()).append(' ')
                .append(recipe.getDescription()).append(' ')
                .append(recipe.getCategory()).append(' ')
                .append(recipe.getDifficulty()).append(' ');
        for (String ingredient : recipe.getIngredients()) {
            builder.append(ingredient).append(' ');
        }
        return normalize(builder.toString()).contains(normalizedQuery);
    }

    private List<CommunityFriend> filterUsers() {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, CommunityFriend> result = new LinkedHashMap<>();
        for (CommunityFriend friend : allUsers) {
            String searchable = normalize(friend.getName() + " " + friend.getEmail());
            if (!searchable.contains(normalizedQuery)) {
                continue;
            }
            result.put(friend.getId(), friend);
        }
        return new ArrayList<>(result.values());
    }

    private List<CommunityFriend> loadUsers() {
        CommunityState state = communityRepository.loadCommunity("", "");
        Map<String, CommunityFriend> result = new LinkedHashMap<>();
        for (CommunityFriend friend : state.getFriends()) {
            result.put(friend.getId(), friend);
        }
        for (CommunityFriend friend : state.getInvites()) {
            result.put(friend.getId(), friend);
        }
        for (CommunityFriend friend : state.getDiscoveries()) {
            result.put(friend.getId(), friend);
        }
        return new ArrayList<>(result.values());
    }

    private View createPersonCard(CommunityFriend friend) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(round(Color.WHITE, dp(24), 0));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setOnClickListener(view -> openCommunityProfile(friend));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView avatar = text(initials(friend.getName()), 17, Color.parseColor("#944A00"), true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(Color.parseColor("#FFF1EC"), dp(28), Color.parseColor("#FFDBCF")));
        card.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(14), 0, dp(10), 0);
        card.addView(textGroup, textParams);

        textGroup.addView(text(friend.getName(), 19, getColor(R.color.on_surface), true));
        textGroup.addView(text(friend.getSharedRecipeCount() + " món chung", 14,
                getColor(R.color.on_surface_variant), false));

        TextView action = text("Xem", 13, Color.parseColor("#944A00"), true);
        action.setGravity(Gravity.CENTER);
        action.setBackground(round(Color.parseColor("#FFF1EC"), dp(20), 0));
        card.addView(action, new LinearLayout.LayoutParams(dp(62), dp(42)));
        return card;
    }

    private void openCommunityProfile(CommunityFriend friend) {
        Intent intent = new Intent(this, CommunityActivity.class);
        intent.putExtra(CommunityActivity.EXTRA_FRIEND_ID, friend.getId());
        startActivity(intent);
    }

    private LinearLayout createBottomNavigation() {
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(10), 0, dp(10), 0);
        bottomBar.setBackgroundResource(R.drawable.bg_bottom_nav);
        bottomBar.setElevation(dp(10));

        bottomBar.addView(createNavItem("Trang chủ", false, view ->
                AppNavigator.openTopLevel(this, HomeActivity.class)));
        bottomBar.addView(createNavItem("Tìm kiếm", true, view ->
                statusText.setText("Bạn đang ở màn tìm kiếm món ăn.")));
        bottomBar.addView(createNavItem("AI Chef", false, view ->
                AppNavigator.openTopLevel(this, AiChefActivity.class)));
        bottomBar.addView(createNavItem("Đi chợ", false, view ->
                AppNavigator.openTopLevel(this, ShoppingListActivity.class)));
        bottomBar.addView(createNavItem("Cá nhân", false, view ->
                AppNavigator.openTopLevel(this, ProfileActivity.class)));
        return bottomBar;
    }

    private TextView createNavItem(String label, boolean active, View.OnClickListener listener) {
        TextView item = text(label, 12, active ? getColor(R.color.on_primary_container)
                : getColor(R.color.on_surface_variant), true);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(10), dp(14), dp(10), dp(14));
        item.setOnClickListener(listener);
        item.setBackgroundResource(active
                ? R.drawable.bg_home_tab_active
                : R.drawable.bg_home_tab_inactive);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(48), 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        item.setLayoutParams(params);
        return item;
    }

    private void addSectionTitle(LinearLayout root, String label, int topMargin) {
        TextView title = text(label, 18, getColor(R.color.on_surface), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, topMargin, 0, 0);
        root.addView(title, params);
    }

    private void addFilterChip(LinearLayout container, String label, String value, String selectedValue,
                               OnFilterClickListener listener) {
        boolean active = value.equals(selectedValue);
        Button chip = new Button(this);
        chip.setText(label);
        chip.setAllCaps(false);
        chip.setTextSize(15);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(active ? Color.parseColor("#944A00") : Color.parseColor("#564337"));
        chip.setBackground(round(active ? Color.parseColor("#FFE2D9") : Color.WHITE,
                dp(20), Color.parseColor(active ? "#FFB89D" : "#E9D6CB")));
        chip.setOnClickListener(view -> listener.onClick(value));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        params.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(params);
        container.addView(chip);
    }

    private void openRecipe(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    private String initials(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            return "BN";
        }
        String[] parts = clean.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.US);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.US);
        return normalized.trim().replaceAll("\\s+", " ");
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

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean canBindUi() {
        return !isFinishing() && !isDestroyed();
    }

    private interface OnFilterClickListener {
        void onClick(String value);
    }

    private static final class SearchData {
        private final List<Recipe> recipes;
        private final List<CommunityFriend> users;

        private SearchData(List<Recipe> recipes, List<CommunityFriend> users) {
            this.recipes = recipes;
            this.users = users;
        }
    }

    private static final class FilterItem {
        private final String label;
        private final String value;

        private FilterItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
