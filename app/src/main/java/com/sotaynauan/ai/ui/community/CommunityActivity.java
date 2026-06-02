package com.sotaynauan.ai.ui.community;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.community.CommunityAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityShare;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.CommunityRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.ui.search.SearchActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityActivity extends Activity {
    public static final String EXTRA_FRIEND_ID = "extra_friend_id";

    private static final String TAB_FRIENDS = "friends";
    private static final String TAB_INVITES = "invites";
    private static final String TAB_DISCOVER = "discover";

    private CommunityViewModel viewModel;
    private CommunityRepository communityRepository;
    private RecipeRepository recipeRepository;
    private CommunityAdapter adapter;
    private CommunityState currentState;
    private String activeTab = TAB_FRIENDS;
    private String pendingFriendId;

    private TextView statusText;
    private TextView emptyText;
    private LinearLayout friendListContainer;
    private LinearLayout shareFeedContainer;
    private TextView friendsTab;
    private TextView invitesTab;
    private TextView discoverTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        pendingFriendId = getIntent().getStringExtra(EXTRA_FRIEND_ID);
        recipeRepository = createRecipeRepository();
        communityRepository = new CommunityRepository(
                new CommunityLocalDataSource(
                        AppDatabase.getInstance(this).communityDao(),
                        new SeedDataProvider()),
                new CommunityMapper());
        viewModel = new CommunityViewModel(communityRepository);
        adapter = new CommunityAdapter(this, new CommunityAdapter.Listener() {
            @Override
            public void onFriendProfile(CommunityFriend friend) {
                showFriendProfile(friend);
            }

            @Override
            public void onFriendShare(CommunityFriend friend) {
                showShareProfile(friend);
            }

            @Override
            public void onAcceptInvite(CommunityFriend friend) {
                activeTab = TAB_FRIENDS;
                bindState(viewModel.acceptInvite(friend.getId()));
            }

            @Override
            public void onInviteDiscovery(CommunityFriend friend) {
                activeTab = TAB_INVITES;
                bindState(viewModel.inviteDiscovery(friend.getId()));
            }

            @Override
            public void onShareDetail(CommunityShare share) {
                showSharedRecipeHistory(share);
            }

            @Override
            public void onLikeShare(CommunityShare share) {
                bindState(viewModel.like(share.getId()));
            }

            @Override
            public void onCommentShare(CommunityShare share) {
                showCommentDialog(share);
            }

            @Override
            public void onSaveShare(CommunityShare share) {
                bindState(viewModel.save(share.getId()));
            }
        });

        bindViews();
        bindActions();
        bindState(viewModel.load());
    }

    private RecipeRepository createRecipeRepository() {
        return new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
    }

    private void bindViews() {
        statusText = findViewById(R.id.communityStatusText);
        emptyText = findViewById(R.id.communityEmptyText);
        friendListContainer = findViewById(R.id.friendListContainer);
        shareFeedContainer = findViewById(R.id.shareFeedContainer);
        friendsTab = findViewById(R.id.friendsTab);
        invitesTab = findViewById(R.id.invitesTab);
        discoverTab = findViewById(R.id.discoverTab);
    }

    private void bindActions() {
        findViewById(R.id.communityMenuButton).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.inviteMemberButton).setOnClickListener(view -> {
            activeTab = TAB_DISCOVER;
            bindState(viewModel.load());
            statusText.setText("Chọn một người trong tab Khám phá để gửi lời mời local.");
        });
        friendsTab.setOnClickListener(view -> {
            activeTab = TAB_FRIENDS;
            bindState(viewModel.load());
        });
        invitesTab.setOnClickListener(view -> {
            activeTab = TAB_INVITES;
            bindState(viewModel.load());
        });
        discoverTab.setOnClickListener(view -> {
            activeTab = TAB_DISCOVER;
            bindState(viewModel.load());
        });

        EditText searchInput = findViewById(R.id.communitySearchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeTab = TAB_FRIENDS;
                bindState(viewModel.search(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.homeTab).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.searchTab).setOnClickListener(view ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void bindState(CommunityState state) {
        currentState = state;
        statusText.setText(state.getStatusMessage());
        bindTabs(state);
        if (TAB_INVITES.equals(activeTab)) {
            adapter.bindInvites(friendListContainer, state.getInvites());
            emptyText.setVisibility(state.getInvites().isEmpty() ? View.VISIBLE : View.GONE);
        } else if (TAB_DISCOVER.equals(activeTab)) {
            adapter.bindDiscoveries(friendListContainer, state.getDiscoveries());
            emptyText.setVisibility(state.getDiscoveries().isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            adapter.bindFriends(friendListContainer, state.getFriends());
            emptyText.setVisibility(state.getFriends().isEmpty() ? View.VISIBLE : View.GONE);
        }
        adapter.bindShares(shareFeedContainer, state.getShares());
        openPendingFriendProfile();
    }

    private void bindTabs(CommunityState state) {
        bindTab(friendsTab, TAB_FRIENDS, "Bạn bè");
        bindTab(invitesTab, TAB_INVITES, "Lời mời " + state.getInvites().size());
        bindTab(discoverTab, TAB_DISCOVER, "Khám phá");
    }

    private void bindTab(TextView tab, String key, String label) {
        boolean active = key.equals(activeTab);
        tab.setText(label);
        tab.setTextColor(active ? Color.parseColor("#944A00") : Color.parseColor("#564337"));
        tab.setBackgroundResource(active ? R.drawable.bg_community_tab_active : 0);
    }

    private void showFriendProfile(CommunityFriend friend) {
        new AlertDialog.Builder(this)
                .setTitle(friend.getName())
                .setView(createFriendProfileView(friend))
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Chia sẻ món", (dialog, which) -> showShareProfile(friend))
                .show();
    }

    private View createFriendProfileView(CommunityFriend friend) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(8));
        scrollView.addView(content);

        TextView emailView = createDialogText(friend.getEmail(), 15, "#564337");
        content.addView(emailView);

        TextView noteView = createDialogText(friend.getNote(), 15, "#2F170F");
        noteView.setPadding(0, dp(8), 0, 0);
        content.addView(noteView);

        addRecipeSection(content, "Món đang chung với bạn", buildCommonRecipes(friend),
                "Chưa có món chung cụ thể với người này.");
        addRecipeSection(content, "Tất cả công thức người này đang có", buildFriendRecipes(friend),
                "Hiện chưa có công thức nào của người này trong kho local.");
        addRecipeSection(content, "Món bạn đã chia sẻ", buildSentRecipes(friend),
                "Bạn chưa chia sẻ món nào cho người này.");
        return scrollView;
    }

    private void addRecipeSection(LinearLayout parent, String title, List<Recipe> recipes, String emptyMessage) {
        TextView titleView = createDialogText(title, 17, "#2F170F");
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        titleView.setPadding(0, dp(18), 0, dp(8));
        parent.addView(titleView);

        if (recipes.isEmpty()) {
            TextView emptyView = createDialogText(emptyMessage, 14, "#7A6558");
            parent.addView(emptyView);
            return;
        }

        for (Recipe recipe : recipes) {
            parent.addView(createRecipeCard(recipe));
        }
    }

    private View createRecipeCard(Recipe recipe) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_voice_settings_card);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setClickable(true);
        card.setOnClickListener(view -> openRecipeDetail(recipe));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        imageParams.setMargins(0, 0, dp(12), 0);
        card.addView(imageView, imageParams);
        RecipeImageResolver.apply(imageView, recipe);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        card.addView(textColumn, textParams);

        TextView nameView = createDialogText(recipe.getName(), 16, "#2F170F");
        nameView.setTypeface(nameView.getTypeface(), android.graphics.Typeface.BOLD);
        textColumn.addView(nameView);

        TextView metaView = createDialogText(
                recipe.getCategory() + " • " + recipe.getDifficulty() + " • " + recipe.getTotalMinutes() + " phút",
                13,
                "#944A00");
        metaView.setPadding(0, dp(4), 0, 0);
        textColumn.addView(metaView);

        TextView descriptionView = createDialogText(recipe.getDescription(), 13, "#564337");
        descriptionView.setPadding(0, dp(6), 0, 0);
        textColumn.addView(descriptionView);

        TextView actionView = createDialogText("Mở", 12, "#944A00");
        actionView.setTypeface(actionView.getTypeface(), android.graphics.Typeface.BOLD);
        actionView.setGravity(Gravity.CENTER);
        actionView.setBackgroundColor(Color.TRANSPARENT);
        card.addView(actionView);
        return card;
    }

    private void showSharedRecipeHistory(CommunityShare share) {
        CommunityFriend friend = findFriendById(share.getFriendId());
        String title = friend == null ? share.getFriendName() : friend.getName();
        List<Recipe> sharedRecipes = friend == null
                ? buildSentRecipes(share.getFriendId(), share.getFriendName())
                : buildSentRecipes(friend);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(createRecipeHistoryView(sharedRecipes,
                        "Bạn chưa chia sẻ món nào cho người này.",
                        share.getMessage()))
                .setNegativeButton("Đóng", null);
        if (friend != null) {
            builder.setPositiveButton("Chia sẻ thêm", (dialog, which) -> showShareProfile(friend));
        }
        builder.show();
    }

    private View createRecipeHistoryView(List<Recipe> recipes, String emptyMessage, String introMessage) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(8));
        scrollView.addView(content);

        if (introMessage != null && !introMessage.trim().isEmpty()) {
            TextView introView = createDialogText(introMessage, 15, "#2F170F");
            introView.setPadding(0, 0, 0, dp(12));
            content.addView(introView);
        }

        addRecipeSection(content, "Chi tiết món đã chia sẻ", recipes, emptyMessage);
        return scrollView;
    }

    private List<Recipe> buildCommonRecipes(CommunityFriend friend) {
        LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
        addRecipesFromShares(recipes, friend.getId(), false, null);
        if (recipes.isEmpty()) {
            addRecipesFromFriendProfile(recipes, friend);
        }
        return new ArrayList<>(recipes.values());
    }

    private List<Recipe> buildFriendRecipes(CommunityFriend friend) {
        LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
        addRecipesFromFriendProfile(recipes, friend);
        addRecipesFromShares(recipes, friend.getId(), false, null);
        return new ArrayList<>(recipes.values());
    }

    private List<Recipe> buildSentRecipes(CommunityFriend friend) {
        return buildSentRecipes(friend.getId(), friend.getName());
    }

    private List<Recipe> buildSentRecipes(String friendId, String friendName) {
        LinkedHashMap<String, Recipe> recipes = new LinkedHashMap<>();
        addRecipesFromShares(recipes, friendId, true, friendName);
        return new ArrayList<>(recipes.values());
    }

    private void addRecipesFromFriendProfile(Map<String, Recipe> recipes, CommunityFriend friend) {
        String normalizedFriendName = normalize(friend.getName());
        for (Recipe recipe : recipeRepository.getAllRecipes()) {
            if (!normalize(recipe.getFriendName()).equals(normalizedFriendName)) {
                continue;
            }
            recipes.putIfAbsent(normalize(recipe.getName()), recipe);
        }
    }

    private void addRecipesFromShares(Map<String, Recipe> recipes, String friendId, boolean fromMeOnly,
                                      String fallbackFriendName) {
        for (CommunityShare share : loadAllCommunityState().getShares()) {
            if (share == null || !isShareForFriend(share, friendId, fallbackFriendName)) {
                continue;
            }
            if (fromMeOnly && !share.isFromMe()) {
                continue;
            }
            Recipe recipe = resolveRecipe(share.getRecipeId(), share.getRecipeName());
            if (recipe == null) {
                continue;
            }
            recipes.putIfAbsent(normalize(recipe.getName()), recipe);
        }
    }

    private boolean isShareForFriend(CommunityShare share, String friendId, String fallbackFriendName) {
        if (share.getFriendId() != null && share.getFriendId().equals(friendId)) {
            return true;
        }
        return fallbackFriendName != null
                && !fallbackFriendName.trim().isEmpty()
                && normalize(share.getFriendName()).contains(normalize(fallbackFriendName));
    }

    private Recipe resolveRecipe(long recipeId, String recipeName) {
        if (recipeId > 0L) {
            Recipe recipe = recipeRepository.findRecipe(recipeId);
            if (recipe != null) {
                return recipe;
            }
        }
        for (Recipe recipe : recipeRepository.getAllRecipes()) {
            if (normalize(recipe.getName()).equals(normalize(recipeName))) {
                return recipe;
            }
        }
        return null;
    }

    private CommunityFriend findFriendById(String friendId) {
        if (friendId == null || friendId.trim().isEmpty()) {
            return null;
        }
        CommunityState state = loadAllCommunityState();
        for (CommunityFriend friend : state.getFriends()) {
            if (friendId.equals(friend.getId())) {
                return friend;
            }
        }
        for (CommunityFriend friend : state.getInvites()) {
            if (friendId.equals(friend.getId())) {
                return friend;
            }
        }
        for (CommunityFriend friend : state.getDiscoveries()) {
            if (friendId.equals(friend.getId())) {
                return friend;
            }
        }
        return null;
    }

    private CommunityState loadAllCommunityState() {
        String currentStatus = currentState == null ? "" : currentState.getStatusMessage();
        return communityRepository.loadCommunity("", currentStatus);
    }

    private void openPendingFriendProfile() {
        if (pendingFriendId == null || pendingFriendId.trim().isEmpty()) {
            return;
        }
        CommunityFriend friend = findFriendById(pendingFriendId);
        pendingFriendId = null;
        if (friend != null) {
            showFriendProfile(friend);
        }
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

    private void showShareProfile(CommunityFriend friend) {
        List<Recipe> recipes = recipeRepository.getAllRecipes();
        if (recipes.isEmpty()) {
            statusText.setText("Chưa có công thức nào để chia sẻ.");
            return;
        }

        boolean[] selectedRecipes = new boolean[recipes.size()];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Chọn công thức chia sẻ")
                .setView(createRecipeSelectionView(recipes, selectedRecipes))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tiếp tục", null)
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    List<Recipe> selected = getSelectedRecipes(recipes, selectedRecipes);
                    if (selected.isEmpty()) {
                        statusText.setText("Bạn hãy chọn ít nhất 1 món để chia sẻ.");
                        return;
                    }
                    dialog.dismiss();
                    showShareConfirmation(friend, selected);
                }));
        dialog.show();
    }

    private View createRecipeSelectionView(List<Recipe> recipes, boolean[] selectedRecipes) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(8), dp(18), dp(4));

        TextView intro = createDialogText(
                "Chọn 1 hoặc nhiều món trong tất cả công thức nấu để gửi.",
                15,
                "#564337");
        intro.setPadding(0, 0, 0, dp(10));
        content.addView(intro);

        for (int index = 0; index < recipes.size(); index++) {
            content.addView(createRecipeSelectionRow(recipes.get(index), index, selectedRecipes));
        }
        scrollView.addView(content);
        return scrollView;
    }

    private View createRecipeSelectionRow(Recipe recipe, int index, boolean[] selectedRecipes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        imageParams.setMargins(0, 0, dp(12), 0);
        row.addView(imageView, imageParams);
        RecipeImageResolver.apply(imageView, recipe);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);

        TextView recipeName = createDialogText(recipe.getName(), 16, "#2F170F");
        recipeName.setTypeface(recipeName.getTypeface(), android.graphics.Typeface.BOLD);
        textColumn.addView(recipeName);

        TextView recipeInfo = createDialogText(
                recipe.getTotalMinutes() + " phút · " + recipe.getDifficulty(),
                13,
                "#564337");
        recipeInfo.setPadding(0, dp(4), 0, 0);
        textColumn.addView(recipeInfo);
        row.addView(textColumn, textParams);

        CheckBox checkBox = new CheckBox(this);
        row.addView(checkBox);

        row.setOnClickListener(view -> {
            selectedRecipes[index] = !selectedRecipes[index];
            checkBox.setChecked(selectedRecipes[index]);
        });
        checkBox.setOnClickListener(view -> selectedRecipes[index] = checkBox.isChecked());
        return row;
    }

    private List<Recipe> getSelectedRecipes(List<Recipe> recipes, boolean[] selectedRecipes) {
        List<Recipe> selected = new ArrayList<>();
        for (int index = 0; index < recipes.size(); index++) {
            if (selectedRecipes[index]) {
                selected.add(recipes.get(index));
            }
        }
        return selected;
    }

    private void showShareConfirmation(CommunityFriend friend, List<Recipe> selectedRecipes) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận chia sẻ")
                .setMessage("Bạn có muốn chia sẻ " + selectedRecipes.size()
                        + " món (" + buildSelectedRecipeSummary(selectedRecipes) + ") cho "
                        + friend.getName() + " nhận?")
                .setNegativeButton("Quay lại", (dialog, which) -> showShareProfile(friend))
                .setPositiveButton("Chia sẻ", (dialog, which) ->
                        bindState(viewModel.shareRecipes(friend.getId(), selectedRecipes)))
                .show();
    }

    private String buildSelectedRecipeSummary(List<Recipe> selectedRecipes) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < selectedRecipes.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(selectedRecipes.get(index).getName());
        }
        return builder.toString();
    }

    private TextView createDialogText(String text, int textSizeSp, String colorHex) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSizeSp);
        textView.setTextColor(Color.parseColor(colorHex));
        textView.setLineSpacing(dp(2), 1.0f);
        return textView;
    }

    private void showCommentDialog(CommunityShare share) {
        EditText input = new EditText(this);
        input.setMinLines(3);
        input.setHint("Nhập bình luận của bạn...");
        input.setTextColor(getColor(R.color.on_surface));
        input.setHintTextColor(getColor(R.color.on_surface_variant));
        new AlertDialog.Builder(this)
                .setTitle("Bình luận " + share.getRecipeName())
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String comment = input.getText().toString().trim();
                    if (comment.isEmpty()) {
                        statusText.setText("Bạn chưa nhập nội dung bình luận.");
                        return;
                    }
                    bindState(viewModel.comment(share.getId(), comment));
                })
                .show();
    }

    private void openRecipeDetail(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
