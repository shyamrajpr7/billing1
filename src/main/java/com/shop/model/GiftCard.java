package com.shop.model;

import java.time.LocalDateTime;

public class GiftCard {
    private int id;
    private String cardNumber;
    private double initialAmount;
    private double balance;
    private int customerId;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;

    public GiftCard() {
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public double getInitialAmount() { return initialAmount; }
    public void setInitialAmount(double initialAmount) { this.initialAmount = initialAmount; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedBalance() { return String.format("₹%.2f", balance); }
    public String getFormattedInitial() { return String.format("₹%.2f", initialAmount); }
    public String getFormattedDate() { return createdAt == null ? "" : createdAt.toLocalDate().toString(); }

    public boolean isActive() { return "ACTIVE".equals(status); }
}
