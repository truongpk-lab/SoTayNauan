package com.sotaynauan.ai.data.seed;

import android.graphics.Color;

import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CommunityFriend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SeedDataProvider {
    private final RecipeMapper mapper = new RecipeMapper();

    public List<RecipeEntity> createRecipes() {
        List<RecipeEntity> recipes = new ArrayList<>();
        recipes.add(recipe("Phở bò", "Nước dùng thơm, thịt bò mềm và rau thơm cho bữa sáng ấm bụng.",
                70, "Vừa phải", "Món nước", "#C56A2C", 98, false, "", "",
                Arrays.asList("Xương bò 800 g", "Bánh phở 500 g", "Thịt bò 300 g",
                        "Hành tây 1 củ", "Gừng 1 củ", "Quế hồi 1 gói"),
                Arrays.asList("Nướng gừng và hành cho thơm.", "Hầm xương với gia vị.", "Chần bánh phở, xếp thịt bò và chan nước dùng.")));
        recipes.add(recipe("Cơm chiên trứng", "Món nhanh từ cơm nguội, trứng và rau củ còn sẵn trong bếp.",
                18, "Dễ làm", "Món nhanh", "#F0A51A", 95, true, "Minh Tuấn",
                "Món cứu đói buổi trưa, thêm hành lá là thơm hẳn.",
                Arrays.asList("Cơm nguội 2 bát", "Trứng gà 2 quả", "Cà rốt 1 củ",
                        "Hành lá 2 nhánh", "Nước tương 1 chai nhỏ"),
                Arrays.asList("Đánh trứng và xào tơi với cơm.", "Thêm rau củ thái nhỏ.", "Nêm nước tương và đảo lửa lớn.")));
        recipes.add(recipe("Trứng xào cà chua", "Mềm, chua ngọt nhẹ, hợp khi cần một đĩa cơm nhà đơn giản.",
                15, "Dễ làm", "Món nhà", "#D84F38", 91, true, "", "",
                Arrays.asList("Trứng gà 2 quả", "Cà chua 3 quả", "Hành lá 2 nhánh",
                        "Nước mắm 1 chai nhỏ", "Tiêu 1 gói"),
                Arrays.asList("Xào cà chua đến khi ra sốt.", "Cho trứng đã đánh vào đảo nhẹ.", "Nêm vừa ăn và rắc hành lá.")));
        recipes.add(recipe("Bò xào cà chua & trứng", "Đậm đà, mềm thơm, tận dụng thịt bò, trứng và cà chua trong bếp.",
                22, "Dễ làm", "Món xào", "#E67E22", 97, true, "", "",
                Arrays.asList("Thịt bò 300 g", "Cà chua 3 quả", "Trứng gà 2 quả",
                        "Hành lá 2 nhánh", "Nước mắm 1 chai nhỏ", "Tiêu 1 gói", "Tỏi 1 củ"),
                Arrays.asList("Ướp thịt bò với nước mắm, tiêu và tỏi băm.",
                        "Xào bò nhanh tay trong 2 phút rồi để riêng.",
                        "Xào cà chua, cho trứng vào đảo mềm và trộn bò lại.")));
        recipes.add(recipe("Gà kho gừng", "Thịt gà đậm vị, thơm gừng, rất hợp bữa cơm tối gia đình.",
                35, "Vừa phải", "Món mặn", "#9E5A2E", 88, false, "", "",
                Arrays.asList("Thịt gà 500 g", "Gừng 1 củ", "Nước mắm 1 chai nhỏ",
                        "Đường 1 gói", "Hành tím 1 củ"),
                Arrays.asList("Ướp gà với gừng và gia vị.", "Thắng đường nhẹ tạo màu.", "Kho nhỏ lửa đến khi thịt thấm.")));
        recipes.add(recipe("Salad trộn", "Tươi mát, nhiều rau, dùng kèm sốt chua ngọt nhẹ.",
                12, "Dễ làm", "Món lành mạnh", "#4C9A63", 86, true, "Lan Anh",
                "Mình thêm bơ và mè rang, ăn nhẹ mà vẫn đủ no.",
                Arrays.asList("Xà lách 1 mớ", "Cà chua bi 200 g", "Dưa leo 2 quả",
                        "Bơ 1 quả", "Dầu ô liu 1 chai nhỏ"),
                Arrays.asList("Rửa sạch và để ráo rau.", "Cắt rau củ vừa ăn.", "Trộn với sốt ngay trước khi dùng.")));
        return recipes;
    }

    public List<CommunityFriendEntity> createCommunityFriends() {
        long now = System.currentTimeMillis();
        List<CommunityFriendEntity> friends = new ArrayList<>();
        friends.add(friend("minh-tuan", "Minh Tuấn", "tuan.bepnha@example.com",
                "Hay chia sẻ món nhanh cho bữa trưa.", CommunityFriend.STATUS_FRIEND, 12, now - 86400000L * 12));
        friends.add(friend("lan-phuong", "Lan Phương", "phuong.bepnha@example.com",
                "Thích món rau, salad và bữa tối nhẹ.", CommunityFriend.STATUS_FRIEND, 8, now - 86400000L * 8));
        friends.add(friend("chu-hai", "Chú Hải", "hai.giadinh@example.com",
                "Giữ nhiều mẹo món kho truyền thống.", CommunityFriend.STATUS_FRIEND, 24, now - 86400000L * 30));
        friends.add(friend("me-an", "Mẹ An", "me.an@example.com",
                "Đang chờ bạn xác nhận lời mời.", CommunityFriend.STATUS_INVITED, 18, now - 86400000L * 2));
        friends.add(friend("bep-nha-linh", "Bếp nhà Linh", "linh.kitchen@example.com",
                "Gợi ý kết bạn từ công thức món lành mạnh.", CommunityFriend.STATUS_DISCOVER, 15, now - 86400000L));
        return friends;
    }

    public List<CommunityShareEntity> createCommunityShares() {
        long now = System.currentTimeMillis();
        List<CommunityShareEntity> shares = new ArrayList<>();
        shares.add(share("share-com-chien", "minh-tuan", "Minh Tuấn", 2L,
                "Cơm chiên trứng", "Món cứu đói buổi trưa, thêm hành lá là thơm hẳn.",
                6, 2, false, false, false, now - 3600000L));
        shares.add(share("share-salad", "lan-phuong", "Lan Phương", 6L,
                "Salad trộn", "Mình thêm bơ và mè rang, ăn nhẹ mà vẫn đủ no.",
                9, 4, false, false, false, now - 7200000L));
        shares.add(share("share-ga-kho", "chu-hai", "Chú Hải", 5L,
                "Gà kho gừng", "Kho nhỏ lửa lâu hơn một chút thì thịt thấm và thơm gừng.",
                14, 5, false, true, false, now - 10800000L));
        return shares;
    }

    private RecipeEntity recipe(String name, String description, int totalMinutes, String difficulty,
                                String category, String colorHex, int popularityScore,
                                boolean todaySuggestion, String friendName, String friendNote,
                                List<String> ingredients, List<String> steps) {
        return mapper.toEntity(name, description, totalMinutes, difficulty, category,
                Color.parseColor(colorHex), popularityScore, todaySuggestion, friendName,
                friendNote, ingredients, steps);
    }

    private CommunityFriendEntity friend(String id, String name, String email, String note,
                                         String status, int sharedRecipeCount, long joinedAtMillis) {
        CommunityFriendEntity entity = new CommunityFriendEntity();
        entity.id = id;
        entity.name = name;
        entity.email = email;
        entity.note = note;
        entity.status = status;
        entity.sharedRecipeCount = sharedRecipeCount;
        entity.joinedAtMillis = joinedAtMillis;
        return entity;
    }

    private CommunityShareEntity share(String id, String friendId, String friendName, long recipeId,
                                       String recipeName, String message, int likeCount,
                                       int commentCount, boolean liked, boolean saved,
                                       boolean fromMe, long createdAtMillis) {
        CommunityShareEntity entity = new CommunityShareEntity();
        entity.id = id;
        entity.friendId = friendId;
        entity.friendName = friendName;
        entity.recipeId = recipeId;
        entity.recipeName = recipeName;
        entity.message = message;
        entity.likeCount = likeCount;
        entity.commentCount = commentCount;
        entity.liked = liked;
        entity.saved = saved;
        entity.fromMe = fromMe;
        entity.createdAtMillis = createdAtMillis;
        return entity;
    }
}
