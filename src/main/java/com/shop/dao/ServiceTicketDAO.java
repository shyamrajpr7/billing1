package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.ServiceTicket;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceTicketDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> tickets = db.getCollection("service_tickets");

    public boolean create(ServiceTicket t) {
        try {
            int id = db.nextId("service_tickets");
            t.setId(id);
            if (t.getTicketNumber() == null || t.getTicketNumber().isEmpty()) {
                t.setTicketNumber(generateNumber());
            }
            t.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("ticket_number", t.getTicketNumber())
                    .append("customer_id", t.getCustomerId())
                    .append("customer_name", t.getCustomerName() != null ? t.getCustomerName() : "")
                    .append("customer_phone", t.getCustomerPhone() != null ? t.getCustomerPhone() : "")
                    .append("subject", t.getSubject())
                    .append("description", t.getDescription() != null ? t.getDescription() : "")
                    .append("status", t.getStatus())
                    .append("priority", t.getPriority())
                    .append("created_at", t.getCreatedAt().toString())
                    .append("resolved_at", null);
            tickets.insertOne(doc);
            new ActivityLogDAO().log("TICKET", "Opened service ticket " + t.getTicketNumber()
                    + " for " + (t.getCustomerName() != null ? t.getCustomerName() : "customer"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int id, String status) {
        Bson update;
        if (ServiceTicket.STATUS_RESOLVED.equals(status) || ServiceTicket.STATUS_CLOSED.equals(status)) {
            update = Updates.combine(
                    Updates.set("status", status),
                    Updates.set("resolved_at", LocalDateTime.now().toString()));
        } else {
            update = Updates.set("status", status);
        }
        var result = tickets.updateOne(Filters.eq("_id", id), update);
        if (result.getMatchedCount() > 0) {
            new ActivityLogDAO().log("TICKET", "Service ticket #" + id + " set to " + status);
            return true;
        }
        return false;
    }

    public List<ServiceTicket> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : tickets.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public List<ServiceTicket> findOpen() {
        List<ServiceTicket> list = new ArrayList<>();
        for (ServiceTicket t : findAll()) {
            if (t.isOpen()) list.add(t);
        }
        return list;
    }

    public int countOpen() {
        return (int) tickets.countDocuments(
                Filters.in("status", ServiceTicket.STATUS_OPEN, ServiceTicket.STATUS_IN_PROGRESS));
    }

    public int countResolved() {
        return (int) tickets.countDocuments(
                Filters.in("status", ServiceTicket.STATUS_RESOLVED, ServiceTicket.STATUS_CLOSED));
    }

    public int countHighPriority() {
        return (int) tickets.countDocuments(Filters.and(
                Filters.in("status", ServiceTicket.STATUS_OPEN, ServiceTicket.STATUS_IN_PROGRESS),
                Filters.eq("priority", ServiceTicket.PRIORITY_HIGH)));
    }

    public int count() {
        return (int) tickets.countDocuments();
    }

    private List<ServiceTicket> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = new HashMap<>();
        for (Document doc : docs) {
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if (cid != null && (name == null || name.isEmpty())) {
                customerNames.putIfAbsent(cid, loadCustomerName(cid));
            }
        }
        List<ServiceTicket> list = new ArrayList<>();
        for (Document doc : docs) {
            ServiceTicket t = mapRow(doc);
            Integer cid = doc.getInteger("customer_id");
            String name = doc.getString("customer_name");
            if ((name == null || name.isEmpty()) && cid != null && customerNames.containsKey(cid)) {
                t.setCustomerName(customerNames.get(cid));
            }
            list.add(t);
        }
        return list;
    }

    private String loadCustomerName(int customerId) {
        Document c = db.getCollection("customers").find(Filters.eq("_id", customerId)).first();
        return c != null ? c.getString("name") : "";
    }

    private ServiceTicket mapRow(Document doc) {
        ServiceTicket t = new ServiceTicket();
        t.setId(doc.getInteger("_id"));
        t.setTicketNumber(doc.getString("ticket_number"));
        Integer cid = doc.getInteger("customer_id");
        t.setCustomerId(cid != null ? cid : 0);
        t.setCustomerName(doc.getString("customer_name"));
        t.setCustomerPhone(doc.getString("customer_phone"));
        t.setSubject(doc.getString("subject"));
        t.setDescription(doc.getString("description"));
        t.setStatus(doc.getString("status") != null ? doc.getString("status") : ServiceTicket.STATUS_OPEN);
        t.setPriority(doc.getString("priority") != null ? doc.getString("priority") : ServiceTicket.PRIORITY_NORMAL);
        String created = doc.getString("created_at");
        if (created != null) {
            try { t.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        String resolved = doc.getString("resolved_at");
        if (resolved != null) {
            try { t.setResolvedAt(LocalDateTime.parse(resolved)); } catch (Exception ignored) {}
        }
        return t;
    }

    private String generateNumber() {
        long count = tickets.countDocuments();
        return String.format("TK-%06d", count + 1);
    }
}
