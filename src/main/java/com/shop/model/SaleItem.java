package com.shop.model;

public class SaleItem {
    private int id;
    private int saleId;
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double discount;
    private double total;

    public SaleItem() {}

    public SaleItem(int productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = 0;
        this.total = quantity * unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculate();
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        recalculate();
    }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) {
        this.discount = discount;
        recalculate();
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    private void recalculate() {
        this.total = (quantity * unitPrice) - discount;
    }

    public String getFormattedTotal() {
        return String.format("₹%.2f", total);
    }

    public String getFormattedUnitPrice() {
        return String.format("₹%.2f", unitPrice);
    }
}
