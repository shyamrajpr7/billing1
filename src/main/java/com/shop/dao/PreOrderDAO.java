package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.PreOrder;
import com.shop.model.PreOrderItem;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.util.SessionManager;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreOrderDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> preOrders = db.getCollection("pre_orders");

    public boolean create(PreOrder po) {
        try {
            po.computeTotal();
            int id = db.nextId("pre_orders");
            po.setId(id);
            if (po.getOrderNumber() == null || po.getOrderNumber().isEmpty()) {
                po.setOrderNumber(generateOrderNumber());
            }
            po.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("order_number", po.getOrderNumber())
                    .append("customer_id", po.getCustomerId())
                    .append("customer_name", po.getCustomerName() != null ? po.getCustomerName() : "")
                    .append("customer_phone", po.getCustomerPhone() != null ? po.getCustomerPhone() : "")
                    .append("status", po.getStatus())
                    .append("total_amount", po.getTotalAmount())
                    .append("advance_paid", po.getAdvancePaid())
                    .append("pickup_date", po.getPickupDate() != null ? po.getPickupDate().toString() : null)
                    .append("note", po.getNote() != null ? po.getNote() : "")
                    .append("created_at", po.getCreatedAt().toString())
                    .append("fulfilled_at", null)
                    .append("sale_invoice_number", null)
                    .append("items", toItemDocs(po.getItems()));
            preOrders.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fulfills a pending pre-order: records the sale (deducts stock, earns loyalty
     * points) and marks the pre-order as Fulfilled with the sale invoice reference.
     */
    public boolean markAsFulfilled(int preOrderId) {
        try {
            PreOrder po = findById(preOrderId);
            if (po == null) return false;
            if (!PreOrder.STATUS_PENDING.equals(po.getStatus())) return false;
            if (po.getItems().isEmpty()) return false;

            Sale sale = new Sale();
            sale.setInvoiceNumber(new SaleDAO().generateNextInvoiceNumber());
            sale.setCustomerId(po.getCustomerId());
            sale.setUserId(SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0);
            sale.setPaymentMethod("Advance");
            sale.setCreditSale(false);
            sale.setSubtotal(po.getTotalAmount());
            sale.setDiscountAmount(0);
            sale.setTax(0);
            sale.setTotal(po.getTotalAmount());
            sale.setAmountPaid(po.getTotalAmount());
            for (PreOrderItem item : po.getItems()) {
                sale.addItem(new SaleItem(item.getProductId(), item.getProductName(),
                        item.getQuantity(), item.getUnitPrice()));
            }

            boolean saleOk = new SaleDAO().createSale(sale);
            if (!saleOk) return false;

            Bson filter = Filters.eq("_id", preOrderId);
            Bson update = Updates.combine(
                    Updates.set("status", PreOrder.STATUS_FULFILLED),
                    Updates.set("fulfilled_at", LocalDateTime.now().toString()),
                    Updates.set("sale_invoice_number", sale.getInvoiceNumber()));
            preOrders.updateOne(filter, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancel(int preOrderId) {
        try {
            PreOrder po = findById(preOrderId);
            if (po == null) return false;
            if (!PreOrder.STATUS_PENDING.equals(po.getStatus())) return false;
            preOrders.updateOne(Filters.eq("_id", preOrderId),
                    Updates.set("status", PreOrder.STATUS_CANCELLED));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PreOrder> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : preOrders.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public List<PreOrder> findPending() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : preOrders.find(Filters.eq("status", PreOrder.STATUS_PENDING))
                .sort(new Document("pickup_date", 1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public PreOrder findById(int id) {
        Document doc = preOrders.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc, true) : null;
    }

    public int countPending() {
        return (int) preOrders.countDocuments(Filters.eq("status", PreOrder.STATUS_PENDING));
    }

    public List<PreOrder> findDueSoon(int days) {
        String today = LocalDate.now().toString();
        String limit = LocalDate.now().plusDays(days).toString();
        List<Document> docs = new ArrayList<>();
        for (Document doc : preOrders.find(Filters.and(
                        Filters.eq("status", PreOrder.STATUS_PENDING),
                        Filters.gte("pickup_date", today),
                        Filters.lte("pickup_date", limit)))
                .sort(new Document("pickup_date", 1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    private List<PreOrder> mapRows(List<Document> docs) {
        Map<Integer, String> customerNames = loadCustomerNames(docs);
        List<PreOrder> list = new ArrayList<>();
        for (Document doc : docs) {
            PreOrder po = mapRow(doc, false);
            Integer cid = doc.getInteger("customer_id");
            if (cid != null && cid > 0) {
                String name = customerNames.get(cid);
                if (name != null) po.setCustomerName(name);
            }
            list.add(po);
        }
        return list;
    }

    private PreOrder mapRow(Document doc, boolean withItems) {
        PreOrder po = new PreOrder();
        po.setId(doc.getInteger("_id"));
        po.setOrderNumber(doc.getString("order_number"));
        Integer cid = doc.getInteger("customer_id");
        po.setCustomerId(cid != null ? cid : 0);
        po.setCustomerName(doc.getString("customer_name"));
        po.setCustomerPhone(doc.getString("customer_phone"));
        po.setStatus(doc.getString("status"));
        po.setTotalAmount(doc.getDouble("total_amount") != null ? doc.getDouble("total_amount") : 0);
        po.setAdvancePaid(doc.getDouble("advance_paid") != null ? doc.getDouble("advance_paid") : 0);
        String pickup = doc.getString("pickup_date");
        if (pickup != null) {
            try { po.setPickupDate(LocalDate.parse(pickup)); } catch (Exception ignored) {}
        }
        po.setNote(doc.getString("note"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { po.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        String fulfilled = doc.getString("fulfilled_at");
        if (fulfilled != null) {
            try { po.setFulfilledAt(LocalDateTime.parse(fulfilled)); } catch (Exception ignored) {}
        }
        po.setSaleInvoiceNumber(doc.getString("sale_invoice_number"));
        if (withItems) {
            po.setItems(mapItems(doc));
        }
        return po;
    }

    private List<PreOrderItem> mapItems(Document doc) {
        List<PreOrderItem> items = new ArrayList<>();
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    PreOrderItem item = new PreOrderItem(
                            itemDoc.getInteger("product_id"),
                            itemDoc.getString("product_name"),
                            itemDoc.getString("barcode"),
                            itemDoc.getInteger("quantity"),
                            itemDoc.getDouble("unit_price"));
                    item.setTotal(itemDoc.getDouble("total"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private List<Document> toItemDocs(List<PreOrderItem> items) {
        List<Document> docs = new ArrayList<>();
        for (PreOrderItem item : items) {
            docs.add(new Document("product_id", item.getProductId())
                    .append("product_name", item.getProductName())
                    .append("barcode", item.getBarcode())
                    .append("quantity", item.getQuantity())
                    .append("unit_price", item.getUnitPrice())
                    .append("total", item.getTotal()));
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

    private String generateOrderNumber() {
        long count = preOrders.countDocuments();
        return String.format("PR-%06d", count + 1);
    }
}
