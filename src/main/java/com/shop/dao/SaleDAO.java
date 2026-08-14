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
                        .append("loyalty_points_redeemed", sale.getPointsRedeemed())
                        .append("tax", sale.getTax())
                        .append("total", sale.getTotal())
                        .append("payment_method", sale.getPaymentMethod())
                        .append("gift_card_number", sale.getGiftCardNumber())
                        .append("gift_card_amount", sale.getGiftCardAmount())
                        .append("credit_sale", sale.isCreditSale())
                        .append("amount_paid", sale.getAmountPaid())
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

                if (sale.getGiftCardAmount() > 0) {
                    Bson gcFilter = Filters.and(
                            Filters.eq("card_number", sale.getGiftCardNumber()),
                            Filters.eq("status", "ACTIVE"),
                            Filters.gte("balance", sale.getGiftCardAmount()));
                    var result = db.getCollection("gift_cards").updateOne(session, gcFilter,
                            Updates.inc("balance", -sale.getGiftCardAmount()));
                    if (result.getMatchedCount() == 0) {
                        throw new RuntimeException("Gift card balance is insufficient: " + sale.getGiftCardNumber());
                    }
                }

                if (sale.getCustomerId() > 0) {
                    int earned = (int) (sale.getTotal() / 100);
                    int redeemed = sale.getPointsRedeemed();
                    int net = earned - redeemed;
                    if (redeemed > 0) {
                        Bson filter = Filters.and(
                                Filters.eq("_id", sale.getCustomerId()),
                                Filters.gte("loyalty_points", redeemed));
                        var result = db.getCollection("customers").updateOne(session, filter,
                                Updates.inc("loyalty_points", net));
                        if (result.getMatchedCount() == 0) {
                            throw new RuntimeException("Customer does not have enough loyalty points to redeem.");
                        }
                    } else if (net > 0) {
                        db.getCollection("customers").updateOne(session,
                                Filters.eq("_id", sale.getCustomerId()),
                                Updates.inc("loyalty_points", net));
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

    public Sale findByInvoiceNumber(String invoiceNumber) {
        Document doc = sales.find(Filters.eq("invoice_number", invoiceNumber)).first();
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

    public long count() {
        return sales.countDocuments();
    }

    public double getTotalRevenueThisMonth() {
        String month = LocalDate.now().toString().substring(0, 7);
        return sumRevenue(Filters.regex("created_at", "^" + month));
    }

    public double getTotalRevenueForMonth(String yearMonth) {
        return sumRevenue(Filters.regex("created_at", "^" + yearMonth));
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

    public List<Sale> getOutstandingCreditSales() {
        Bson filter = Filters.and(Filters.eq("credit_sale", true));
        List<Document> docs = new ArrayList<>();
        for (Document doc : sales.find(filter).sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        Map<Integer, String> userNames = loadUserNames(docs);
        List<Sale> list = new ArrayList<>();
        for (Document doc : docs) {
            Sale sale = mapSaleRow(doc, customerNames, userNames);
            if (sale.getDueAmount() > 0.001) {
                list.add(sale);
            }
        }
        return list;
    }

    public boolean collectPayment(int saleId, double amount) {
        if (amount <= 0) return false;
        try {
            Bson filter = Filters.and(
                    Filters.eq("_id", saleId),
                    Filters.eq("credit_sale", true));
            Document saleDoc = sales.find(filter).first();
            if (saleDoc == null) return false;
            double total = saleDoc.getDouble("total");
            double paid = saleDoc.getDouble("amount_paid") != null ? saleDoc.getDouble("amount_paid") : 0;
            if (paid + amount > total + 0.001) return false;
            var result = sales.updateOne(filter, Updates.inc("amount_paid", amount));
            if (result.getMatchedCount() > 0) {
                Document invDoc = sales.find(Filters.eq("_id", saleId)).first();
                String inv = invDoc != null ? invDoc.getString("invoice_number") : ("#" + saleId);
                new ActivityLogDAO().log("PAYMENT", "Collected ₹" + String.format("%.2f", amount)
                        + " towards credit sale " + inv);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public java.util.LinkedHashMap<String, Double> getCustomerDueTotals() {
        java.util.LinkedHashMap<String, Double> dueByCustomer = new java.util.LinkedHashMap<>();
        for (Sale sale : getOutstandingCreditSales()) {
            dueByCustomer.merge(sale.getCustomerName(), sale.getDueAmount(), Double::sum);
        }
        return dueByCustomer;
    }

    public java.util.List<com.shop.model.BestSeller> getTopSellers(int days, int limit) {
        Map<Integer, int[]> unitsByProduct = new HashMap<>();
        Map<Integer, Double> revenueByProduct = new HashMap<>();
        String start = LocalDate.now().minusDays(Math.max(days - 1, 0)).toString();
        for (Document doc : sales.find(Filters.gte("created_at", start))) {
            Object raw = doc.get("items");
            if (!(raw instanceof List<?> list)) continue;
            for (Object o : list) {
                if (!(o instanceof Document itemDoc)) continue;
                Integer pid = itemDoc.getInteger("product_id");
                Integer qty = itemDoc.getInteger("quantity");
                Double unitPrice = itemDoc.getDouble("unit_price");
                if (pid != null) {
                    unitsByProduct.computeIfAbsent(pid, k -> new int[1])[0] += qty != null ? qty : 0;
                    revenueByProduct.merge(pid, (qty != null ? qty : 0) * (unitPrice != null ? unitPrice : 0), Double::sum);
                }
            }
        }
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, String> categories = new HashMap<>();
        for (Document p : db.getCollection("products").find(Filters.in("_id", unitsByProduct.keySet()))) {
            names.put(p.getInteger("_id"), p.getString("name"));
            categories.put(p.getInteger("_id"), p.getString("category"));
        }
        java.util.List<com.shop.model.BestSeller> list = new java.util.ArrayList<>();
        for (Map.Entry<Integer, Double> e : revenueByProduct.entrySet()) {
            com.shop.model.BestSeller bs = new com.shop.model.BestSeller();
            bs.setProductId(e.getKey());
            bs.setName(names.getOrDefault(e.getKey(), "Product #" + e.getKey()));
            bs.setCategory(categories.getOrDefault(e.getKey(), ""));
            bs.setUnits(unitsByProduct.get(e.getKey())[0]);
            bs.setRevenue(Math.round(e.getValue() * 100.0) / 100.0);
            list.add(bs);
        }
        list.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        if (list.size() > limit) list = new java.util.ArrayList<>(list.subList(0, limit));
        return list;
    }

    public java.util.List<com.shop.model.CategorySales> getTopCategories(int days) {
        java.util.List<com.shop.model.BestSeller> sellers = getTopSellers(days, 1000);
        Map<String, com.shop.model.CategorySales> byCat = new java.util.LinkedHashMap<>();
        for (com.shop.model.BestSeller bs : sellers) {
            String cat = bs.getCategory() == null || bs.getCategory().isEmpty() ? "Uncategorized" : bs.getCategory();
            com.shop.model.CategorySales cs = byCat.computeIfAbsent(cat, k -> {
                com.shop.model.CategorySales c = new com.shop.model.CategorySales();
                c.setCategory(k);
                return c;
            });
            cs.setUnits(cs.getUnits() + bs.getUnits());
            cs.setRevenue(cs.getRevenue() + bs.getRevenue());
        }
        java.util.List<com.shop.model.CategorySales> list = new java.util.ArrayList<>(byCat.values());
        list.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        return list;
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
        s.setPointsRedeemed(doc.getInteger("loyalty_points_redeemed") != null ? doc.getInteger("loyalty_points_redeemed") : 0);
        s.setTax(doc.getDouble("tax"));
        s.setTotal(doc.getDouble("total"));
        s.setPaymentMethod(doc.getString("payment_method"));
        s.setGiftCardNumber(doc.getString("gift_card_number"));
        s.setGiftCardAmount(doc.getDouble("gift_card_amount") != null ? doc.getDouble("gift_card_amount") : 0);
        s.setCreditSale(doc.getBoolean("credit_sale") != null && doc.getBoolean("credit_sale"));
        s.setAmountPaid(doc.getDouble("amount_paid") != null ? doc.getDouble("amount_paid")
                : (s.isCreditSale() ? 0 : s.getTotal()));
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            s.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        return s;
    }
}
