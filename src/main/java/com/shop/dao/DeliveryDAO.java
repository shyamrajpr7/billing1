package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Delivery;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> deliveries = db.getCollection("deliveries");

    public boolean create(Delivery d) {
        try {
            int id = db.nextId("deliveries");
            d.setId(id);
            if (d.getDeliveryNumber() == null || d.getDeliveryNumber().isEmpty()) {
                d.setDeliveryNumber(generateNumber());
            }
            d.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("delivery_number", d.getDeliveryNumber())
                    .append("customer_id", d.getCustomerId())
                    .append("customer_name", d.getCustomerName() != null ? d.getCustomerName() : "")
                    .append("customer_phone", d.getCustomerPhone() != null ? d.getCustomerPhone() : "")
                    .append("address", d.getAddress() != null ? d.getAddress() : "")
                    .append("sale_invoice_number", d.getSaleInvoiceNumber() != null ? d.getSaleInvoiceNumber() : "")
                    .append("items_description", d.getItemsDescription() != null ? d.getItemsDescription() : "")
                    .append("status", d.getStatus())
                    .append("scheduled_date", d.getScheduledDate() != null ? d.getScheduledDate().toString() : null)
                    .append("delivered_date", null)
                    .append("courier", d.getCourier() != null ? d.getCourier() : "")
                    .append("tracking_number", d.getTrackingNumber() != null ? d.getTrackingNumber() : "")
                    .append("note", d.getNote() != null ? d.getNote() : "")
                    .append("created_at", d.getCreatedAt().toString());
            deliveries.insertOne(doc);
            new ActivityLogDAO().log("DELIVERY", "Created delivery " + d.getDeliveryNumber()
                    + " for " + (d.getCustomerName() != null ? d.getCustomerName() : "customer"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int id, String status) {
        Bson filter = Filters.eq("_id", id);
        Bson update;
        if (Delivery.STATUS_DELIVERED.equals(status)) {
            update = Updates.combine(
                    Updates.set("status", status),
                    Updates.set("delivered_date", LocalDate.now().toString()));
        } else {
            update = Updates.set("status", status);
        }
        var result = deliveries.updateOne(filter, update);
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("DELIVERY", "Delivery #" + id + " marked as " + status);
            return true;
        }
        return false;
    }

    public List<Delivery> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : deliveries.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public int countActive() {
        return (int) deliveries.countDocuments(
                Filters.in("status", Delivery.STATUS_PENDING, Delivery.STATUS_OUT_FOR_DELIVERY));
    }

    public int countDelivered() {
        return (int) deliveries.countDocuments(Filters.eq("status", Delivery.STATUS_DELIVERED));
    }

    public int countDueToday() {
        String today = LocalDate.now().toString();
        return (int) deliveries.countDocuments(Filters.and(
                Filters.in("status", Delivery.STATUS_PENDING, Delivery.STATUS_OUT_FOR_DELIVERY),
                Filters.eq("scheduled_date", today)));
    }

    public int count() {
        return (int) deliveries.countDocuments();
    }

    private List<Delivery> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = new HashMap<>();
        for (Document doc : docs) {
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if (cid != null && (name == null || name.isEmpty())) {
                customerNames.putIfAbsent(cid, loadCustomerName(cid));
            }
        }
        List<Delivery> list = new ArrayList<>();
        for (Document doc : docs) {
            Delivery d = mapRow(doc);
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if ((name == null || name.isEmpty()) && cid != null && customerNames.containsKey(cid)) {
                d.setCustomerName(customerNames.get(cid));
            }
            list.add(d);
        }
        return list;
    }

    private String loadCustomerName(int customerId) {
        Document c = db.getCollection("customers").find(Filters.eq("_id", customerId)).first();
        return c != null ? c.getString("name") : "";
    }

    private Delivery mapRow(Document doc) {
        Delivery d = new Delivery();
        d.setId(doc.getInteger("_id"));
        d.setDeliveryNumber(doc.getString("delivery_number"));
        Integer cid = doc.getInteger("customer_id");
        d.setCustomerId(cid != null ? cid : 0);
        d.setCustomerName(doc.getString("customer_name"));
        d.setCustomerPhone(doc.getString("customer_phone"));
        d.setAddress(doc.getString("address"));
        d.setSaleInvoiceNumber(doc.getString("sale_invoice_number"));
        d.setItemsDescription(doc.getString("items_description"));
        d.setStatus(doc.getString("status") != null ? doc.getString("status") : Delivery.STATUS_PENDING);
        String scheduled = doc.getString("scheduled_date");
        if (scheduled != null) {
            try { d.setScheduledDate(LocalDate.parse(scheduled)); } catch (Exception ignored) {}
        }
        String delivered = doc.getString("delivered_date");
        if (delivered != null) {
            try { d.setDeliveredDate(LocalDate.parse(delivered)); } catch (Exception ignored) {}
        }
        d.setCourier(doc.getString("courier"));
        d.setTrackingNumber(doc.getString("tracking_number"));
        d.setNote(doc.getString("note"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { d.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        return d;
    }

    private String generateNumber() {
        long count = deliveries.countDocuments();
        return String.format("DLV-%06d", count + 1);
    }
}
