package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Customer;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CustomerDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> customers = db.getCollection("customers");

    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        for (Document doc : customers.find().sort(new Document("name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Customer> search(String query) {
        Pattern pattern = Pattern.compile(".*" + Pattern.quote(query.toLowerCase()) + ".*", Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("name", pattern),
                Filters.regex("phone", pattern),
                Filters.regex("email", pattern));
        List<Customer> list = new ArrayList<>();
        for (Document doc : customers.find(filter).sort(new Document("name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public Customer findById(int id) {
        Document doc = customers.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc) : null;
    }

    public boolean insert(Customer customer) {
        try {
            int id = db.nextId("customers");
            customer.setId(id);
            Document doc = new Document("_id", id)
                    .append("name", customer.getName())
                    .append("phone", customer.getPhone() != null ? customer.getPhone() : "")
                    .append("email", customer.getEmail() != null ? customer.getEmail() : "")
                    .append("address", customer.getAddress() != null ? customer.getAddress() : "")
                    .append("loyalty_points", customer.getLoyaltyPoints())
                    .append("created_at", customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null);
            customers.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Customer customer) {
        try {
            Bson filter = Filters.eq("_id", customer.getId());
            Bson update = Updates.combine(
                    Updates.set("name", customer.getName()),
                    Updates.set("phone", customer.getPhone() != null ? customer.getPhone() : ""),
                    Updates.set("email", customer.getEmail() != null ? customer.getEmail() : ""),
                    Updates.set("address", customer.getAddress() != null ? customer.getAddress() : ""),
                    Updates.set("loyalty_points", customer.getLoyaltyPoints()));
            customers.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            customers.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int count() {
        return (int) customers.countDocuments();
    }

    public boolean addLoyaltyPoints(int customerId, int points) {
        try {
            customers.updateOne(Filters.eq("_id", customerId), Updates.inc("loyalty_points", points));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Customer mapRow(Document doc) {
        Customer c = new Customer();
        c.setId(doc.getInteger("_id"));
        c.setName(doc.getString("name"));
        c.setPhone(doc.getString("phone"));
        c.setEmail(doc.getString("email"));
        c.setAddress(doc.getString("address"));
        c.setLoyaltyPoints(doc.getInteger("loyalty_points") != null ? doc.getInteger("loyalty_points") : 0);
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            c.setCreatedAt(java.time.LocalDateTime.parse(createdAt));
        }
        return c;
    }
}
