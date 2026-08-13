package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Reminder {
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";

    private int id;
    private String title;
    private String details;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private String priority = PRIORITY_NORMAL;
    private boolean completed;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalTime getDueTime() { return dueTime; }
    public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDueDateLabel() {
        return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public String getDueTimeLabel() {
        return dueTime != null ? dueTime.withSecond(0).toString() : "—";
    }

    public boolean isOverdue() {
        if (completed || dueDate == null) return false;
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) return true;
        if (dueDate.equals(today) && dueTime != null && dueTime.isBefore(LocalTime.now())) return true;
        return false;
    }
}
