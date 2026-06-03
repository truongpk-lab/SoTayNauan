package com.sotaynauan.ai.adapter.community;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityShare;
import com.sotaynauan.ai.data.model.CommunityComment;

import java.util.List;

public class CommunityAdapter {
    public interface Listener {
        void onFriendProfile(CommunityFriend friend);
        void onFriendShare(CommunityFriend friend);
        void onAcceptInvite(CommunityFriend friend);
        void onInviteDiscovery(CommunityFriend friend);
        void onShareDetail(CommunityShare share);
        void onLikeShare(CommunityShare share);
        void onCommentShare(CommunityShare share);
        void onSaveShare(CommunityShare share);
    }

    private final Context context;
    private final Listener listener;

    public CommunityAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bindFriends(LinearLayout container, List<CommunityFriend> friends) {
        container.removeAllViews();
        for (CommunityFriend friend : friends) {
            container.addView(createFriendCard(friend, "Chia sẻ", view -> listener.onFriendShare(friend)));
        }
    }

    public void bindInvites(LinearLayout container, List<CommunityFriend> invites) {
        container.removeAllViews();
        appendInviteSection(container, "Đã nhận", invites, "Nhận lời", true);
    }

    public void appendInviteSection(LinearLayout container, String title, List<CommunityFriend> invites,
                                    String action, boolean canAccept) {
        TextView titleView = title(title, 17);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(12), 0, dp(8));
        titleView.setLayoutParams(titleParams);
        container.addView(titleView);

        if (invites.isEmpty()) {
            TextView empty = body(canAccept
                            ? "Chưa có lời mời mới."
                            : "Chưa gửi lời mời nào.",
                    14,
                    Color.parseColor("#7A6558"));
            empty.setPadding(dp(4), 0, 0, dp(8));
            container.addView(empty);
            return;
        }

        for (CommunityFriend friend : invites) {
            container.addView(createFriendCard(friend, action, view -> {
                if (canAccept) {
                    listener.onAcceptInvite(friend);
                } else {
                    listener.onFriendProfile(friend);
                }
            }));
        }
    }

    public void bindDiscoveries(LinearLayout container, List<CommunityFriend> discoveries) {
        container.removeAllViews();
        for (CommunityFriend friend : discoveries) {
            container.addView(createFriendCard(friend, "Mời", view -> listener.onInviteDiscovery(friend)));
        }
    }

    public void bindShares(LinearLayout container, List<CommunityShare> shares) {
        container.removeAllViews();
        for (CommunityShare share : shares) {
            container.addView(createShareCard(share));
        }
    }

    private View createFriendCard(CommunityFriend friend, String action, View.OnClickListener actionClick) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(round(Color.WHITE, dp(28), 0));
        card.setElevation(dp(3));
        card.setClickable(true);
        card.setOnClickListener(view -> listener.onFriendProfile(friend));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView avatar = avatar(friend.getName());
        card.addView(avatar);

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(14), 0, dp(10), 0);
        card.addView(textGroup, textParams);

        textGroup.addView(title(friend.getName(), 20));
        textGroup.addView(body("🍴 " + friend.getSharedRecipeCount() + " món chung", 15,
                Color.parseColor("#564337")));

        TextView button = circleButton(action);
        button.setOnClickListener(actionClick);
        card.addView(button);
        return card;
    }

    private View createShareCard(CommunityShare share) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(round(Color.WHITE, dp(22), 0));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setOnClickListener(view -> listener.onShareDetail(share));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);
        row.addView(avatar(share.getFriendName()));

        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(12), 0, 0, 0);
        row.addView(textGroup, textParams);
        textGroup.addView(title(share.getFriendName(), 17));
        textGroup.addView(body(share.getRecipeName(), 15, Color.parseColor("#944A00")));

        TextView badge = new TextView(context);
        badge.setText(share.isFromMe() ? "Bạn gửi" : "Mới");
        badge.setGravity(Gravity.CENTER);
        badge.setTextSize(12);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.parseColor("#502600"));
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(round(Color.parseColor("#FFE2D9"), dp(16), 0));
        row.addView(badge);

        TextView message = body(share.getMessage(), 15, Color.parseColor("#2E150B"));
        message.setPadding(0, dp(12), 0, dp(10));
        card.addView(message);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(actions);
        actions.addView(actionChip((share.isLiked() ? "♥ " : "♡ ") + share.getLikeCount(),
                share.isLiked(), view -> listener.onLikeShare(share)));
        actions.addView(actionChip("Bình luận " + share.getCommentCount(),
                false, view -> listener.onCommentShare(share)));
        actions.addView(actionChip(share.isSaved() ? "Đã lưu" : "Lưu món",
                share.isSaved(), view -> listener.onSaveShare(share)));
        addComments(card, share.getComments());
        return card;
    }

    private void addComments(LinearLayout card, List<CommunityComment> comments) {
        if (comments.isEmpty()) {
            return;
        }

        LinearLayout commentBox = new LinearLayout(context);
        commentBox.setOrientation(LinearLayout.VERTICAL);
        commentBox.setPadding(dp(12), dp(10), dp(12), dp(8));
        commentBox.setBackground(round(Color.parseColor("#FFF8F6"), dp(14), Color.parseColor("#E7CFC3")));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, 0);
        card.addView(commentBox, params);

        int start = Math.max(0, comments.size() - 2);
        for (int index = start; index < comments.size(); index++) {
            CommunityComment comment = comments.get(index);
            TextView text = body(comment.getAuthorName() + ": " + comment.getBody(),
                    13,
                    Color.parseColor("#2E150B"));
            if (index > start) {
                text.setPadding(0, dp(6), 0, 0);
            }
            commentBox.addView(text);
        }
    }

    private TextView avatar(String name) {
        TextView avatar = new TextView(context);
        avatar.setText(initials(name));
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextSize(17);
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setTextColor(Color.parseColor("#944A00"));
        avatar.setBackground(round(Color.parseColor("#FFF1EC"), dp(28), Color.parseColor("#FFDBCF")));
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        return avatar;
    }

    private String initials(String name) {
        String clean = name.replace("Bạn →", "").trim();
        String[] parts = clean.split("\\s+");
        if (parts.length == 0 || clean.isEmpty()) {
            return "BN";
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    private TextView title(String value, int sizeSp) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(Color.parseColor("#2E150B"));
        text.setTextSize(sizeSp);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        return text;
    }

    private TextView body(String value, int sizeSp, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(sizeSp);
        text.setLineSpacing(0f, 1.08f);
        return text;
    }

    private TextView circleButton(String value) {
        TextView button = new TextView(context);
        button.setText(value);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Color.parseColor("#944A00"));
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(round(Color.parseColor("#FFF1EC"), dp(24), 0));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(48)));
        return button;
    }

    private TextView actionChip(String label, boolean active, View.OnClickListener clickListener) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(active ? Color.parseColor("#502600") : Color.parseColor("#564337"));
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(round(active ? Color.parseColor("#FFDBCF") : Color.parseColor("#FFF8F6"),
                dp(18), Color.parseColor("#DCC1B1")));
        chip.setOnClickListener(clickListener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
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
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
