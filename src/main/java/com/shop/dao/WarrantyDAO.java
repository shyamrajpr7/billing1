package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Warranty;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WarrantyDAO {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> warranties = db.getCollection("warranties");

    public boolean create(Warranty w) {
        try {
            w.computeExpiryDate();
            int id = db.nextId("warranties");
            w.setId(id);
            w.setWarrantyNumber(generateNumber());
            Document doc = new Document("_id", id)
                    .append("warranty_number", w.getWarrantyNumber())
                    .append("product_id", w.getProductId())
                    .append("product_name", w.getProductName())
                    .append("barcode", w.getBarcode())
                    .append("customer_id", w.getCustomerId())
                    .append("customer_name", w.getCustomerName() != null ? w.getCustomerName() : "")
                    .append("customer_phone", w.getCustomerPhone() != null ? w.getCustomerPhone() : "")
                    .append("purchase_date", w.getPurchaseDate() != null ? w.getPurchaseDate().toString() : null)
                    .append("warranty_months", w.getWarrantyMonths())
                    .append("expiry_date", w.getExpiryDate().toString())
                    .append("purchase_invoice_number", w.getPurchaseInvoiceNumber() != null ? w.getPurchaseInvoiceNumber() : "")
                    .append("notes", w.getNotes() != null ? w.getNotes() : "")
                    .append("status", w.getStatus());
            warranties.insertOne(doc);
            new ActivityLogDAO().log("WARRANTY", "Registered warranty " + w.getWarrantyNumber()
                    + " for " + w.getProductName()
                    + (w.getCustomerName() != null && !w.getCustomerName().isEmpty() ? " for " + w.getCustomerName() : ""));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markClaimed(int id) {
        var result = warranties.updateOne(Filters.eq("_id", id), Updates.set("status", Warranty.STATUS_CLAIMED));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("WARRANTY", "Marked warranty #" + id + " as claimed");
            return true;
        }
        return false;
    }

    public void markExpired() {
        String today = LocalDate.now().toString();
        warranties.updateMany(
                Filters.and(Filters.eq("status", Warranty.STATUS_ACTIVE), Filters.lt("expiry_date", today)),
                Updates.set("status", Warranty.STATUS_EXPIRED));
    }

    public List<Warranty> findAll() {
        markExpired();
        List<Warranty> list = new ArrayList<>();
        for (Document doc : warranties.find().sort(new Document("expiry_date", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int countActive() {
        markExpired();
        return (int) warranties.countDocuments(Filters.eq("status", Warranty.STATUS_ACTIVE));
    }

    public int countExpiringSoon(int days) {
        markExpired();
        String today = LocalDate.now().toString();
        String limit = LocalDate.now().plusDays(days).toString();
        return (int) warranties.countDocuments(Filters.and(
                Filters.eq("status", Warranty.STATUS_ACTIVE),
                Filters.gte("expiry_date", today),
                Filters.lte("expiry_date", limit)));
    }

    public int count() {
        return (int) warranties.countDocuments();
    }

    public String generateNumber() {
        StringBuilder sb = new StringBuilder("WR-");
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private Warranty mapRow(Document doc) {
        Warranty w = new Warranty();
        w.setId(doc.getInteger("_id"));
        w.setWarrantyNumber(doc.getString("warranty_number"));
        w.setProductId(doc.getInteger("product_id") != null ? doc.getInteger("product_id") : 0);
        w.setProductName(doc.getString("product_name"));
        w.setBarcode(doc.getString("barcode"));
        Integer cid = doc.getInteger("customer_id");
        w.setCustomerId(cid != null ? cid : 0);
        w.setCustomerName(doc.getString("customer_name"));
        w.setCustomerPhone(doc.getString("customer_phone"));
        String purchase = doc.getString("purchase_date");
        if (purchase != null) {
            try { w.setPurchaseDate(LocalDate.parse(purchase)); } catch (Exception ignored) {}
        }
        w.setWarrantyMonths(doc.getInteger("warranty_months") != null ? doc.getInteger("warranty_months") : 12);
        String expiry = doc.getString("expiry_date");
        if (expiry != null) {
            try { w.setExpiryDate(LocalDate.parse(expiry)); } catch (Exception ignored) {}
        }
        w.setPurchaseInvoiceNumber(doc.getString("purchase_invoice_number"));
        w.setNotes(doc.getString("notes"));
        w.setStatus(doc.getString("status") != null ? doc.getString("status") : Warranty.STATUS_ACTIVE);
        return w;
    }
}
