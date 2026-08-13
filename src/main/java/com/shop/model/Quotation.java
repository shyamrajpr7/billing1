package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Quotation {
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ACCEPTED = "Accepted";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_EXPIRED = "Expired";

    private int id;
    private String quoteNumber;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private List<QuotationItem> items = new ArrayList<>();
    private double subtotal;
    private double discountAmount;
    private double tax;
    private double total;
    private String status = STATUS_PENDING;
    private LocalDate validUntil;
    private String note;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<QuotationItem> getItems() { return items; }
    public void setItems(List<QuotationItem> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedTotal() {
        return String.format("₹%.2f", total);
    }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "—";
    }

    public String getValidUntilLabel() {
        return validUntil != null ? validUntil.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public int getItemCount() {
        int count = 0;
        for (QuotationItem item : items) count += item.getQuantity();
        return count;
    }

    public void computeTotals() {
        subtotal = 0;
        for (QuotationItem item : items) subtotal += item.getTotal();
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        total = Math.round((subtotal - discountAmount + tax) * 100.0) / 100.0;
    }
}
