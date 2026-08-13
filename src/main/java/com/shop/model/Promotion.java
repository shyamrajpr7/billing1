package com.shop.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Promotion {
    private int id;
    private String code;
    private String name;
    private String description;
    private double discountPercent;
    private double minPurchase;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active = true;
    private String applicableCategory;
    private int usageLimit;
    private int usageCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double getMinPurchase() { return minPurchase; }
    public void setMinPurchase(double minPurchase) { this.minPurchase = minPurchase; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getApplicableCategory() { return applicableCategory; }
    public void setApplicableCategory(String applicableCategory) { this.applicableCategory = applicableCategory; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public String getDateRangeLabel() {
        String start = startDate != null ? startDate.format(DateTimeFormatter.ofPattern("dd MMM")) : "—";
        String end = endDate != null ? endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
        return start + " → " + end;
    }

    public boolean isValidNow(double purchaseAmount) {
        LocalDate today = LocalDate.now();
        boolean withinDates = (startDate == null || !today.isBefore(startDate))
                && (endDate == null || !today.isAfter(endDate));
        return active && withinDates && purchaseAmount >= minPurchase
                && (usageLimit <= 0 || usageCount < usageLimit);
    }
}
