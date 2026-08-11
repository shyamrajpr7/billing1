package com.shop.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Stocktake;
import com.shop.model.StocktakeItem;
import com.shop.util.SessionManager;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StocktakeDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> stocktakes = db.getCollection("stocktakes");
    private final MongoCollection<Document> products = db.getCollection("products");

    public boolean create(Stocktake st) {
        try {
            int id = db.nextId("stocktakes");
            st.setId(id);
            int userId = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0;
            st.setCreatedBy(userId);
            st.setCreatedByName(SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getFullName() : "System");
            st.setStatus("OPEN");

            List<Document> itemDocs = new ArrayList<>();
            for (StocktakeItem item : st.getItems()) {
                itemDocs.add(toItemDoc(item));
            }
            stocktakes.insertOne(new Document("_id", id)
                    .append("name", st.getName())
                    .append("status", "OPEN")
                    .append("created_by", userId)
                    .append("created_by_name", st.getCreatedByName())
                    .append("created_at", LocalDateTime.now().toString())
                    .append("completed_at", (String) null)
                    .append("items", itemDocs));
            new ActivityLogDAO().log("STOCKTAKE", "Started stocktake '" + st.getName() + "' with " + st.getItems().size() + " products");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveCounts(int stocktakeId, List<StocktakeItem> items) {
        try {
            List<Document> itemDocs = new ArrayList<>();
            for (StocktakeItem item : items) {
                itemDocs.add(toItemDoc(item));
            }
            stocktakes.updateOne(Filters.eq("_id", stocktakeId), Updates.set("items", itemDocs));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean complete(Stocktake st) {
        MongoClient client = db.getClient();
        try (ClientSession session = client.startSession()) {
            session.startTransaction();
            try {
                int adjusted = 0;
                for (StocktakeItem item : st.getItems()) {
                    if (item.getVariance() != 0) {
                        products.updateOne(session,
                                Filters.eq("_id", item.getProductId()),
                                Updates.set("quantity", item.getCountedQty()));
                        adjusted++;
                    }
                }
                stocktakes.updateOne(session, Filters.eq("_id", st.getId()),
                        Updates.combine(
                                Updates.set("status", "COMPLETED"),
                                Updates.set("completed_at", LocalDateTime.now().toString()),
                                Updates.set("items", st.getItems().stream().map(this::toItemDoc).toList())));
                session.commitTransaction();
                new ActivityLogDAO().log("STOCK_ADJUST", "Completed stocktake '" + st.getName()
                        + "': " + adjusted + " product(s) adjusted");
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

    public boolean cancel(int id) {
        try {
            stocktakes.updateOne(Filters.eq("_id", id), Updates.set("status", "CANCELLED"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Stocktake> findAll() {
        List<Stocktake> list = new ArrayList<>();
        for (Document doc : stocktakes.find().sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Stocktake> findOpen() {
        List<Stocktake> list = new ArrayList<>();
        for (Document doc : stocktakes.find(Filters.eq("status", "OPEN")).sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    private Document toItemDoc(StocktakeItem item) {
        return new Document("product_id", item.getProductId())
                .append("product_name", item.getProductName())
                .append("barcode", item.getBarcode())
                .append("expected_qty", item.getExpectedQty())
                .append("counted_qty", item.getCountedQty());
    }

    private Stocktake mapRow(Document doc) {
        Stocktake st = new Stocktake();
        st.setId(doc.getInteger("_id"));
        st.setName(doc.getString("name"));
        st.setStatus(doc.getString("status"));
        st.setCreatedBy(doc.getInteger("created_by") != null ? doc.getInteger("created_by") : 0);
        st.setCreatedByName(doc.getString("created_by_name"));
        String created = doc.getString("created_at");
        if (created != null) {
            try { st.setCreatedAt(LocalDateTime.parse(created)); } catch (Exception ignored) {}
        }
        String completed = doc.getString("completed_at");
        if (completed != null) {
            try { st.setCompletedAt(LocalDateTime.parse(completed)); } catch (Exception ignored) {}
        }
        Object raw = doc.get("items");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Document idoc) {
                    StocktakeItem item = new StocktakeItem();
                    item.setProductId(idoc.getInteger("product_id"));
                    item.setProductName(idoc.getString("product_name"));
                    item.setBarcode(idoc.getString("barcode"));
                    item.setExpectedQty(idoc.getInteger("expected_qty") != null ? idoc.getInteger("expected_qty") : 0);
                    item.setCountedQty(idoc.getInteger("counted_qty") != null ? idoc.getInteger("counted_qty") : 0);
                    st.getItems().add(item);
                }
            }
        }
        return st;
    }
}
