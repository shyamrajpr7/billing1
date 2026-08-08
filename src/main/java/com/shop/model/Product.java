package com.shop.model;

public class Product {
    private int id;
    private String name;
    private String barcode;
    private String category;
    private double buyPrice;
    private double sellPrice;
    private int quantity;
    private int minStockLevel;
    private int supplierId;
    private String supplierName;

    public Product() {
        this.minStockLevel = 10;
    }

    public Product(String name, String barcode, String category, double buyPrice,
                   double sellPrice, int quantity, int minStockLevel, int supplierId) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
        this.supplierId = supplierId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public boolean isLowStock() { return quantity <= minStockLevel; }

    public String getStockStatus() {
        if (quantity == 0) return "Out of Stock";
        if (quantity <= minStockLevel) return "Low Stock";
        return "In Stock";
    }

    @Override
    public String toString() { return name; }
}
