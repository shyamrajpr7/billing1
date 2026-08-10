package com.shop.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaleDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> sales = db.getCollection("sales");
    private final MongoCollection<Document> products = db.getCollection("products");

    public boolean createSale(Sale sale) {
        MongoClient client = db.getClient();
        try (ClientSession session = client.startSession()) {
            session.startTransaction();
            try {
                int saleId = db.nextId("sales");
                sale.setId(saleId);

                List<Document> items = new ArrayList<>();
                for (SaleItem item : sale.getItems()) {
                    items.add(new Document("product_id", item.getProductId())
                            .append("product_name", item.getProductName())
                            .append("quantity", item.getQuantity())
                            .append("unit_price", item.getUnitPrice())
                            .append("discount", item.getDiscount())
                            .append("total", item.getTotal()));
                }

                Document saleDoc = new Document("_id", saleId)
                        .append("invoice_number", sale.getInvoiceNumber())
                        .append("customer_id", sale.getCustomerId())
                        .append("user_id", sale.getUserId())
                        .append("subtotal", sale.getSubtotal())
                        .append("discount_amount", sale.getDiscountAmount())
                        .append("tax", sale.getTax())
                        .append("total", sale.getTotal())
                        .append("payment_method", sale.getPaymentMethod())
                        .append("created_at", LocalDateTime.now().toString())
                        .append("items", items);
                sales.insertOne(session, saleDoc);

                for (SaleItem item : sale.getItems()) {
                    Bson stockFilter = Filters.and(
                            Filters.eq("_id", item.getProductId()),
                            Filters.gte("quantity", item.getQuantity()));
                    var result = products.updateOne(session, stockFilter, Updates.inc("quantity", -item.getQuantity()));
                    if (result.getMatchedCount() == 0) {
                        throw new RuntimeException("Not enough stock for product: " + item.getProductName());
                    }
                }

                if (sale.getCustomerId() > 0) {
                    int points = (int) (sale.getTotal() / 100);
                    if (points > 0) {
                        db.getCollection("customers").updateOne(session,
                                Filters.eq("_id", sale.getCustomerId()),
                                Updates.inc("loyalty_points", points));
                    }
                }

                session.commitTransaction();
                new ActivityLogDAO().log("SALE", "Invoice " + sale.getInvoiceNumber() +
                        " for ₹" + String.format("%.2f", sale.getTotal()) +
                        " (" + sale.getItems().size() + " items)");
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

    public List<Sale> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : sales.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        Map<Integer, String> userNames = loadUserNames(docs);
        List<Sale> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapSaleRow(doc, customerNames, userNames));
        }
        return list;
    }

    public Sale findById(int id) {
        Document doc = sales.find(Filters.eq("_id", id)).first();
        if (doc == null) return null;
        Map<Integer, String> customerNames = loadCustomerNames(List.of(doc));
        Map<Integer, String> userNames = loadUserNames(List.of(doc));
        Sale sale = mapSaleRow(doc, customerNames, userNames);
        sale.setItems(findItemsBySaleId(sale.getId()));
        return sale;
    }

    public List<SaleItem> findItemsBySaleId(int saleId) {
        List<SaleItem> items = new ArrayList<>();
        Document doc = sales.find(Filters.eq("_id", saleId)).first();
        if (doc == null) return items;
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    SaleItem item = new SaleItem();
                    item.setProductId(itemDoc.getInteger("product_id"));
                    item.setProductName(itemDoc.getString("product_name"));
                    item.setQuantity(itemDoc.getInteger("quantity"));
                    item.setUnitPrice(itemDoc.getDouble("unit_price"));
                    item.setDiscount(itemDoc.getDouble("discount"));
                    item.setTotal(itemDoc.getDouble("total"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<Sale> findByCustomerId(int customerId) {
        List<Document> docs = new ArrayList<>();
        for (Document doc : sales.find(Filters.eq("customer_id", customerId)).sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        Map<Integer, String> userNames = loadUserNames(docs);
        List<Sale> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapSaleRow(doc, customerNames, userNames));
        }
        return list;
    }

    public double getTotalRevenueToday() {
        String today = LocalDate.now().toString();
        return sumRevenue(Filters.regex("created_at", "^" + today));
    }

    public int getSalesCountToday() {
        String today = LocalDate.now().toString();
        return (int) sales.countDocuments(Filters.regex("created_at", "^" + today));
    }

    public double getTotalRevenueThisMonth() {
        String month = LocalDate.now().toString().substring(0, 7);
        return sumRevenue(Filters.regex("created_at", "^" + month));
    }

    public java.util.LinkedHashMap<String, Double> getDailyRevenue(int days) {
        java.util.LinkedHashMap<String, Double> daily = new java.util.LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            daily.put(today.minusDays(i).toString(), 0.0);
        }
        for (Document doc : sales.find()) {
            String created = doc.getString("created_at");
            if (created == null || created.length() < 10) continue;
            String day = created.substring(0, 10);
            if (daily.containsKey(day)) {
                daily.put(day, daily.get(day) + doc.getDouble("total"));
            }
        }
        return daily;
    }

    /**
     * Returns productId -> total units sold over the last {@code days} days,
     * used by the smart reorder suggestions to estimate daily demand.
     */
    public Map<Integer, Integer> getProductSalesLastDays(int days) {
        Map<Integer, Integer> sold = new HashMap<>();
        String start = LocalDate.now().minusDays(Math.max(days - 1, 0)).toString();
        for (Document doc : sales.find(Filters.gte("created_at", start))) {
            Object raw = doc.get("items");
            if (!(raw instanceof List<?> list)) continue;
            for (Object o : list) {
                if (!(o instanceof Document itemDoc)) continue;
                Integer pid = itemDoc.getInteger("product_id");
                Integer qty = itemDoc.getInteger("quantity");
                if (pid != null && qty != null) {
                    sold.merge(pid, qty, Integer::sum);
                }
            }
        }
        return sold;
    }

    public String generateNextInvoiceNumber() {
        long count = sales.countDocuments();
        return String.format("INV-%06d", count + 1);
    }

    private double sumRevenue(Bson filter) {
        double total = 0;
        for (Document doc : sales.find(filter)) {
            total += doc.getDouble("total");
        }
        return total;
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

    private Map<Integer, String> loadUserNames(List<Document> docs) {
        Map<Integer, String> names = new HashMap<>();
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Document doc : docs) {
            Integer uid = doc.getInteger("user_id");
            if (uid != null) ids.add(uid);
        }
        if (ids.isEmpty()) return names;
        for (Document u : db.getCollection("users").find(Filters.in("_id", ids))) {
            names.put(u.getInteger("_id"), u.getString("full_name"));
        }
        return names;
    }

    private Sale mapSaleRow(Document doc, Map<Integer, String> customerNames, Map<Integer, String> userNames) {
        Sale s = new Sale();
        s.setId(doc.getInteger("_id"));
        s.setInvoiceNumber(doc.getString("invoice_number"));
        Integer cid = doc.getInteger("customer_id");
        s.setCustomerId(cid != null ? cid : 0);
        String customerName = cid != null ? customerNames.get(cid) : null;
        s.setCustomerName(customerName != null ? customerName : "Walk-in Customer");
        Integer uid = doc.getInteger("user_id");
        s.setUserId(uid != null ? uid : 0);
        String userName = uid != null ? userNames.get(uid) : null;
        s.setUserName(userName != null ? userName : "");
        s.setSubtotal(doc.getDouble("subtotal"));
        s.setDiscountAmount(doc.getDouble("discount_amount"));
        s.setTax(doc.getDouble("tax"));
        s.setTotal(doc.getDouble("total"));
        s.setPaymentMethod(doc.getString("payment_method"));
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            s.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        return s;
    }
}
