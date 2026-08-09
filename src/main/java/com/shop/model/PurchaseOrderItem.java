package com.shop.model;

public class PurchaseOrderItem {
    private int productId;
    private String productName;
    private String barcode;
    private int quantity;
    private double unitCost;
    private double total;

    public PurchaseOrderItem() {}

    public PurchaseOrderItem(int productId, String productName, String barcode, int quantity, double unitCost) {
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.total = quantity * unitCost;
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
        this.total = quantity * unitCost;
    }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
        this.total = quantity * unitCost;
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
