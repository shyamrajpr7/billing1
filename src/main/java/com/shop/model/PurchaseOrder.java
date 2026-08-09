package com.shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_RECEIVED = "Received";
    public static final String STATUS_CANCELLED = "Cancelled";

    private int id;
    private String orderNumber;
    private int supplierId;
    private String supplierName;
    private List<PurchaseOrderItem> items = new ArrayList<>();
    private String status = STATUS_DRAFT;
    private double totalCost;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime receivedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.toLocalDate().toString() + " " + createdAt.toLocalTime().withNano(0) : "—";
    }

    public String getReceivedAtLabel() {
        return receivedAt != null ? receivedAt.toLocalDate().toString() + " " + receivedAt.toLocalTime().withNano(0) : "—";
    }

    public int getItemCount() {
        int count = 0;
        for (PurchaseOrderItem item : items) count += item.getQuantity();
        return count;
    }

    public void computeTotal() {
        totalCost = 0;
        for (PurchaseOrderItem item : items) totalCost += item.getTotal();
    }
}
