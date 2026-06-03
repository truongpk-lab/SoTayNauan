package com.sotaynauan.ai.data.model;

public class CommunityComment {
    private final String id;
    private final String shareId;
    private final String authorName;
    private final String body;
    private final long createdAtMillis;

    public CommunityComment(String id, String shareId, String authorName, String body,
                            long createdAtMillis) {
        this.id = id;
        this.shareId = shareId;
        this.authorName = authorName;
        this.body = body;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() { return id; }
    public String getShareId() { return shareId; }
    public String getAuthorName() { return authorName; }
    public String getBody() { return body; }
    public long getCreatedAtMillis() { return createdAtMillis; }
}
