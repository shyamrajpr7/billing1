package com.shop.model;

public enum Role {
    ADMIN("Admin"),
    MANAGER("Manager"),
    CASHIER("Cashier");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
