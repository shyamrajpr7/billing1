package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Reminder;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> reminders = db.getCollection("reminders");

    public boolean create(Reminder r) {
        try {
            int id = db.nextId("reminders");
            r.setId(id);
            r.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("title", r.getTitle())
                    .append("details", r.getDetails() != null ? r.getDetails() : "")
                    .append("due_date", r.getDueDate() != null ? r.getDueDate().toString() : null)
                    .append("due_time", r.getDueTime() != null ? r.getDueTime().toString() : null)
                    .append("priority", r.getPriority())
                    .append("completed", r.isCompleted())
                    .append("created_at", r.getCreatedAt().toString());
            reminders.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean toggleCompleted(int id) {
        Document doc = reminders.find(Filters.eq("_id", id)).first();
        if (doc == null) return false;
        boolean completed = doc.getBoolean("completed") != null && doc.getBoolean("completed");
        reminders.updateOne(Filters.eq("_id", id), Updates.set("completed", !completed));
        return true;
    }

    public boolean delete(int id) {
        return reminders.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public List<Reminder> findAll() {
        List<Reminder> list = new ArrayList<>();
        for (Document doc : reminders.find().sort(new Document("completed", 1)).sort(new Document("due_date", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int countPending() {
        int count = 0;
        for (Reminder r : findAll()) {
            if (!r.isCompleted()) count++;
        }
        return count;
    }

    public int countOverdue() {
        int count = 0;
        for (Reminder r : findAll()) {
            if (r.isOverdue()) count++;
        }
        return count;
    }

    public int countDueSoon(int days) {
        int count = 0;
        String today = LocalDate.now().toString();
        String limit = LocalDate.now().plusDays(days).toString();
        for (Document doc : reminders.find(Filters.and(
                Filters.eq("completed", false),
                Filters.gte("due_date", today),
                Filters.lte("due_date", limit)))) {
            count++;
        }
        return count;
    }

    private Reminder mapRow(Document doc) {
        Reminder r = new Reminder();
        r.setId(doc.getInteger("_id"));
        r.setTitle(doc.getString("title"));
        r.setDetails(doc.getString("details"));
        String date = doc.getString("due_date");
        if (date != null) {
            try { r.setDueDate(LocalDate.parse(date)); } catch (Exception ignored) {}
        }
        String time = doc.getString("due_time");
        if (time != null) {
            try { r.setDueTime(LocalTime.parse(time)); } catch (Exception ignored) {}
        }
        r.setPriority(doc.getString("priority") != null ? doc.getString("priority") : Reminder.PRIORITY_NORMAL);
        r.setCompleted(doc.getBoolean("completed") != null && doc.getBoolean("completed"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { r.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return r;
    }
}
