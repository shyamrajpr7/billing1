package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Expense {
    private int id;
    private String description;
    private String category;
    private double amount;
    private LocalDate expenseDate;
    private int userId;
    private LocalDateTime createdAt;

    public Expense() {
        this.expenseDate = LocalDate.now();
        this.category = "General";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
