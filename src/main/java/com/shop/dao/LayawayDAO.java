package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Layaway;
import com.shop.model.LayawayItem;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayawayDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> layaways = db.getCollection("layaways");

    public boolean create(Layaway lay) {
        try {
            lay.computeTotals();
            int id = db.nextId("layaways");
            lay.setId(id);
            if (lay.getPlanNumber() == null || lay.getPlanNumber().isEmpty()) {
                lay.setPlanNumber(generatePlanNumber());
            }
            lay.setCreatedAt(LocalDateTime.now());
            if (lay.getStartDate() == null) lay.setStartDate(LocalDate.now());
            Document doc = new Document("_id", id)
                    .append("plan_number", lay.getPlanNumber())
                    .append("customer_id", lay.getCustomerId())
                    .append("customer_name", lay.getCustomerName() != null ? lay.getCustomerName() : "")
                    .append("customer_phone", lay.getCustomerPhone() != null ? lay.getCustomerPhone() : "")
                    .append("total_amount", lay.getTotalAmount())
                    .append("down_payment", lay.getDownPayment())
                    .append("amount_paid", lay.getAmountPaid())
                    .append("installments_count", lay.getInstallmentsCount())
                    .append("installments_paid", lay.getInstallmentsPaid())
                    .append("installment_amount", lay.getInstallmentAmount())
                    .append("status", lay.getStatus())
                    .append("start_date", lay.getStartDate().toString())
                    .append("due_date", lay.getDueDate() != null ? lay.getDueDate().toString() : null)
                    .append("note", lay.getNote() != null ? lay.getNote() : "")
                    .append("created_at", lay.getCreatedAt().toString())
                    .append("items", toItemDocs(lay.getItems()));
            layaways.insertOne(doc);
            new ActivityLogDAO().log("LAWAY", "Created layaway plan " + lay.getPlanNumber()
                    + " for " + (lay.getCustomerName() != null ? lay.getCustomerName() : "customer")
                    + " worth " + String.format("₹%.2f", lay.getTotalAmount()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean recordPayment(int id, double amount) {
        if (amount <= 0) return false;
        Layaway lay = findById(id);
        if (lay == null || !lay.isActive()) return false;

        Bson filter = Filters.eq("_id", id);
        layaways.updateOne(filter, Updates.combine(
                Updates.inc("amount_paid", amount),
                Updates.inc("installments_paid", 1)));

        Layaway updated = findById(id);
        if (updated.isPaidOff()) {
            layaways.updateOne(filter, Updates.set("status", Layaway.STATUS_COMPLETED));
        }
        new ActivityLogDAO().log("LAWAY", "Received " + String.format("₹%.2f", amount)
                + " installment for layaway #" + id);
        return true;
    }

    public boolean cancel(int id) {
        var result = layaways.updateOne(Filters.eq("_id", id), Updates.set("status", Layaway.STATUS_CANCELLED));
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("LAWAY", "Cancelled layaway #" + id);
            return true;
        }
        return false;
    }

    public List<Layaway> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : layaways.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public Layaway findById(int id) {
        Document doc = layaways.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc, true) : null;
    }

    public int countActive() {
        return (int) layaways.countDocuments(Filters.eq("status", Layaway.STATUS_ACTIVE));
    }

    public int count() {
        return (int) layaways.countDocuments();
    }

    public double totalOutstanding() {
        double total = 0;
        for (Document doc : layaways.find(Filters.eq("status", Layaway.STATUS_ACTIVE))) {
            double totalAmount = doc.getDouble("total_amount") != null ? doc.getDouble("total_amount") : 0;
            double amountPaid = doc.getDouble("amount_paid") != null ? doc.getDouble("amount_paid") : 0;
            total += Math.max(0, totalAmount - amountPaid);
        }
        return total;
    }

    private List<Layaway> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        List<Layaway> list = new ArrayList<>();
        for (Document doc : docs) {
            Layaway lay = mapRow(doc, false);
            Integer cid = doc.getInteger("customer_id");
            if (cid != null && cid > 0) {
                String name = customerNames.get(cid);
                if (name != null) lay.setCustomerName(name);
            }
            list.add(lay);
        }
        return list;
    }

    private Layaway mapRow(Document doc, boolean withItems) {
        Layaway lay = new Layaway();
        lay.setId(doc.getInteger("_id"));
        lay.setPlanNumber(doc.getString("plan_number"));
        Integer cid = doc.getInteger("customer_id");
        lay.setCustomerId(cid != null ? cid : 0);
        lay.setCustomerName(doc.getString("customer_name"));
        lay.setCustomerPhone(doc.getString("customer_phone"));
        lay.setTotalAmount(doc.getDouble("total_amount") != null ? doc.getDouble("total_amount") : 0);
        lay.setDownPayment(doc.getDouble("down_payment") != null ? doc.getDouble("down_payment") : 0);
        lay.setAmountPaid(doc.getDouble("amount_paid") != null ? doc.getDouble("amount_paid") : 0);
        lay.setInstallmentsCount(doc.getInteger("installments_count") != null ? doc.getInteger("installments_count") : 1);
        lay.setInstallmentsPaid(doc.getInteger("installments_paid") != null ? doc.getInteger("installments_paid") : 0);
        lay.setInstallmentAmount(doc.getDouble("installment_amount") != null ? doc.getDouble("installment_amount") : 0);
        lay.setStatus(doc.getString("status") != null ? doc.getString("status") : Layaway.STATUS_ACTIVE);
        String start = doc.getString("start_date");
        if (start != null) {
            try { lay.setStartDate(LocalDate.parse(start)); } catch (Exception ignored) {}
        }
        String due = doc.getString("due_date");
        if (due != null) {
            try { lay.setDueDate(LocalDate.parse(due)); } catch (Exception ignored) {}
        }
        lay.setNote(doc.getString("note"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { lay.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        if (withItems) {
            lay.setItems(mapItems(doc));
        }
        return lay;
    }

    private List<LayawayItem> mapItems(Document doc) {
        List<LayawayItem> items = new ArrayList<>();
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    items.add(new LayawayItem(
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

    private List<Document> toItemDocs(List<LayawayItem> items) {
        List<Document> docs = new ArrayList<>();
        for (LayawayItem item : items) {
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

    private String generatePlanNumber() {
        long count = layaways.countDocuments();
        return String.format("LW-%06d", count + 1);
    }
}
