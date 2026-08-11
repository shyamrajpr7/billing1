package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.SalesTarget;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesTargetDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> targets = db.getCollection("sales_targets");

    public boolean upsert(String month, double target) {
        try {
            if (target <= 0) return false;
            Document existing = targets.find(Filters.eq("month", month)).first();
            if (existing != null) {
                targets.updateOne(Filters.eq("_id", existing.getInteger("_id")),
                        com.mongodb.client.model.Updates.set("target", target));
            } else {
                int id = db.nextId("sales_targets");
                targets.insertOne(new Document("_id", id)
                        .append("month", month)
                        .append("target", target)
                        .append("created_at", LocalDateTime.now().toString()));
            }
            new ActivityLogDAO().log("TARGET_SET", "Sales target for " + month + ": ₹" + String.format("%.0f", target));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getTargetForMonth(String month) {
        Document doc = targets.find(Filters.eq("month", month)).first();
        return doc != null ? doc.getDouble("target") : 0;
    }

    public List<SalesTarget> findAll() {
        List<SalesTarget> list = new ArrayList<>();
        for (Document doc : targets.find().sort(new Document("month", -1))) {
            SalesTarget t = new SalesTarget();
            t.setId(doc.getInteger("_id"));
            t.setMonth(doc.getString("month"));
            t.setTarget(doc.getDouble("target"));
            t.setCreatedAt(doc.getString("created_at"));
            list.add(t);
        }
        return list;
    }
}
