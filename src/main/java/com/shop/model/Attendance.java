package com.shop.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Attendance {
    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_ABSENT = "ABSENT";
    public static final String STATUS_LEAVE = "LEAVE";
    public static final String STATUS_HALF_DAY = "HALF_DAY";

    private int id;
    private int userId;
    private String employeeName;
    private LocalDate date;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String status = STATUS_PRESENT;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDateLabel() {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public String getCheckInLabel() {
        return checkInTime != null ? checkInTime.withSecond(0).toString() : "—";
    }

    public String getCheckOutLabel() {
        return checkOutTime != null ? checkOutTime.withSecond(0).toString() : "—";
    }

    public double getHoursWorked() {
        if (checkInTime == null || checkOutTime == null) return 0;
        return Math.round(java.time.Duration.between(checkInTime, checkOutTime).toMinutes() / 60.0 * 100.0) / 100.0;
    }

    public boolean isCheckedIn() {
        return checkInTime != null && checkOutTime == null;
    }
}
