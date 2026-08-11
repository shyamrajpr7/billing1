package com.shop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Shift {
    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_CLOSED = "Closed";

    private int id;
    private int cashierId;
    private String cashierName;
    private double openingBalance;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private double countedCash;
    private double expectedCash;
    private double variance;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public double getCountedCash() { return countedCash; }
    public void setCountedCash(double countedCash) { this.countedCash = countedCash; }

    public double getExpectedCash() { return expectedCash; }
    public void setExpectedCash(double expectedCash) { this.expectedCash = expectedCash; }

    public double getVariance() { return variance; }
    public void setVariance(double variance) { this.variance = variance; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getOpenedAtLabel() {
        return openedAt != null
                ? openedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "—";
    }

    public String getClosedAtLabel() {
        return closedAt != null
                ? closedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "—";
    }

    public String getFormattedOpening() { return String.format("₹%,.2f", openingBalance); }
    public String getFormattedExpected() { return String.format("₹%,.2f", expectedCash); }
    public String getFormattedCounted() { return String.format("₹%,.2f", countedCash); }
    public String getFormattedVariance() {
        return (variance >= 0 ? "+" : "") + String.format("₹%,.2f", variance);
    }
}
