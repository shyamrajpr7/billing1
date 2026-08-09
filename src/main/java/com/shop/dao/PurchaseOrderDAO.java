package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.PurchaseOrder;
import com.shop.model.PurchaseOrderItem;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseOrderDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> pos = db.getCollection("purchase_orders");
    private final MongoCollection<Document> products = db.getCollection("products");

    public boolean create(PurchaseOrder po) {
        try {
            po.computeTotal();
            int id = db.nextId("purchase_orders");
            po.setId(id);
            if (po.getOrderNumber() == null || po.getOrderNumber().isEmpty()) {
                po.setOrderNumber(generateOrderNumber());
            }
            po.setCreatedAt(LocalDateTime.now());
            Document doc = new Document("_id", id)
                    .append("order_number", po.getOrderNumber())
                    .append("supplier_id", po.getSupplierId())
                    .append("status", po.getStatus())
                    .append("total_cost", po.getTotalCost())
                    .append("note", po.getNote() != null ? po.getNote() : "")
                    .append("created_at", po.getCreatedAt().toString())
                    .append("received_at", po.getReceivedAt() != null ? po.getReceivedAt().toString() : null)
                    .append("items", toItemDocs(po.getItems()));
            pos.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markAsReceived(int poId) {
        try {
            PurchaseOrder po = findById(poId);
            if (po == null) return false;
            if (!PurchaseOrder.STATUS_PENDING.equals(po.getStatus())) return false;

            // Update stock (transactionally)
            try (com.mongodb.client.ClientSession session = db.getClient().startSession()) {
                session.startTransaction();
                try {
                    for (PurchaseOrderItem item : po.getItems()) {
                        products.updateOne(session,
                                Filters.eq("_id", item.getProductId()),
                                Updates.combine(
                                        Updates.inc("quantity", item.getQuantity()),
                                        Updates.set("buy_price", item.getUnitCost())));
                    }
                    Bson filter = Filters.eq("_id", poId);
                    Bson update = Updates.combine(
                            Updates.set("status", PurchaseOrder.STATUS_RECEIVED),
                            Updates.set("received_at", LocalDateTime.now().toString()));
                    pos.updateOne(session, filter, update);
                    session.commitTransaction();
                    return true;
                } catch (Exception e) {
                    session.abortTransaction();
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancel(int poId) {
        try {
            PurchaseOrder po = findById(poId);
            if (po == null) return false;
            if (!PurchaseOrder.STATUS_PENDING.equals(po.getStatus())) return false;
            pos.updateOne(Filters.eq("_id", poId),
                    Updates.set("status", PurchaseOrder.STATUS_CANCELLED));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int poId) {
        try {
            pos.deleteOne(Filters.eq("_id", poId));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PurchaseOrder> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : pos.find().sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public List<PurchaseOrder> findPending() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : pos.find(Filters.eq("status", PurchaseOrder.STATUS_PENDING))
                .sort(new Document("created_at", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public PurchaseOrder findById(int id) {
        Document doc = pos.find(Filters.eq("_id", id)).first();
        return doc != null ? mapRow(doc, true) : null;
    }

    private List<PurchaseOrder> mapRows(List<Document> docs) {
        Map<Integer, String> supplierNames = loadSupplierNames(docs);
        List<PurchaseOrder> list = new ArrayList<>();
        for (Document doc : docs) {
            list.add(mapRow(doc, false, supplierNames));
        }
        return list;
    }

    private PurchaseOrder mapRow(Document doc, boolean withItems) {
        Map<Integer, String> names = loadSupplierNames(List.of(doc));
        return mapRow(doc, withItems, names);
    }

    private PurchaseOrder mapRow(Document doc, boolean withItems, Map<Integer, String> supplierNames) {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(doc.getInteger("_id"));
        po.setOrderNumber(doc.getString("order_number"));
        Integer sid = doc.getInteger("supplier_id");
        po.setSupplierId(sid != null ? sid : 0);
        String supplierName = sid != null ? supplierNames.get(sid) : null;
        po.setSupplierName(supplierName != null ? supplierName : "Unknown Supplier");
        po.setStatus(doc.getString("status"));
        po.setTotalCost(doc.getDouble("total_cost"));
        po.setNote(doc.getString("note"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { po.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        String received = doc.getString("received_at");
        if (received != null) {
            try { po.setReceivedAt(LocalDateTime.parse(received)); } catch (Exception ignored) {}
        }
        if (withItems) {
            po.setItems(mapItems(doc));
        }
        return po;
    }

    private List<PurchaseOrderItem> mapItems(Document doc) {
        List<PurchaseOrderItem> items = new ArrayList<>();
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document itemDoc) {
                    PurchaseOrderItem item = new PurchaseOrderItem(
                            itemDoc.getInteger("product_id"),
                            itemDoc.getString("product_name"),
                            itemDoc.getString("barcode"),
                            itemDoc.getInteger("quantity"),
                            itemDoc.getDouble("unit_cost"));
                    item.setTotal(itemDoc.getDouble("total"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private List<Document> toItemDocs(List<PurchaseOrderItem> items) {
        List<Document> docs = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            docs.add(new Document("product_id", item.getProductId())
                    .append("product_name", item.getProductName())
                    .append("barcode", item.getBarcode())
                    .append("quantity", item.getQuantity())
                    .append("unit_cost", item.getUnitCost())
                    .append("total", item.getTotal()));
        }
        return docs;
    }

    private Map<Integer, String> loadSupplierNames(List<Document> docs) {
        Map<Integer, String> names = new HashMap<>();
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Document doc : docs) {
            Integer sid = doc.getInteger("supplier_id");
            if (sid != null && sid > 0) ids.add(sid);
        }
        if (ids.isEmpty()) return names;
        for (Document s : db.getCollection("suppliers").find(Filters.in("_id", ids))) {
            names.put(s.getInteger("_id"), s.getString("company_name"));
        }
        return names;
    }

    private String generateOrderNumber() {
        long count = pos.countDocuments();
        return String.format("PO-%06d", count + 1);
    }
}
