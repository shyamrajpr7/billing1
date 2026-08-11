package com.shop.model;

import java.time.LocalDateTime;

public class PriceChange {
    private int id;
    private int productId;
    private String productName;
    private String field;
    private double oldValue;
    private double newValue;
    private int userId;
    private String userName;
    private LocalDateTime changedAt;

    public PriceChange() {
        this.changedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public double getOldValue() { return oldValue; }
    public void setOldValue(double oldValue) { this.oldValue = oldValue; }

    public double getNewValue() { return newValue; }
    public void setNewValue(double newValue) { this.newValue = newValue; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getFormattedDate() {
        if (changedAt == null) return "";
        return changedAt.toString().replace("T", " ");
    }

    public String getFormattedOld() { return String.format("₹%.2f", oldValue); }
    public String getFormattedNew() { return String.format("₹%.2f", newValue); }
}
