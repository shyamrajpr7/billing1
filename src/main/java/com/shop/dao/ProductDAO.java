package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Product;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ProductDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> products = db.getCollection("products");

    public List<Product> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : products.find().sort(new Document("name", 1))) {
            docs.add(doc);
        }
        Map<Integer, String> supplierNames = loadSupplierNames(docs);
        List<Product> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapRow(doc, supplierNames));
        }
        return list;
    }

    public List<Product> search(String query) {
        String q = query.toLowerCase();
        Pattern pattern = Pattern.compile(".*" + Pattern.quote(q) + ".*", Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.or(
                Filters.regex("name", pattern),
                Filters.regex("barcode", pattern),
                Filters.regex("category", pattern));
        List<Document> docs = new ArrayList<>();
        for (Document doc : products.find(filter).sort(new Document("name", 1))) {
            docs.add(doc);
        }
        Map<Integer, String> supplierNames = loadSupplierNames(docs);
        List<Product> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapRow(doc, supplierNames));
        }
        return list;
    }

    public Product findById(int id) {
        Document doc = products.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc, supplierNameFor(doc)) : null;
    }

    public Product findByBarcode(String barcode) {
        Document doc = products.find(Filters.eq("barcode", barcode)).first();
        return doc != null ? mapRow(doc, supplierNameFor(doc)) : null;
    }

    public boolean insert(Product product) {
        try {
            if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
                product.setBarcode(generateBarcode());
            }
            int id = db.nextId("products");
            product.setId(id);
            Document doc = new Document("_id", id)
                    .append("name", product.getName())
                    .append("barcode", product.getBarcode())
                    .append("category", product.getCategory() != null ? product.getCategory() : "")
                    .append("buy_price", product.getBuyPrice())
                    .append("sell_price", product.getSellPrice())
                    .append("quantity", product.getQuantity())
                    .append("min_stock_level", product.getMinStockLevel())
                    .append("supplier_id", product.getSupplierId());
            products.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(Product product) {
        try {
            Bson filter = Filters.eq("_id", product.getId());
            Bson update = Updates.combine(
                    Updates.set("name", product.getName()),
                    Updates.set("barcode", product.getBarcode()),
                    Updates.set("category", product.getCategory() != null ? product.getCategory() : ""),
                    Updates.set("buy_price", product.getBuyPrice()),
                    Updates.set("sell_price", product.getSellPrice()),
                    Updates.set("quantity", product.getQuantity()),
                    Updates.set("min_stock_level", product.getMinStockLevel()),
                    Updates.set("supplier_id", product.getSupplierId()));
            products.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateQuantity(int productId, int quantityChange) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("_id", productId),
                    Filters.gte("quantity", -quantityChange));
            Bson update = Updates.inc("quantity", quantityChange);
            products.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            products.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int count() {
        return (int) products.countDocuments();
    }

    public int countLowStock() {
        return (int) products.countDocuments(new Document("$expr",
                new Document("$lte", java.util.List.of("$quantity", "$min_stock_level"))));
    }

    public List<Product> findLowStock() {
        Bson filter = new Document("$expr",
                new Document("$lte", java.util.List.of("$quantity", "$min_stock_level")));
        List<Document> docs = new ArrayList<>();
        for (Document doc : products.find(filter).sort(new Document("quantity", 1))) {
            docs.add(doc);
        }
        Map<Integer, String> supplierNames = loadSupplierNames(docs);
        List<Product> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapRow(doc, supplierNames));
        }
        return list;
    }

    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        List<String> distinct = products.distinct("category", String.class).into(new ArrayList<>());
        for (String c : distinct) {
            if (c != null && !c.isEmpty()) {
                categories.add(c);
            }
        }
        categories.sort(String::compareTo);
        return categories;
    }

    private Map<Integer, String> loadSupplierNames(List<Document> productDocs) {
        Map<Integer, String> names = new HashMap<>();
        java.util.Set<Integer> supplierIds = new java.util.HashSet<>();
        for (Document doc : productDocs) {
            Integer sid = doc.getInteger("supplier_id");
            if (sid != null && sid > 0) {
                supplierIds.add(sid);
            }
        }
        if (supplierIds.isEmpty()) return names;
        MongoCollection<Document> suppliers = db.getCollection("suppliers");
        for (Document s : suppliers.find(Filters.in("_id", supplierIds))) {
            names.put(s.getInteger("_id"), s.getString("company_name"));
        }
        return names;
    }

    private String supplierNameFor(Document productDoc) {
        Integer sid = productDoc.getInteger("supplier_id");
        if (sid == null || sid <= 0) return "";
        Document s = db.getCollection("suppliers").find(Filters.eq("_id", sid)).first();
        return s != null ? s.getString("company_name") : "";
    }

    private String generateBarcode() {
        return "P" + UUID.randomUUID().toString().substring(0, 12).toUpperCase().replace("-", "");
    }

    private Product mapRow(Document doc, Map<Integer, String> supplierNames) {
        Product p = new Product();
        p.setId(doc.getInteger("_id"));
        p.setName(doc.getString("name"));
        p.setBarcode(doc.getString("barcode"));
        p.setCategory(doc.getString("category"));
        p.setBuyPrice(doc.getDouble("buy_price"));
        p.setSellPrice(doc.getDouble("sell_price"));
        p.setQuantity(doc.getInteger("quantity"));
        p.setMinStockLevel(doc.getInteger("min_stock_level"));
        Integer sid = doc.getInteger("supplier_id");
        p.setSupplierId(sid != null ? sid : 0);
        String supplierName = sid != null && supplierNames != null ? supplierNames.get(sid) : null;
        p.setSupplierName(supplierName != null ? supplierName : "");
        return p;
    }

    private Product mapRow(Document doc, String supplierName) {
        Product p = mapRow(doc, (java.util.Map<Integer, String>) null);
        p.setSupplierName(supplierName != null ? supplierName : "");
        return p;
    }
}
