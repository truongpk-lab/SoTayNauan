package com.sotaynauan.ai.data.model;

public class ConfirmedIngredient {
    public static final String SOURCE_CAMERA = "camera";
    public static final String SOURCE_KEYBOARD = "keyboard";
    public static final String SOURCE_KITCHEN = "kitchen";

    private final String id;
    private final String name;
    private final String quantity;
    private final String source;
    private final boolean selected;

    public ConfirmedIngredient(String id, String name, String quantity, String source, boolean selected) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.source = source;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getSource() {
        return source;
    }

    public boolean isSelected() {
        return selected;
    }

    public ConfirmedIngredient withSelection(boolean nextSelected) {
        return new ConfirmedIngredient(id, name, quantity, source, nextSelected);
    }

    public ConfirmedIngredient withContent(String nextName, String nextQuantity) {
        return new ConfirmedIngredient(id, nextName, nextQuantity, source, selected);
    }
}
