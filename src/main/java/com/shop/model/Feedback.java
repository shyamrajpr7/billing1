package com.shop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Feedback {
    public static final String CATEGORY_PRODUCT = "Product";
    public static final String CATEGORY_SERVICE = "Service";
    public static final String CATEGORY_STORE = "Store";
    public static final String CATEGORY_OTHER = "Other";

    private int id;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private int rating = 5;
    private String category = CATEGORY_PRODUCT;
    private String comment;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRatingLabel() {
        return "⭐".repeat(Math.max(0, Math.min(5, rating)));
    }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }
}
