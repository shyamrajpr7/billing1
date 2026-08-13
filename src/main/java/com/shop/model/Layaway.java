package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Layaway {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private String planNumber;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private List<LayawayItem> items = new ArrayList<>();
    private double totalAmount;
    private double downPayment;
    private double amountPaid;
    private int installmentsCount = 1;
    private int installmentsPaid;
    private double installmentAmount;
    private String status = STATUS_ACTIVE;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String note;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlanNumber() { return planNumber; }
    public void setPlanNumber(String planNumber) { this.planNumber = planNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<LayawayItem> getItems() { return items; }
    public void setItems(List<LayawayItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getDownPayment() { return downPayment; }
    public void setDownPayment(double downPayment) { this.downPayment = downPayment; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public int getInstallmentsCount() { return installmentsCount; }
    public void setInstallmentsCount(int installmentsCount) { this.installmentsCount = installmentsCount; }

    public int getInstallmentsPaid() { return installmentsPaid; }
    public void setInstallmentsPaid(int installmentsPaid) { this.installmentsPaid = installmentsPaid; }

    public double getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(double installmentAmount) { this.installmentAmount = installmentAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getBalanceDue() {
        return Math.max(0, totalAmount - amountPaid);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isPaidOff() {
        return amountPaid >= totalAmount - 0.005;
    }

    public void computeTotals() {
        totalAmount = 0;
        for (LayawayItem item : items) totalAmount += item.getTotal();
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;
    }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public String getDueDateLabel() {
        return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }
}
