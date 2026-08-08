package com.shop.model;

import java.time.LocalDate;

public class Discount {
    private int id;
    private String code;
    private String description;
    private String type; // "PERCENTAGE" or "FLAT"
    private double value;
    private double minPurchase;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public Discount() {
        this.active = true;
        this.startDate = LocalDate.now();
        this.endDate = LocalDate.now().plusMonths(1);
        this.type = "PERCENTAGE";
    }

    public Discount(String code, String description, String type, double value,
                    double minPurchase, LocalDate startDate, LocalDate endDate) {
        this.code = code;
        this.description = description;
        this.type = type;
        this.value = value;
        this.minPurchase = minPurchase;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getMinPurchase() { return minPurchase; }
    public void setMinPurchase(double minPurchase) { this.minPurchase = minPurchase; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getStatus() {
        if (!active) return "Inactive";
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return "Upcoming";
        if (today.isAfter(endDate)) return "Expired";
        return "Active";
    }

    public String getValueDisplay() {
        if ("PERCENTAGE".equals(type)) return value + "%";
        return "₹" + String.format("%.2f", value);
    }

    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return active && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    public double apply(double amount) {
        if (!isValid() || amount < minPurchase) return 0;
        if ("PERCENTAGE".equals(type)) return amount * (value / 100.0);
        return Math.min(value, amount);
    }
}
