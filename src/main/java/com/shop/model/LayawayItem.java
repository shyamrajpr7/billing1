package com.shop.model;

public class LayawayItem {
    private int productId;
    private String productName;
    private String barcode;
    private int quantity;
    private double unitPrice;

    public LayawayItem() {
    }

    public LayawayItem(int productId, String productName, String barcode, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getTotal() {
        return Math.round(quantity * unitPrice * 100.0) / 100.0;
    }
}
