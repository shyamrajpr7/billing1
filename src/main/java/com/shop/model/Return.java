package com.shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Return {
    private int id;
    private int saleId;
    private String invoiceNumber;
    private int customerId;
    private String customerName;
    private double refundAmount;
    private String reason;
    private String status;
    private int userId;
    private LocalDateTime createdAt;
    private List<SaleItem> items = new ArrayList<>();

    public Return() {
        this.status = "COMPLETED";
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.toLocalDate().toString();
    }

    public String getFormattedRefund() {
        return String.format("₹%.2f", refundAmount);
    }
}
