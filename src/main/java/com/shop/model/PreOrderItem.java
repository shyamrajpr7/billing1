package com.shop.model;

public class PreOrderItem {
    private int productId;
    private String productName;
    private String barcode;
    private int quantity;
    private double unitPrice;
    private double total;

    public PreOrderItem() {}

    public PreOrderItem(int productId, String productName, String barcode, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = quantity * unitPrice;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.total = quantity * unitPrice;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.total = quantity * unitPrice;
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
