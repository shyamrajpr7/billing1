package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Brand;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BrandDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> brands = db.getCollection("brands");

    public boolean create(Brand b) {
        try {
            if (findByName(b.getName()) != null) return false;
            int id = db.nextId("brands");
            b.setId(id);
            b.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("name", b.getName())
                    .append("manufacturer", b.getManufacturer() != null ? b.getManufacturer() : "")
                    .append("origin_country", b.getOriginCountry() != null ? b.getOriginCountry() : "")
                    .append("description", b.getDescription() != null ? b.getDescription() : "")
                    .append("contact", b.getContact() != null ? b.getContact() : "")
                    .append("created_at", b.getCreatedAt().toString());
            brands.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Brand findByName(String name) {
        if (name == null || name.isBlank()) return null;
        Document doc = brands.find(Filters.eq("name", name.trim())).first();
        return doc != null ? mapRow(doc) : null;
    }

    public List<Brand> findAll() {
        List<Brand> list = new ArrayList<>();
        for (Document doc : brands.find().sort(new Document("name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public boolean update(int id, String manufacturer, String originCountry, String description, String contact) {
        var result = brands.updateOne(Filters.eq("_id", id), Updates.combine(
                Updates.set("manufacturer", manufacturer != null ? manufacturer : ""),
                Updates.set("origin_country", originCountry != null ? originCountry : ""),
                Updates.set("description", description != null ? description : ""),
                Updates.set("contact", contact != null ? contact : "")));
        return result.getMatchedCount() > 0;
    }

    public boolean delete(int id) {
        return brands.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    public int count() {
        return (int) brands.countDocuments();
    }

    private Brand mapRow(Document doc) {
        Brand b = new Brand();
        b.setId(doc.getInteger("_id"));
        b.setName(doc.getString("name"));
        b.setManufacturer(doc.getString("manufacturer"));
        b.setOriginCountry(doc.getString("origin_country"));
        b.setDescription(doc.getString("description"));
        b.setContact(doc.getString("contact"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { b.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return b;
    }
}
