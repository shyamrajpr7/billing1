package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.Feedback;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> feedbacks = db.getCollection("feedback");

    public boolean create(Feedback f) {
        try {
            int id = db.nextId("feedback");
            f.setId(id);
            f.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("customer_id", f.getCustomerId())
                    .append("customer_name", f.getCustomerName() != null ? f.getCustomerName() : "")
                    .append("customer_phone", f.getCustomerPhone() != null ? f.getCustomerPhone() : "")
                    .append("rating", f.getRating())
                    .append("category", f.getCategory())
                    .append("comment", f.getComment() != null ? f.getComment() : "")
                    .append("created_at", f.getCreatedAt().toString());
            feedbacks.insertOne(doc);
            new ActivityLogDAO().log("FEEDBACK", "Received " + f.getRating() + "-star " + f.getCategory()
                    + " feedback from " + (f.getCustomerName() != null ? f.getCustomerName() : "customer"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Feedback> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : feedbacks.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public double averageRating() {
        double total = 0;
        int count = 0;
        for (Document doc : feedbacks.find()) {
            total += doc.getInteger("rating") != null ? doc.getInteger("rating") : 0;
            count++;
        }
        return count == 0 ? 0 : Math.round(total / count * 100.0) / 100.0;
    }

    public int countPositive() {
        return (int) feedbacks.countDocuments(Filters.gte("rating", 4));
    }

    public int countNegative() {
        return (int) feedbacks.countDocuments(Filters.lte("rating", 2));
    }

    public int count() {
        return (int) feedbacks.countDocuments();
    }

    private List<Feedback> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = new HashMap<>();
        for (Document doc : docs) {
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if (cid != null && (name == null || name.isEmpty())) {
                customerNames.putIfAbsent(cid, loadCustomerName(cid));
            }
        }
        List<Feedback> list = new ArrayList<>();
        for (Document doc : docs) {
            Feedback f = mapRow(doc);
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if ((name == null || name.isEmpty()) && cid != null && customerNames.containsKey(cid)) {
                f.setCustomerName(customerNames.get(cid));
            }
            list.add(f);
        }
        return list;
    }

    private String loadCustomerName(int customerId) {
        Document c = db.getCollection("customers").find(Filters.eq("_id", customerId)).first();
        return c != null ? c.getString("name") : "";
    }

    private Feedback mapRow(Document doc) {
        Feedback f = new Feedback();
        f.setId(doc.getInteger("_id"));
        Integer cid = doc.getInteger("customer_id");
        f.setCustomerId(cid != null ? cid : 0);
        f.setCustomerName(doc.getString("customer_name"));
        f.setCustomerPhone(doc.getString("customer_phone"));
        f.setRating(doc.getInteger("rating") != null ? doc.getInteger("rating") : 5);
        f.setCategory(doc.getString("category") != null ? doc.getString("category") : Feedback.CATEGORY_OTHER);
        f.setComment(doc.getString("comment"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { f.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return f;
    }
}
