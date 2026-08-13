package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.SupplierPayment;
import org.bson.Document;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupplierPaymentDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> payments = db.getCollection("supplier_payments");

    public boolean create(SupplierPayment sp) {
        try {
            int id = db.nextId("supplier_payments");
            sp.setId(id);
            if (sp.getPaymentNumber() == null || sp.getPaymentNumber().isEmpty()) {
                sp.setPaymentNumber(generateNumber());
            }
            if (sp.getPaymentDate() == null) sp.setPaymentDate(LocalDate.now());
            Document doc = new Document("_id", id)
                    .append("payment_number", sp.getPaymentNumber())
                    .append("supplier_id", sp.getSupplierId())
                    .append("supplier_name", sp.getSupplierName() != null ? sp.getSupplierName() : "")
                    .append("amount", sp.getAmount())
                    .append("payment_method", sp.getPaymentMethod() != null ? sp.getPaymentMethod() : "Cash")
                    .append("payment_date", sp.getPaymentDate().toString())
                    .append("reference", sp.getReference() != null ? sp.getReference() : "")
                    .append("note", sp.getNote() != null ? sp.getNote() : "");
            payments.insertOne(doc);
            new ActivityLogDAO().log("SUPPLIER_PAYMENT", "Paid " + String.format("₹%.2f", sp.getAmount())
                    + " to " + (sp.getSupplierName() != null ? sp.getSupplierName() : "supplier")
                    + " (" + sp.getPaymentNumber() + ")");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<SupplierPayment> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : payments.find().sort(new Document("payment_date", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public List<SupplierPayment> findBySupplier(int supplierId) {
        List<Document> docs = new ArrayList<>();
        for (Document doc : payments.find(Filters.eq("supplier_id", supplierId))
                .sort(new Document("payment_date", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public double totalPaid() {
        double total = 0;
        for (Document doc : payments.find()) {
            total += doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
        }
        return total;
    }

    public double totalPaidForSupplier(int supplierId) {
        double total = 0;
        for (Document doc : payments.find(Filters.eq("supplier_id", supplierId))) {
            total += doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
        }
        return total;
    }

    public double totalPaidThisMonth() {
        String month = YearMonth.now().toString();
        double total = 0;
        for (Document doc : payments.find()) {
            String date = doc.getString("payment_date");
            if (date != null && date.startsWith(month)) {
                total += doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
            }
        }
        return total;
    }

    public Map<Integer, Double> totalsBySupplier() {
        Map<Integer, Double> totals = new HashMap<>();
        for (Document doc : payments.find()) {
            Integer sid = doc.getInteger("supplier_id");
            if (sid != null) {
                totals.merge(sid, doc.getDouble("amount") != null ? doc.getDouble("amount") : 0, Double::sum);
            }
        }
        return totals;
    }

    public int count() {
        return (int) payments.countDocuments();
    }

    private List<SupplierPayment> mapRows(List<Document> docs) {
        List<SupplierPayment> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapRow(doc));
        }
        return list;
    }

    private SupplierPayment mapRow(Document doc) {
        SupplierPayment sp = new SupplierPayment();
        sp.setId(doc.getInteger("_id"));
        sp.setPaymentNumber(doc.getString("payment_number"));
        Integer sid = doc.getInteger("supplier_id");
        sp.setSupplierId(sid != null ? sid : 0);
        sp.setSupplierName(doc.getString("supplier_name"));
        sp.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
        sp.setPaymentMethod(doc.getString("payment_method"));
        String date = doc.getString("payment_date");
        if (date != null) {
            try { sp.setPaymentDate(LocalDate.parse(date)); } catch (Exception ignored) {}
        }
        sp.setReference(doc.getString("reference"));
        sp.setNote(doc.getString("note"));
        return sp;
    }

    private String generateNumber() {
        long count = payments.countDocuments();
        return String.format("PAY-%06d", count + 1);
    }
}
