package com.shop.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.IntegerProperty;

/**
 * A smart restock suggestion for a single product. Computes how fast the
 * product sells (average daily sales over the recent window) so the suggested
 * reorder quantity tops stock up to cover roughly two weeks of demand while
 * never dropping below the minimum stock level.
 */
public class ReorderSuggestion {

    public static final String URGENCY_OUT_OF_STOCK = "Out of Stock";
    public static final String URGENCY_CRITICAL = "Critical";
    public static final String URGENCY_LOW = "Low";

    private final Product product;
    private final double avgDailySales;
    private final double daysOfStockLeft;

    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    private final IntegerProperty suggestedQty = new SimpleIntegerProperty(0);

    public ReorderSuggestion(Product product, double avgDailySales, double daysOfStockLeft, int suggestedQty) {
        this.product = product;
        this.avgDailySales = avgDailySales;
        this.daysOfStockLeft = daysOfStockLeft;
        this.suggestedQty.set(suggestedQty);
    }

    public Product getProduct() {
        return product;
    }

    public double getAvgDailySales() {
        return avgDailySales;
    }

    public double getDaysOfStockLeft() {
        return daysOfStockLeft;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public int getSuggestedQty() {
        return suggestedQty.get();
    }

    public void setSuggestedQty(int suggestedQty) {
        this.suggestedQty.set(Math.max(suggestedQty, 0));
    }

    public IntegerProperty suggestedQtyProperty() {
        return suggestedQty;
    }

    public double getEstimatedCost() {
        return suggestedQty.get() * product.getBuyPrice();
    }

    public String getDaysOfStockLabel() {
        if (avgDailySales <= 0) return "—";
        return String.format("%.1f days", daysOfStockLeft);
    }

    public String getUrgency() {
        if (product.getQuantity() <= 0) return URGENCY_OUT_OF_STOCK;
        if (daysOfStockLeft < 3) return URGENCY_CRITICAL;
        return URGENCY_LOW;
    }

    @Override
    public String toString() {
        return product.getName() + " x" + suggestedQty.get();
    }
}
