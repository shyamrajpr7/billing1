package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Supplier;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SupplierDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> suppliers = db.getCollection("suppliers");

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        for (Document doc : suppliers.find().sort(new Document("company_name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Supplier> search(String query) {
        Pattern pattern = Pattern.compile(".*" + Pattern.quote(query.toLowerCase()) + ".*", Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("company_name", pattern),
                Filters.regex("contact_person", pattern),
                Filters.regex("phone", pattern));
        List<Supplier> list = new ArrayList<>();
        for (Document doc : suppliers.find(filter).sort(new Document("company_name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public Supplier findById(int id) {
        Document doc = suppliers.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc) : null;
    }

    public boolean insert(Supplier supplier) {
        try {
            int id = db.nextId("suppliers");
            supplier.setId(id);
            Document doc = new Document("_id", id)
                    .append("company_name", supplier.getCompanyName())
                    .append("contact_person", supplier.getContactPerson() != null ? supplier.getContactPerson() : "")
                    .append("phone", supplier.getPhone() != null ? supplier.getPhone() : "")
                    .append("email", supplier.getEmail() != null ? supplier.getEmail() : "")
                    .append("address", supplier.getAddress() != null ? supplier.getAddress() : "");
            suppliers.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Supplier supplier) {
        try {
            Bson filter = Filters.eq("_id", supplier.getId());
            Bson update = Updates.combine(
                    Updates.set("company_name", supplier.getCompanyName()),
                    Updates.set("contact_person", supplier.getContactPerson() != null ? supplier.getContactPerson() : ""),
                    Updates.set("phone", supplier.getPhone() != null ? supplier.getPhone() : ""),
                    Updates.set("email", supplier.getEmail() != null ? supplier.getEmail() : ""),
                    Updates.set("address", supplier.getAddress() != null ? supplier.getAddress() : ""));
            suppliers.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            suppliers.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int count() {
        return (int) suppliers.countDocuments();
    }

    private Supplier mapRow(Document doc) {
        Supplier s = new Supplier();
        s.setId(doc.getInteger("_id"));
        s.setCompanyName(doc.getString("company_name"));
        s.setContactPerson(doc.getString("contact_person"));
        s.setPhone(doc.getString("phone"));
        s.setEmail(doc.getString("email"));
        s.setAddress(doc.getString("address"));
        return s;
    }
}
