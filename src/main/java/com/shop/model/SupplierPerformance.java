package com.shop.model;

public class SupplierPerformance {
    private int supplierId;
    private String supplierName;
    private String contactPerson;
    private String phone;
    private int totalOrders;
    private double totalSpend;
    private int receivedOrders;
    private int pendingOrders;
    private int cancelledOrders;
    private int itemsReceived;
    private double avgDaysToReceive;
    private String lastOrderDate;

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalSpend() { return totalSpend; }
    public void setTotalSpend(double totalSpend) { this.totalSpend = totalSpend; }

    public int getReceivedOrders() { return receivedOrders; }
    public void setReceivedOrders(int receivedOrders) { this.receivedOrders = receivedOrders; }

    public int getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }

    public int getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public int getItemsReceived() { return itemsReceived; }
    public void setItemsReceived(int itemsReceived) { this.itemsReceived = itemsReceived; }

    public double getAvgDaysToReceive() { return avgDaysToReceive; }
    public void setAvgDaysToReceive(double avgDaysToReceive) { this.avgDaysToReceive = avgDaysToReceive; }

    public String getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(String lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public String getFormattedSpend() { return String.format("₹%,.2f", totalSpend); }
    public String getFormattedAvgDays() {
        return avgDaysToReceive > 0 ? String.format("%.1f days", avgDaysToReceive) : "—";
    }
}
