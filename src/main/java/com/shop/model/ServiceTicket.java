package com.shop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceTicket {
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";

    private int id;
    private String ticketNumber;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String subject;
    private String description;
    private String status = STATUS_OPEN;
    private String priority = PRIORITY_NORMAL;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "—";
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }
}
