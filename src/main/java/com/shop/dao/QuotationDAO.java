package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Quotation;
import com.shop.model.QuotationItem;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuotationDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> quotations = db.getCollection("quotations");

    public boolean create(Quotation q) {
        try {
            q.computeTotals();
            int id = db.nextId("quotations");
            q.setId(id);
            if (q.getQuoteNumber() == null || q.getQuoteNumber().isEmpty()) {
                q.setQuoteNumber(generateQuoteNumber());
            }
            q.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("quote_number", q.getQuoteNumber())
                    .append("customer_id", q.getCustomerId())
                    .append("customer_name", q.getCustomerName() != null ? q.getCustomerName() : "")
                    .append("customer_phone", q.getCustomerPhone() != null ? q.getCustomerPhone() : "")
                    .append("subtotal", q.getSubtotal())
                    .append("discount_amount", q.getDiscountAmount())
                    .append("tax", q.getTax())
                    .append("total", q.getTotal())
                    .append("status", q.getStatus())
                    .append("valid_until", q.getValidUntil() != null ? q.getValidUntil().toString() : null)
                    .append("note", q.getNote() != null ? q.getNote() : "")
                    .append("created_at", q.getCreatedAt().toString())
                    .append("items", toItemDocs(q.getItems()));
            quotations.insertOne(doc);
            new ActivityLogDAO().log("QUOTATION", "Created quotation " + q.getQuoteNumber()
                    + " for " + (q.getCustomerName() != null ? q.getCustomerName() : "walk-in")
                    + " worth " + q.getFormattedTotal());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int id, String status) {
        Bson filter = Filters.eq("_id", id);
        var result = quotations.updateOne(filter, Updates.set("status", status));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("QUOTATION", "Quotation #" + id + " marked as " + status);
            return true;
        }
        return false;
    }

    public void markExpired() {
        String today = LocalDate.now().toString();
        quotations.updateMany(
                Filters.and(
                        Filters.in("status", Quotation.STATUS_PENDING, Quotation.STATUS_DRAFT),
                        Filters.lte("valid_until", today)),
                Updates.set("status", Quotation.STATUS_EXPIRED));
    }

    public List<Quotation> findAll() {
        markExpired();
        List<Document> docs = new ArrayList<>();
        for (Document doc : quotations.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public Quotation findById(int id) {
        Document doc = quotations.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc, true) : null;
    }

    public int countPending() {
        markExpired();
        return (int) quotations.countDocuments(Filters.in("status", Quotation.STATUS_PENDING, Quotation.STATUS_DRAFT));
    }

    public int count() {
        return (int) quotations.countDocuments();
    }

    public double totalPendingValue() {
        markExpired();
        double total = 0;
        for (Document doc : quotations.find(Filters.eq("status", Quotation.STATUS_PENDING))) {
            total += doc.getDouble("total") != null ? doc.getDouble("total") : 0;
        }
        return total;
    }

    private List<Quotation> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        List<Quotation> list = new ArrayList<>();
        for (Document doc : docs) {
            Quotation q = mapRow(doc, false);
            Integer cid = doc.getInteger("customer_id");
            if (cid != null && cid > 0) {
                String name = customerNames.get(cid);
                if (name != null) q.setCustomerName(name);
            }
            list.add(q);
        }
        return list;
    }

    private Quotation mapRow(Document doc, boolean withItems) {
        Quotation q = new Quotation();
        q.setId(doc.getInteger("_id"));
        q.setQuoteNumber(doc.getString("quote_number"));
        Integer cid = doc.getInteger("customer_id");
        q.setCustomerId(cid != null ? cid : 0);
        q.setCustomerName(doc.getString("customer_name"));
        q.setCustomerPhone(doc.getString("customer_phone"));
        q.setSubtotal(doc.getDouble("subtotal") != null ? doc.getDouble("subtotal") : 0);
        q.setDiscountAmount(doc.getDouble("discount_amount") != null ? doc.getDouble("discount_amount") : 0);
        q.setTax(doc.getDouble("tax") != null ? doc.getDouble("tax") : 0);
        q.setTotal(doc.getDouble("total") != null ? doc.getDouble("total") : 0);
        q.setStatus(doc.getString("status") != null ? doc.getString("status") : Quotation.STATUS_PENDING);
        String valid = doc.getString("valid_until");
        if (valid != null) {
            try { q.setValidUntil(LocalDate.parse(valid)); } catch (Exception ignored) {}
        }
        q.setNote(doc.getString("note"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { q.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        if (withItems) {
            q.setItems(mapItems(doc));
        }
        return q;
    }

    private List<QuotationItem> mapItems(Document doc) {
        List<QuotationItem> items = new ArrayList<>();
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    items.add(new QuotationItem(
                            itemDoc.getInteger("product_id"),
                            itemDoc.getString("product_name"),
                            itemDoc.getString("barcode"),
                            itemDoc.getInteger("quantity"),
                            itemDoc.getDouble("unit_price")));
                }
            }
        }
        return items;
    }

    private List<Document> toItemDocs(List<QuotationItem> items) {
        List<Document> docs = new ArrayList<>();
        for (QuotationItem item : items) {
            docs.add(new Document("product_id", item.getProductId())
                    .append("product_name", item.getProductName())
                    .append("barcode", item.getBarcode())
                    .append("quantity", item.getQuantity())
                    .append("unit_price", item.getUnitPrice()));
        }
        return docs;
    }

    private Map<Integer, String> loadCustomerNames(List<Document> docs) {
        Map<Integer, String> names = new HashMap<>();
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Document doc : docs) {
            Integer cid = doc.getInteger("customer_id");
            if (cid != null && cid > 0) ids.add(cid);
        }
        if (ids.isEmpty()) return names;
        for (Document c : db.getCollection("customers").find(Filters.in("_id", ids))) {
            names.put(c.getInteger("_id"), c.getString("name"));
        }
        return names;
    }

    private String generateQuoteNumber() {
        long count = quotations.countDocuments();
        return String.format("QT-%06d", count + 1);
    }
}
