package com.sotaynauan.ai.data.model;

public class CommunityFriend {
    public static final String STATUS_FRIEND = "friend";
    public static final String STATUS_INVITE_SENT = "invite_sent";
    public static final String STATUS_INVITE_RECEIVED = "invite_received";
    public static final String STATUS_INVITED = STATUS_INVITE_RECEIVED;
    public static final String STATUS_DISCOVER = "discover";

    private final String id;
    private final String name;
    private final String email;
    private final String note;
    private final String status;
    private final int sharedRecipeCount;

    public CommunityFriend(String id, String name, String email, String note,
                           String status, int sharedRecipeCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.note = note;
        this.status = status;
        this.sharedRecipeCount = sharedRecipeCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public int getSharedRecipeCount() { return sharedRecipeCount; }
}
