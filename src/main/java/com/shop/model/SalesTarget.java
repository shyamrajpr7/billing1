package com.shop.model;

public class SalesTarget {
    private int id;
    private String month;
    private double target;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getTarget() { return target; }
    public void setTarget(double target) { this.target = target; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getMonthLabel() {
        if (month == null || month.length() != 7) return month;
        try {
            java.time.YearMonth ym = java.time.YearMonth.parse(month);
            return ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        } catch (Exception e) {
            return month;
        }
    }

    public String getFormattedTarget() { return String.format("₹%,.0f", target); }
}
