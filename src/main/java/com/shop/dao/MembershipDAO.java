package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Membership;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MembershipDAO {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> memberships = db.getCollection("memberships");

    public boolean create(Membership m) {
        try {
            int id = db.nextId("memberships");
            m.setId(id);
            m.setMembershipNumber(generateNumber());
            if (m.getStartDate() == null) m.setStartDate(LocalDate.now());
            if (m.getExpiryDate() == null) m.setExpiryDate(LocalDate.now().plusYears(1));
            Document doc = new Document("_id", id)
                    .append("membership_number", m.getMembershipNumber())
                    .append("customer_id", m.getCustomerId())
                    .append("tier", m.getTier())
                    .append("points", m.getPoints())
                    .append("total_spent", m.getTotalSpent())
                    .append("start_date", m.getStartDate().toString())
                    .append("expiry_date", m.getExpiryDate().toString())
                    .append("status", m.getStatus());
            memberships.insertOne(doc);
            new ActivityLogDAO().log("MEMBERSHIP", "Enrolled " + (m.getCustomerName() != null ? m.getCustomerName() : "customer")
                    + " into " + m.getTier() + " loyalty membership " + m.getMembershipNumber());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Membership> findAll() {
        List<Membership> list = new ArrayList<>();
        for (Document doc : memberships.find().sort(new Document("start_date", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public Membership findByCustomerId(int customerId) {
        Document doc = memberships.find(Filters.eq("customer_id", customerId)).first();
        return doc != null ? mapRow(doc) : null;
    }

    public boolean changeTier(int id, String tier) {
        if (!Membership.TIER_SILVER.equals(tier) && !Membership.TIER_GOLD.equals(tier)
                && !Membership.TIER_PLATINUM.equals(tier)) {
            return false;
        }
        return memberships.updateOne(Filters.eq("_id", id), Updates.set("tier", tier)).getMatchedCount() > 0;
    }

    public boolean addPoints(int id, int points) {
        if (points <= 0) return false;
        var result = memberships.updateOne(Filters.eq("_id", id), Updates.inc("points", points));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("MEMBERSHIP", "Added " + points + " loyalty points to membership #" + id);
            return true;
        }
        return false;
    }

    public boolean redeemPoints(int id, int points) {
        if (points <= 0) return false;
        Bson filter = Filters.and(Filters.eq("_id", id), Filters.gte("points", points));
        var result = memberships.updateOne(filter, Updates.inc("points", -points));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("MEMBERSHIP", "Redeemed " + points + " loyalty points from membership #" + id);
            return true;
        }
        return false;
    }

    public boolean addSpend(int id, double amount) {
        if (amount <= 0) return false;
        return memberships.updateOne(Filters.eq("_id", id), Updates.inc("total_spent", amount)).getMatchedCount() > 0;
    }

    public boolean deactivate(int id) {
        var result = memberships.updateOne(Filters.eq("_id", id), Updates.set("status", Membership.STATUS_EXPIRED));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("MEMBERSHIP", "Expired membership #" + id);
            return true;
        }
        return false;
    }

    public int count() {
        return (int) memberships.countDocuments();
    }

    public int countActive() {
        return (int) memberships.countDocuments(Filters.eq("status", Membership.STATUS_ACTIVE));
    }

    public int totalPoints() {
        int total = 0;
        for (Document doc : memberships.find()) {
            total += doc.getInteger("points") != null ? doc.getInteger("points") : 0;
        }
        return total;
    }

    public String generateNumber() {
        StringBuilder sb = new StringBuilder("MBR-");
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private Membership mapRow(Document doc) {
        Membership m = new Membership();
        m.setId(doc.getInteger("_id"));
        m.setMembershipNumber(doc.getString("membership_number"));
        Integer cid = doc.getInteger("customer_id");
        m.setCustomerId(cid != null ? cid : 0);
        m.setTier(doc.getString("tier") != null ? doc.getString("tier") : Membership.TIER_SILVER);
        m.setPoints(doc.getInteger("points") != null ? doc.getInteger("points") : 0);
        m.setTotalSpent(doc.getDouble("total_spent") != null ? doc.getDouble("total_spent") : 0);
        String start = doc.getString("start_date");
        if (start != null) {
            try { m.setStartDate(LocalDate.parse(start)); } catch (Exception ignored) {}
        }
        String expiry = doc.getString("expiry_date");
        if (expiry != null) {
            try { m.setExpiryDate(LocalDate.parse(expiry)); } catch (Exception ignored) {}
        }
        m.setStatus(doc.getString("status") != null ? doc.getString("status") : Membership.STATUS_ACTIVE);
        if (m.getCustomerId() > 0) {
            Document c = db.getCollection("customers").find(Filters.eq("_id", m.getCustomerId())).first();
            if (c != null) m.setCustomerName(c.getString("name"));
        }
        return m;
    }
}
