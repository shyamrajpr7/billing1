package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.CalendarEvent;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CalendarEventDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> events = db.getCollection("calendar_events");

    public boolean create(CalendarEvent e) {
        try {
            int id = db.nextId("calendar_events");
            e.setId(id);
            e.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("title", e.getTitle())
                    .append("description", e.getDescription() != null ? e.getDescription() : "")
                    .append("date", e.getDate() != null ? e.getDate().toString() : null)
                    .append("time", e.getTime() != null ? e.getTime().toString() : null)
                    .append("event_type", e.getEventType())
                    .append("created_by", e.getCreatedBy() != null ? e.getCreatedBy() : "")
                    .append("created_at", e.getCreatedAt().toString());
            events.insertOne(doc);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<CalendarEvent> findAll() {
        List<CalendarEvent> list = new ArrayList<>();
        for (Document doc : events.find().sort(new Document("date", 1)).sort(new Document("time", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<CalendarEvent> findUpcoming() {
        String today = LocalDate.now().toString();
        List<CalendarEvent> list = new ArrayList<>();
        for (Document doc : events.find(Filters.gte("date", today))
                .sort(new Document("date", 1)).sort(new Document("time", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public boolean delete(int id) {
        return events.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public int countUpcoming() {
        return findUpcoming().size();
    }

    public int count() {
        return (int) events.countDocuments();
    }

    private CalendarEvent mapRow(Document doc) {
        CalendarEvent e = new CalendarEvent();
        e.setId(doc.getInteger("_id"));
        e.setTitle(doc.getString("title"));
        e.setDescription(doc.getString("description"));
        String date = doc.getString("date");
        if (date != null) {
            try { e.setDate(LocalDate.parse(date)); } catch (Exception ignored) {}
        }
        String time = doc.getString("time");
        if (time != null) {
            try { e.setTime(LocalTime.parse(time)); } catch (Exception ignored) {}
        }
        e.setEventType(doc.getString("event_type") != null ? doc.getString("event_type") : CalendarEvent.TYPE_OTHER);
        e.setCreatedBy(doc.getString("created_by"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { e.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return e;
    }
}
