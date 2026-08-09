package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Role;
import com.shop.model.User;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> users = db.getCollection("users");

    public User authenticate(String username, String passwordHash) {
        Document doc = users.find(Filters.and(
                Filters.eq("username", username),
                Filters.eq("password_hash", passwordHash),
                Filters.eq("active", true))).first();
        return doc != null ? mapRow(doc) : null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        for (Document doc : users.find().sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public User findById(int id) {
        Document doc = users.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc) : null;
    }

    public boolean insert(User user) {
        try {
            int id = db.nextId("users");
            user.setId(id);
            Document doc = new Document("_id", id)
                    .append("username", user.getUsername())
                    .append("password_hash", user.getPasswordHash())
                    .append("full_name", user.getFullName())
                    .append("role", user.getRole().name())
                    .append("active", user.isActive())
                    .append("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            users.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(User user) {
        try {
            Bson filter = Filters.eq("_id", user.getId());
            Bson update = Updates.combine(
                    Updates.set("username", user.getUsername()),
                    Updates.set("full_name", user.getFullName()),
                    Updates.set("role", user.getRole().name()),
                    Updates.set("active", user.isActive()));
            users.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        try {
            users.updateOne(Filters.eq("_id", userId), Updates.set("password_hash", newPasswordHash));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTheme(int userId, boolean dark) {
        try {
            users.updateOne(Filters.eq("_id", userId), Updates.set("dark_theme", dark));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            users.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int count() {
        return (int) users.countDocuments(Filters.eq("active", true));
    }

    private User mapRow(Document doc) {
        User user = new User();
        user.setId(doc.getInteger("_id"));
        user.setUsername(doc.getString("username"));
        user.setPasswordHash(doc.getString("password_hash"));
        user.setFullName(doc.getString("full_name"));
        user.setRole(Role.valueOf(doc.getString("role")));
        user.setActive(Boolean.TRUE.equals(doc.getBoolean("active")));
        user.setDarkTheme(Boolean.TRUE.equals(doc.getBoolean("dark_theme")));
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            user.setCreatedAt(java.time.LocalDateTime.parse(createdAt));
        }
        return user;
    }
}
