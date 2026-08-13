package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Delivery {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private String deliveryNumber;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String address;
    private String saleInvoiceNumber;
    private String itemsDescription;
    private String status = STATUS_PENDING;
    private LocalDate scheduledDate;
    private LocalDate deliveredDate;
    private String courier;
    private String trackingNumber;
    private String note;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeliveryNumber() { return deliveryNumber; }
    public void setDeliveryNumber(String deliveryNumber) { this.deliveryNumber = deliveryNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSaleInvoiceNumber() { return saleInvoiceNumber; }
    public void setSaleInvoiceNumber(String saleInvoiceNumber) { this.saleInvoiceNumber = saleInvoiceNumber; }

    public String getItemsDescription() { return itemsDescription; }
    public void setItemsDescription(String itemsDescription) { this.itemsDescription = itemsDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDate getDeliveredDate() { return deliveredDate; }
    public void setDeliveredDate(LocalDate deliveredDate) { this.deliveredDate = deliveredDate; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return STATUS_PENDING.equals(status) || STATUS_OUT_FOR_DELIVERY.equals(status);
    }

    public boolean isDelayed() {
        return isActive() && scheduledDate != null && scheduledDate.isBefore(LocalDate.now());
    }

    public String getScheduledDateLabel() {
        return scheduledDate != null ? scheduledDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }
}
