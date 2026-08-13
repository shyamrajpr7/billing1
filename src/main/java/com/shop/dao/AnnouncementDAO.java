package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.Announcement;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> announcements = db.getCollection("announcements");

    public boolean create(Announcement a) {
        try {
            int id = db.nextId("announcements");
            a.setId(id);
            a.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("title", a.getTitle())
                    .append("message", a.getMessage())
                    .append("created_by", a.getCreatedBy() != null ? a.getCreatedBy() : "")
                    .append("created_by_id", a.getCreatedById())
                    .append("created_at", a.getCreatedAt().toString())
                    .append("pinned", a.isPinned())
                    .append("expires_at", a.getExpiresAt() != null ? a.getExpiresAt().toString() : null)
                    .append("priority", a.getPriority());
            announcements.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void removeExpired() {
        String today = LocalDate.now().toString();
        announcements.deleteMany(Filters.and(
                Filters.exists("expires_at"),
                Filters.lt("expires_at", today)));
    }

    public List<Announcement> findAll() {
        removeExpired();
        List<Announcement> list = new ArrayList<>();
        for (Document doc : announcements.find().sort(new Document("pinned", -1)).sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Announcement> findActive() {
        List<Announcement> list = new ArrayList<>();
        for (Announcement a : findAll()) {
            if (a.isActive()) list.add(a);
        }
        return list;
    }

    public boolean delete(int id) {
        return announcements.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public boolean togglePin(int id) {
        Document doc = announcements.find(Filters.eq("_id", id)).first();
        if (doc == null) return false;
        boolean pinned = doc.getBoolean("pinned") != null && doc.getBoolean("pinned");
        announcements.updateOne(Filters.eq("_id", id),
                new Document("$set", new Document("pinned", !pinned)));
        return true;
    }

    public int countActive() {
        return findActive().size();
    }

    private Announcement mapRow(Document doc) {
        Announcement a = new Announcement();
        a.setId(doc.getInteger("_id"));
        a.setTitle(doc.getString("title"));
        a.setMessage(doc.getString("message"));
        a.setCreatedBy(doc.getString("created_by"));
        a.setCreatedById(doc.getInteger("created_by_id") != null ? doc.getInteger("created_by_id") : 0);
        String created = doc.getString("created_at");
        if (created != null) {
            try { a.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        a.setPinned(doc.getBoolean("pinned") != null && doc.getBoolean("pinned"));
        String expires = doc.getString("expires_at");
        if (expires != null) {
            try { a.setExpiresAt(LocalDate.parse(expires)); } catch (Exception ignored) {}
        }
        a.setPriority(doc.getString("priority") != null ? doc.getString("priority") : Announcement.PRIORITY_NORMAL);
        return a;
    }
}
