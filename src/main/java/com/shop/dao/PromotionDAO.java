package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Promotion;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PromotionDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> promotions = db.getCollection("promotions");

    public boolean create(Promotion p) {
        try {
            int id = db.nextId("promotions");
            p.setId(id);
            if (p.getCode() == null || p.getCode().isEmpty()) {
                p.setCode(generateCode(p.getName()));
            }
            Document doc = new Document("_id", id)
                    .append("code", p.getCode())
                    .append("name", p.getName())
                    .append("description", p.getDescription() != null ? p.getDescription() : "")
                    .append("discount_percent", p.getDiscountPercent())
                    .append("min_purchase", p.getMinPurchase())
                    .append("start_date", p.getStartDate() != null ? p.getStartDate().toString() : null)
                    .append("end_date", p.getEndDate() != null ? p.getEndDate().toString() : null)
                    .append("active", p.isActive())
                    .append("applicable_category", p.getApplicableCategory() != null ? p.getApplicableCategory() : "")
                    .append("usage_limit", p.getUsageLimit())
                    .append("usage_count", p.getUsageCount());
            promotions.insertOne(doc);
            new ActivityLogDAO().log("PROMOTION", "Created promotion " + p.getCode()
                    + " (" + p.getDiscountPercent() + "% off)");
            return true;
        } catch (com.mongodb.MongoWriteException ex) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Promotion validateCode(String code, double purchaseAmount) {
        if (code == null || code.isBlank()) return null;
        Document doc = promotions.find(Filters.eq("code", code.toUpperCase().trim())).first();
        if (doc == null) return null;
        Promotion p = mapRow(doc);
        return p.isValidNow(purchaseAmount) ? p : null;
    }

    public boolean incrementUsage(int id) {
        return promotions.updateOne(Filters.eq("_id", id), Updates.inc("usage_count", 1)).getMatchedCount() > 0;
    }

    public boolean toggleActive(int id) {
        Document doc = promotions.find(Filters.eq("_id", id)).first();
        if (doc == null) return false;
        boolean active = doc.getBoolean("active") != null && doc.getBoolean("active");
        promotions.updateOne(Filters.eq("_id", id), Updates.set("active", !active));
        return true;
    }

    public boolean delete(int id) {
        return promotions.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public List<Promotion> findAll() {
        List<Promotion> list = new ArrayList<>();
        for (Document doc : promotions.find().sort(new Document("end_date", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int countActive() {
        String today = LocalDate.now().toString();
        int count = 0;
        for (Document doc : promotions.find(Filters.eq("active", true))) {
            Promotion p = mapRow(doc);
            if (p.isValidNow(0)) count++;
        }
        return count;
    }

    public int count() {
        return (int) promotions.countDocuments();
    }

    private String generateCode(String name) {
        if (name == null || name.isBlank()) name = "SALE";
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)));
        }
        String base = sb.length() >= 3 ? sb.substring(0, Math.min(6, sb.length())) : (sb + "SALE");
        return base + System.currentTimeMillis() % 1000;
    }

    private Promotion mapRow(Document doc) {
        Promotion p = new Promotion();
        p.setId(doc.getInteger("_id"));
        p.setCode(doc.getString("code"));
        p.setName(doc.getString("name"));
        p.setDescription(doc.getString("description"));
        p.setDiscountPercent(doc.getDouble("discount_percent") != null ? doc.getDouble("discount_percent") : 0);
        p.setMinPurchase(doc.getDouble("min_purchase") != null ? doc.getDouble("min_purchase") : 0);
        String start = doc.getString("start_date");
        if (start != null) {
            try { p.setStartDate(LocalDate.parse(start)); } catch (Exception ignored) {}
        }
        String end = doc.getString("end_date");
        if (end != null) {
            try { p.setEndDate(LocalDate.parse(end)); } catch (Exception ignored) {}
        }
        p.setActive(doc.getBoolean("active") == null || doc.getBoolean("active"));
        p.setApplicableCategory(doc.getString("applicable_category"));
        p.setUsageLimit(doc.getInteger("usage_limit") != null ? doc.getInteger("usage_limit") : 0);
        p.setUsageCount(doc.getInteger("usage_count") != null ? doc.getInteger("usage_count") : 0);
        return p;
    }
}
