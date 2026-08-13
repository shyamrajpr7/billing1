package com.shop.model;

import java.time.LocalDate;

public class Membership {
    public static final String TIER_SILVER = "SILVER";
    public static final String TIER_GOLD = "GOLD";
    public static final String TIER_PLATINUM = "PLATINUM";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private int id;
    private String membershipNumber;
    private int customerId;
    private String customerName;
    private String tier = TIER_SILVER;
    private int points;
    private double totalSpent;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String status = STATUS_ACTIVE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status)
                && (expiryDate == null || !expiryDate.isBefore(LocalDate.now()));
    }

    public String getTierLabel() { return tier; }

    public int getDiscountPercent() {
        return switch (tier) {
            case TIER_PLATINUM -> 10;
            case TIER_GOLD -> 5;
            default -> 2;
        };
    }
}
