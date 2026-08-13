package com.shop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CalendarEvent {
    public static final String TYPE_HOLIDAY = "HOLIDAY";
    public static final String TYPE_SALE = "SALE";
    public static final String TYPE_MEETING = "MEETING";
    public static final String TYPE_OTHER = "OTHER";

    private int id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime time;
    private String eventType = TYPE_OTHER;
    private String createdBy;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDateLabel() {
        return date != null ? date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) : "—";
    }

    public String getTimeLabel() {
        return time != null ? time.withSecond(0).toString() : "—";
    }

    public boolean isUpcoming() {
        if (date == null) return false;
        if (date.isAfter(LocalDate.now())) return true;
        return date.equals(LocalDate.now())
                && (time == null || !time.isBefore(LocalTime.now()));
    }
}
