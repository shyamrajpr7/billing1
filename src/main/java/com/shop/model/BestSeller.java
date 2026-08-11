package com.shop.model;

public class BestSeller {
    private int productId;
    private String name;
    private String category;
    private int units;
    private double revenue;

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }

    public String getFormattedRevenue() { return String.format("₹%.2f", revenue); }
}
