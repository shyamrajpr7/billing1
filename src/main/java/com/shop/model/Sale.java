package com.shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int id;
    private String invoiceNumber;
    private int customerId;
    private String customerName;
    private int userId;
    private String userName;
    private double subtotal;
    private double discountAmount;
    private int pointsRedeemed;
    private double tax;
    private double total;
    private String paymentMethod;
    private String giftCardNumber;
    private double giftCardAmount;
    private LocalDateTime createdAt;
    private List<SaleItem> items;

    public Sale() {
        this.createdAt = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.paymentMethod = "Cash";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public int getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(int pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public double getLoyaltyDiscount() { return pointsRedeemed; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getGiftCardNumber() { return giftCardNumber; }
    public void setGiftCardNumber(String giftCardNumber) { this.giftCardNumber = giftCardNumber; }

    public double getGiftCardAmount() { return giftCardAmount; }
    public void setGiftCardAmount(double giftCardAmount) { this.giftCardAmount = giftCardAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }

    public void addItem(SaleItem item) { this.items.add(item); }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.toLocalDate().toString();
    }

    public String getFormattedTotal() {
        return String.format("₹%.2f", total);
    }
}
