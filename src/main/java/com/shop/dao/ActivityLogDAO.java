package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.ActivityLog;
import com.shop.model.User;
import com.shop.util.SessionManager;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ActivityLogDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> logs = db.getCollection("activity_logs");

    public void log(String action, String details) {
        try {
            User user = SessionManager.getInstance().getCurrentUser();
            int userId = user != null ? user.getId() : 0;
            String userName = user != null ? user.getFullName() : "System";
            int id = db.nextId("activity_logs");
            logs.insertOne(new Document("_id", id)
                    .append("user_id", userId)
                    .append("user_name", userName)
                    .append("action", action)
                    .append("details", details != null ? details : "")
                    .append("created_at", LocalDateTime.now().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logAs(int userId, String userName, String action, String details) {
        try {
            int id = db.nextId("activity_logs");
            logs.insertOne(new Document("_id", id)
                    .append("user_id", userId)
                    .append("user_name", userName)
                    .append("action", action)
                    .append("details", details != null ? details : "")
                    .append("created_at", LocalDateTime.now().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ActivityLog> findAll() {
        return find(null);
    }

    public List<ActivityLog> find(String query) {
        Bson filter = null;
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim();
            Pattern pattern = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);
            filter = Filters.or(
                    Filters.regex("action", pattern),
                    Filters.regex("user_name", pattern),
                    Filters.regex("details", pattern));
        }
        List<ActivityLog> list = new ArrayList<>();
        for (Document doc : (filter == null ? logs.find() : logs.find(filter))
                .sort(new Document("created_at", -1)).limit(2000)) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int count() {
        return (int) logs.countDocuments();
    }

    private ActivityLog mapRow(Document doc) {
        ActivityLog log = new ActivityLog();
        log.setId(doc.getInteger("_id"));
        log.setUserId(doc.getInteger("user_id"));
        log.setUserName(doc.getString("user_name"));
        log.setAction(doc.getString("action"));
        log.setDetails(doc.getString("details"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { log.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return log;
    }
}
