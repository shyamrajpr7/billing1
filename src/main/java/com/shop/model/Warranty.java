package com.shop.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Warranty {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CLAIMED = "CLAIMED";

    private int id;
    private String warrantyNumber;
    private int productId;
    private String productName;
    private String barcode;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate purchaseDate;
    private int warrantyMonths = 12;
    private LocalDate expiryDate;
    private String purchaseInvoiceNumber;
    private String notes;
    private String status = STATUS_ACTIVE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getWarrantyNumber() { return warrantyNumber; }
    public void setWarrantyNumber(String warrantyNumber) { this.warrantyNumber = warrantyNumber; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getPurchaseInvoiceNumber() { return purchaseInvoiceNumber; }
    public void setPurchaseInvoiceNumber(String purchaseInvoiceNumber) { this.purchaseInvoiceNumber = purchaseInvoiceNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status) && expiryDate != null && !expiryDate.isBefore(LocalDate.now());
    }

    public void computeExpiryDate() {
        LocalDate base = purchaseDate != null ? purchaseDate : LocalDate.now();
        expiryDate = base.plusMonths(warrantyMonths);
    }

    public int getDaysRemaining() {
        if (!isActive()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public String getExpiryDateLabel() {
        return expiryDate != null ? expiryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }
}
