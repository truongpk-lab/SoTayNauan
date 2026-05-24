package com.sotaynauan.ai.data.model;

public class ShoppingPlanItem {
    private final String id;
    private final String name;
    private final int amount;
    private final String unit;
    private final String note;
    private final String category;
    private final ShoppingItemStatus status;
    private final boolean committed;

    public ShoppingPlanItem(String id, String name, int amount, String unit, String note,
                            String category, ShoppingItemStatus status) {
        this(id, name, amount, unit, note, category, status, false);
    }

    public ShoppingPlanItem(String id, String name, int amount, String unit, String note,
                            String category, ShoppingItemStatus status, boolean committed) {
        this.id = id;
        this.name = name;
        this.amount = Math.max(1, amount);
        this.unit = unit == null ? "" : unit;
        this.note = note == null ? "" : note;
        this.category = category == null ? "" : category;
        this.status = status == null ? ShoppingItemStatus.NEED_BUY : status;
        this.committed = committed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public String getUnit() {
        return unit;
    }

    public String getNote() {
        return note;
    }

    public String getCategory() {
        return category;
    }

    public ShoppingItemStatus getStatus() {
        return status;
    }

    public boolean isCommitted() {
        return committed;
    }

    public String getQuantityText() {
        String quantity = amount + (unit.isEmpty() ? "" : " " + unit);
        return note.isEmpty() ? quantity : quantity + " • " + note;
    }

    public boolean isActiveForShoppingList() {
        return status == ShoppingItemStatus.NEED_BUY || status == ShoppingItemStatus.BOUGHT;
    }

    public ShoppingPlanItem withStatus(ShoppingItemStatus nextStatus) {
        return new ShoppingPlanItem(id, name, amount, unit, note, category, nextStatus, committed);
    }

    public ShoppingPlanItem withId(String nextId) {
        return new ShoppingPlanItem(nextId, name, amount, unit, note, category, status, committed);
    }

    public ShoppingPlanItem withAmount(int nextAmount) {
        return new ShoppingPlanItem(id, name, nextAmount, unit, note, category, status, committed);
    }

    public ShoppingPlanItem withCommitted(boolean nextCommitted) {
        return new ShoppingPlanItem(id, name, amount, unit, note, category, status, nextCommitted);
    }
}
