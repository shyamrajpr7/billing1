package com.shop.model;

public class StocktakeItem {
    private int productId;
    private String productName;
    private String barcode;
    private int expectedQty;
    private int countedQty;

    public StocktakeItem() {
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getExpectedQty() { return expectedQty; }
    public void setExpectedQty(int expectedQty) { this.expectedQty = expectedQty; }

    public int getCountedQty() { return countedQty; }
    public void setCountedQty(int countedQty) { this.countedQty = countedQty; }

    public int getVariance() { return countedQty - expectedQty; }
}
