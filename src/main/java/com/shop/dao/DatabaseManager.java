package com.shop.dao;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.shop.model.Role;
import com.shop.util.PasswordUtil;
import org.bson.Document;

import java.time.LocalDateTime;

public class DatabaseManager {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DB_NAME = "shop_management";
    private static DatabaseManager instance;

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    private DatabaseManager() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);
        createIndexes();
        seedDefaultAdmin();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public MongoClient getClient() {
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    /**
     * Returns the next auto-increment id for the given collection.
     * Keeps integer ids so the rest of the application is unchanged.
     */
    public int nextId(String collection) {
        Document result = database.getCollection("counters").findOneAndUpdate(
                new Document("_id", collection),
                new Document("$inc", new Document("seq", 1)),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return result.getInteger("seq");
    }

    private void createIndexes() {
        getCollection("users").createIndex(
                Indexes.ascending("username"),
                new IndexOptions().unique(true));
        getCollection("products").createIndex(
                Indexes.ascending("barcode"),
                new IndexOptions().unique(true));
        getCollection("sales").createIndex(
                Indexes.ascending("invoice_number"),
                new IndexOptions().unique(true));
        getCollection("discounts").createIndex(
                Indexes.ascending("code"),
                new IndexOptions().unique(true));
        getCollection("sales").createIndex(Indexes.descending("created_at"));
        getCollection("products").createIndex(Indexes.ascending("name"));
    }

    private void seedDefaultAdmin() {
        MongoCollection<Document> users = getCollection("users");
        if (users.countDocuments(new Document("username", "admin")) > 0) {
            return;
        }

        Document admin = new Document("_id", nextId("users"))
                .append("username", "admin")
                .append("password_hash", PasswordUtil.hash("admin123"))
                .append("full_name", "System Administrator")
                .append("role", Role.ADMIN.name())
                .append("active", true)
                .append("created_at", LocalDateTime.now().toString());
        users.insertOne(admin);
        System.out.println("Default admin user created (admin / admin123)");
    }
}
