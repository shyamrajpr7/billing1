package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PreOrder {
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_FULFILLED = "Fulfilled";
    public static final String STATUS_CANCELLED = "Cancelled";

    private int id;
    private String orderNumber;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private List<PreOrderItem> items = new ArrayList<>();
    private String status = STATUS_PENDING;
    private double totalAmount;
    private double advancePaid;
    private LocalDate pickupDate;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
    private String saleInvoiceNumber;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<PreOrderItem> getItems() { return items; }
    public void setItems(List<PreOrderItem> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getAdvancePaid() { return advancePaid; }
    public void setAdvancePaid(double advancePaid) { this.advancePaid = advancePaid; }

    public double getBalanceDue() { return Math.max(0, totalAmount - advancePaid); }

    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(LocalDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }

    public String getSaleInvoiceNumber() { return saleInvoiceNumber; }
    public void setSaleInvoiceNumber(String saleInvoiceNumber) { this.saleInvoiceNumber = saleInvoiceNumber; }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.toLocalDate().toString() + " " + createdAt.toLocalTime().withNano(0) : "—";
    }

    public String getPickupDateLabel() {
        return pickupDate != null ? pickupDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public int getItemCount() {
        int count = 0;
        for (PreOrderItem item : items) count += item.getQuantity();
        return count;
    }

    public void computeTotal() {
        totalAmount = 0;
        for (PreOrderItem item : items) totalAmount += item.getTotal();
    }
}
