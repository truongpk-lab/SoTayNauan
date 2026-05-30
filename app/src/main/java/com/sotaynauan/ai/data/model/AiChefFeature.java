package com.sotaynauan.ai.data.model;

public class AiChefFeature {
    private final String id;
    private final String title;
    private final String description;
    private final String iconLabel;
    private final int accentColor;
    private final boolean primary;

    public AiChefFeature(String id, String title, String description, String iconLabel,
                         int accentColor, boolean primary) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconLabel = iconLabel;
        this.accentColor = accentColor;
        this.primary = primary;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconLabel() { return iconLabel; }
    public int getAccentColor() { return accentColor; }
    public boolean isPrimary() { return primary; }
}
