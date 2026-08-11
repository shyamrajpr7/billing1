package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.PriceChange;
import com.shop.util.SessionManager;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PriceHistoryDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> history = db.getCollection("price_history");

    public void log(int productId, String productName, String field, double oldValue, double newValue) {
        try {
            int id = db.nextId("price_history");
            int userId = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0;
            String userName = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getFullName() : "System";
            history.insertOne(new Document("_id", id)
                    .append("product_id", productId)
                    .append("product_name", productName)
                    .append("field", field)
                    .append("old_value", oldValue)
                    .append("new_value", newValue)
                    .append("user_id", userId)
                    .append("user_name", userName)
                    .append("changed_at", LocalDateTime.now().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<PriceChange> findAll() {
        return find(null, null);
    }

    public List<PriceChange> find(String query, String field) {
        List<Bson> filters = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim();
            Pattern pattern = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);
            filters.add(Filters.or(
                    Filters.regex("product_name", pattern),
                    Filters.regex("user_name", pattern)));
        }
        if (field != null && !field.isEmpty() && !"All Fields".equals(field)) {
            filters.add(Filters.eq("field", field));
        }
        Bson filter = filters.isEmpty() ? null : Filters.and(filters);
        List<PriceChange> list = new ArrayList<>();
        for (Document doc : (filter == null ? history.find() : history.find(filter))
                .sort(new Document("changed_at", -1)).limit(5000)) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int count() {
        return (int) history.countDocuments();
    }

    private PriceChange mapRow(Document doc) {
        PriceChange pc = new PriceChange();
        pc.setId(doc.getInteger("_id"));
        pc.setProductId(doc.getInteger("product_id") != null ? doc.getInteger("product_id") : 0);
        pc.setProductName(doc.getString("product_name"));
        pc.setField(doc.getString("field"));
        pc.setOldValue(doc.getDouble("old_value") != null ? doc.getDouble("old_value") : 0);
        pc.setNewValue(doc.getDouble("new_value") != null ? doc.getDouble("new_value") : 0);
        pc.setUserId(doc.getInteger("user_id") != null ? doc.getInteger("user_id") : 0);
        pc.setUserName(doc.getString("user_name"));
        String changed = doc.getString("changed_at");
        if (changed != null) {
            try { pc.setChangedAt(LocalDateTime.parse(changed)); } catch (Exception ignored) {}
        }
        return pc;
    }
}
