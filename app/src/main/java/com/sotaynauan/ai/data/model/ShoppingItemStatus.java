package com.sotaynauan.ai.data.model;

public enum ShoppingItemStatus {
    NEED_BUY("Cần mua"),
    BOUGHT("Đã mua"),
    AT_HOME("Đã có ở nhà"),
    SKIPPED("Bỏ qua");

    private final String label;

    ShoppingItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ShoppingItemStatus fromName(String value) {
        if (value == null) {
            return NEED_BUY;
        }
        try {
            return ShoppingItemStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return NEED_BUY;
        }
    }
}
