package com.sotaynauan.ai.ui.community;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.List;

public class CommunityActivity extends Activity {
    private static final String TAB_FRIENDS = "friends";
    private static final String TAB_INVITES = "invites";
    private static final String TAB_DISCOVER = "discover";

    private CommunityViewModel viewModel;
    private RecipeRepository recipeRepository;
    private CommunityAdapter adapter;
    private String activeTab = TAB_FRIENDS;

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

        recipeRepository = createRecipeRepository();
        viewModel = new CommunityViewModel(new CommunityRepository(
                new CommunityLocalDataSource(
                        AppDatabase.getInstance(this).communityDao(),
                        new SeedDataProvider()),
                new CommunityMapper()));
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
            statusText.setText("Chọn một bếp nhà trong Khám phá để gửi lời mời local.");
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
                statusText.setText("Ô tìm kiếm phía trên đang lọc bạn bè và bài chia sẻ local."));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void bindState(CommunityState state) {
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
                .setMessage(friend.getEmail() + "\n\n" + friend.getNote()
                        + "\n\n" + friend.getSharedRecipeCount() + " món ăn chung trong cộng đồng local.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Chia sẻ món", (dialog, which) ->
                        showShareProfile(friend))
                .show();
    }

    private void showShareProfile(CommunityFriend friend) {
        Recipe recipe = pickShareRecipe();
        if (recipe == null) {
            statusText.setText("Chưa có công thức nào để chia sẻ.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Chia sẻ với " + friend.getName())
                .setView(createShareRecipeView(friend, recipe))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Chia sẻ món", (dialog, which) ->
                        bindState(viewModel.shareRecipe(friend.getId(), recipe)))
                .show();
    }

    private Recipe pickShareRecipe() {
        Recipe recipe = recipeRepository.getRandomQuickSuggestion();
        if (recipe != null) {
            return recipe;
        }
        List<Recipe> recipes = recipeRepository.getAllRecipes();
        return recipes.isEmpty() ? null : recipes.get(0);
    }

    private View createShareRecipeView(CommunityFriend friend, Recipe recipe) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(180));
        imageParams.setMargins(0, 0, 0, dp(14));
        content.addView(imageView, imageParams);
        RecipeImageResolver.apply(imageView, recipe);

        TextView recipeName = createDialogText(recipe.getName(), 20, "#2F170F");
        recipeName.setTypeface(recipeName.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(recipeName);

        TextView recipeInfo = createDialogText(
                recipe.getDescription() + "\n" + recipe.getTotalMinutes() + " phút · " + recipe.getDifficulty(),
                15,
                "#564337");
        recipeInfo.setPadding(0, dp(6), 0, dp(12));
        content.addView(recipeInfo);

        TextView confirmText = createDialogText(
                "Bạn có muốn chia sẻ món " + recipe.getName() + " cho " + friend.getName() + " nhận?",
                16,
                "#944A00");
        confirmText.setTypeface(confirmText.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(confirmText);

        TextView commonText = createDialogText(
                "Hai bếp có " + friend.getSharedRecipeCount() + " món ăn chung.",
                14,
                "#6D5142");
        commonText.setPadding(0, dp(8), 0, 0);
        content.addView(commonText);
        return content;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
