package com.shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Stocktake {
    private int id;
    private String name;
    private String status;
    private int createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<StocktakeItem> items = new ArrayList<>();

    public Stocktake() {
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<StocktakeItem> getItems() { return items; }
    public void setItems(List<StocktakeItem> items) { this.items = items; }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.toLocalDate().toString();
    }

    public boolean isOpen() { return "OPEN".equals(status); }

    public int getItemCount() { return items.size(); }

    public int getAdjustedCount() {
        int n = 0;
        for (StocktakeItem item : items) {
            if (item.getVariance() != 0) n++;
        }
        return n;
    }
}
