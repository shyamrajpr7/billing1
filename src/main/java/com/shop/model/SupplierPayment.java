package com.shop.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SupplierPayment {
    private int id;
    private String paymentNumber;
    private int supplierId;
    private String supplierName;
    private double amount;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String reference;
    private String note;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getPaymentDateLabel() {
        return paymentDate != null ? paymentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }
}
