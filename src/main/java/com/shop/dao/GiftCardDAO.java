package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.GiftCard;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GiftCardDAO {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> cards = db.getCollection("gift_cards");

    public boolean create(GiftCard gc) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String number = generateNumber();
            try {
                int id = db.nextId("gift_cards");
                gc.setId(id);
                gc.setCardNumber(number);
                Document doc = new Document("_id", id)
                        .append("card_number", number)
                        .append("initial_amount", gc.getInitialAmount())
                        .append("balance", gc.getBalance())
                        .append("customer_id", gc.getCustomerId())
                        .append("status", "ACTIVE")
                        .append("created_at", LocalDateTime.now().toString());
                cards.insertOne(doc);
                new ActivityLogDAO().log("GIFT_CARD", "Issued gift card " + number
                        + " worth ₹" + String.format("%.2f", gc.getInitialAmount())
                        + (gc.getCustomerId() > 0 ? " for " + gc.getCustomerName() : ""));
                return true;
            } catch (com.mongodb.MongoWriteException ex) {
                // Duplicate card number, retry with a new one
            }
        }
        return false;
    }

    public GiftCard findByNumber(String number) {
        Document doc = cards.find(Filters.eq("card_number", number)).first();
        return doc == null ? null : mapRow(doc);
    }

    public List<GiftCard> findAll() {
        List<GiftCard> list = new ArrayList<>();
        for (Document doc : cards.find().sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public boolean deductBalance(String number, double amount) {
        if (amount <= 0) return false;
        Bson filter = Filters.and(
                Filters.eq("card_number", number),
                Filters.eq("status", "ACTIVE"),
                Filters.gte("balance", amount));
        var result = cards.updateOne(filter, Updates.inc("balance", -amount));
        return result.getMatchedCount() > 0;
    }

    public boolean topUp(String number, double amount) {
        if (amount <= 0) return false;
        Bson filter = Filters.and(
                Filters.eq("card_number", number),
                Filters.eq("status", "ACTIVE"));
        var result = cards.updateOne(filter, Updates.inc("balance", amount));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("GIFT_CARD", "Topped up " + number + " by ₹" + String.format("%.2f", amount));
            return true;
        }
        return false;
    }

    public boolean deactivate(String number) {
        var result = cards.updateOne(Filters.eq("card_number", number), Updates.set("status", "INACTIVE"));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("GIFT_CARD", "Deactivated gift card " + number);
            return true;
        }
        return false;
    }

    public double totalActiveBalance() {
        double total = 0;
        for (Document doc : cards.find(Filters.eq("status", "ACTIVE"))) {
            total += doc.getDouble("balance") != null ? doc.getDouble("balance") : 0;
        }
        return total;
    }

    public int count() {
        return (int) cards.countDocuments();
    }

    public String generateNumber() {
        StringBuilder sb = new StringBuilder("GC-");
        for (int i = 0; i < 8; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private GiftCard mapRow(Document doc) {
        GiftCard gc = new GiftCard();
        gc.setId(doc.getInteger("_id"));
        gc.setCardNumber(doc.getString("card_number"));
        gc.setInitialAmount(doc.getDouble("initial_amount") != null ? doc.getDouble("initial_amount") : 0);
        gc.setBalance(doc.getDouble("balance") != null ? doc.getDouble("balance") : 0);
        gc.setCustomerId(doc.getInteger("customer_id") != null ? doc.getInteger("customer_id") : 0);
        gc.setStatus(doc.getString("status"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { gc.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        if (gc.getCustomerId() > 0) {
            Document c = db.getCollection("customers").find(Filters.eq("_id", gc.getCustomerId())).first();
            if (c != null) gc.setCustomerName(c.getString("name"));
        }
        return gc;
    }
}
