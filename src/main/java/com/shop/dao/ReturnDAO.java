package com.shop.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Return;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.util.SessionManager;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> returns = db.getCollection("returns");
    private final MongoCollection<Document> sales = db.getCollection("sales");
    private final MongoCollection<Document> products = db.getCollection("products");

    public boolean create(Return ret, Sale sale) {
        MongoClient client = db.getClient();
        try (ClientSession session = client.startSession()) {
            session.startTransaction();
            try {
                int id = db.nextId("returns");
                ret.setId(id);

                List<Document> itemDocs = new ArrayList<>();
                for (SaleItem item : ret.getItems()) {
                    products.updateOne(session,
                            Filters.eq("_id", item.getProductId()),
                            Updates.inc("quantity", item.getQuantity()));
                    itemDocs.add(new Document("product_id", item.getProductId())
                            .append("product_name", item.getProductName())
                            .append("quantity", item.getQuantity())
                            .append("unit_price", item.getUnitPrice())
                            .append("refund_amount", item.getTotal()));
                }

                int userId = SessionManager.getInstance().getCurrentUser() != null
                        ? SessionManager.getInstance().getCurrentUser().getId() : 0;

                Document doc = new Document("_id", id)
                        .append("sale_id", ret.getSaleId())
                        .append("invoice_number", ret.getInvoiceNumber())
                        .append("customer_id", ret.getCustomerId())
                        .append("customer_name", ret.getCustomerName())
                        .append("refund_amount", ret.getRefundAmount())
                        .append("reason", ret.getReason())
                        .append("status", ret.getStatus())
                        .append("user_id", userId)
                        .append("created_at", LocalDateTime.now().toString())
                        .append("items", itemDocs);
                returns.insertOne(session, doc);

                sales.updateOne(session, Filters.eq("_id", sale.getId()), Updates.set("has_return", true));

                session.commitTransaction();
                new ActivityLogDAO().log("REFUND", "Refund ₹" + String.format("%.2f", ret.getRefundAmount())
                        + " on " + ret.getInvoiceNumber() + " (" + ret.getItems().size() + " items restocked)");
                return true;
            } catch (Exception e) {
                session.abortTransaction();
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Return> findAll() {
        List<Return> list = new ArrayList<>();
        for (Document doc : returns.find().sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Return> findByInvoiceNumber(String invoiceNumber) {
        List<Return> list = new ArrayList<>();
        for (Document doc : returns.find(Filters.eq("invoice_number", invoiceNumber))
                .sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public int getReturnedQuantity(int saleId, int productId) {
        int total = 0;
        for (Document doc : returns.find(Filters.eq("sale_id", saleId))) {
            Object raw = doc.get("items");
            if (!(raw instanceof List<?> list)) continue;
            for (Object o : list) {
                if (!(o instanceof Document itemDoc)) continue;
                if (itemDoc.getInteger("product_id") != null
                        && itemDoc.getInteger("product_id") == productId) {
                    total += itemDoc.getInteger("quantity") != null ? itemDoc.getInteger("quantity") : 0;
                }
            }
        }
        return total;
    }

    public double getTotalRefundsToday() {
        return sumRefunds(Filters.regex("created_at", "^" + LocalDate.now()));
    }

    public double getTotalRefundsThisMonth() {
        String month = LocalDate.now().toString().substring(0, 7);
        return sumRefunds(Filters.regex("created_at", "^" + month));
    }

    public int count() {
        return (int) returns.countDocuments();
    }

    private double sumRefunds(org.bson.conversions.Bson filter) {
        double total = 0;
        for (Document doc : returns.find(filter)) {
            total += doc.getDouble("refund_amount") != null ? doc.getDouble("refund_amount") : 0;
        }
        return total;
    }

    private Return mapRow(Document doc) {
        Return r = new Return();
        r.setId(doc.getInteger("_id"));
        r.setSaleId(doc.getInteger("sale_id"));
        r.setInvoiceNumber(doc.getString("invoice_number"));
        r.setCustomerId(doc.getInteger("customer_id") != null ? doc.getInteger("customer_id") : 0);
        r.setCustomerName(doc.getString("customer_name"));
        r.setRefundAmount(doc.getDouble("refund_amount") != null ? doc.getDouble("refund_amount") : 0);
        r.setReason(doc.getString("reason"));
        r.setStatus(doc.getString("status"));
        r.setUserId(doc.getInteger("user_id") != null ? doc.getInteger("user_id") : 0);
        String created = doc.getString("created_at");
        if (created != null) {
            try { r.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    SaleItem item = new SaleItem();
                    item.setProductId(itemDoc.getInteger("product_id"));
                    item.setProductName(itemDoc.getString("product_name"));
                    item.setQuantity(itemDoc.getInteger("quantity"));
                    item.setUnitPrice(itemDoc.getDouble("unit_price") != null ? itemDoc.getDouble("unit_price") : 0);
                    item.setTotal(itemDoc.getDouble("refund_amount") != null ? itemDoc.getDouble("refund_amount") : 0);
                    r.getItems().add(item);
                }
            }
        }
        return r;
    }
}
