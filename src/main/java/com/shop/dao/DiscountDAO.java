package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Discount;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DiscountDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> discounts = db.getCollection("discounts");

    public List<Discount> findAll() {
        List<Discount> list = new ArrayList<>();
        for (Document doc : discounts.find().sort(new Document("end_date", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public Discount findByCode(String code) {
        Document doc = discounts.find(Filters.and(
                Filters.regex("code", Pattern.compile("^" + Pattern.quote(code) + "$", Pattern.CASE_INSENSITIVE)),
                Filters.eq("active", true))).first();
        return doc != null ? mapRow(doc) : null;
    }

    public boolean insert(Discount discount) {
        try {
            int id = db.nextId("discounts");
            discount.setId(id);
            Document doc = new Document("_id", id)
                    .append("code", discount.getCode().toUpperCase())
                    .append("description", discount.getDescription() != null ? discount.getDescription() : "")
                    .append("type", discount.getType())
                    .append("value", discount.getValue())
                    .append("min_purchase", discount.getMinPurchase())
                    .append("start_date", discount.getStartDate() != null ? discount.getStartDate().toString() : null)
                    .append("end_date", discount.getEndDate() != null ? discount.getEndDate().toString() : null)
                    .append("active", discount.isActive());
            discounts.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Discount discount) {
        try {
            Bson filter = Filters.eq("_id", discount.getId());
            Bson update = Updates.combine(
                    Updates.set("code", discount.getCode().toUpperCase()),
                    Updates.set("description", discount.getDescription() != null ? discount.getDescription() : ""),
                    Updates.set("type", discount.getType()),
                    Updates.set("value", discount.getValue()),
                    Updates.set("min_purchase", discount.getMinPurchase()),
                    Updates.set("start_date", discount.getStartDate() != null ? discount.getStartDate().toString() : null),
                    Updates.set("end_date", discount.getEndDate() != null ? discount.getEndDate().toString() : null),
                    Updates.set("active", discount.isActive()));
            discounts.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            discounts.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Discount mapRow(Document doc) {
        Discount d = new Discount();
        d.setId(doc.getInteger("_id"));
        d.setCode(doc.getString("code"));
        d.setDescription(doc.getString("description"));
        d.setType(doc.getString("type"));
        d.setValue(doc.getDouble("value"));
        d.setMinPurchase(doc.getDouble("min_purchase"));
        String start = doc.getString("start_date");
        if (start != null) d.setStartDate(LocalDate.parse(start));
        String end = doc.getString("end_date");
        if (end != null) d.setEndDate(LocalDate.parse(end));
        d.setActive(Boolean.TRUE.equals(doc.getBoolean("active")));
        return d;
    }
}
