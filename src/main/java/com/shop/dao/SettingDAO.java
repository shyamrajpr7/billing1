package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.Setting;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class SettingDAO {
    public static final String KEY_STORE_NAME = "store_name";
    public static final String KEY_OWNER = "owner";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_GSTIN = "gstin";
    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_TAX_PERCENT = "tax_percent";
    public static final String KEY_RECEIPT_FOOTER = "receipt_footer";
    public static final String KEY_LOW_STOCK_ALERT = "low_stock_alert";

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> settings = db.getCollection("settings");

    public String get(String key, String defaultValue) {
        Document doc = settings.find(Filters.eq("key", key)).first();
        if (doc == null) return defaultValue;
        String value = doc.getString("value");
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    public boolean set(String key, String value) {
        try {
            Document existing = settings.find(Filters.eq("key", key)).first();
            if (existing != null) {
                settings.updateOne(Filters.eq("key", key),
                        new Document("$set", new Document("value", value != null ? value : "")));
            } else {
                settings.insertOne(new Document("_id", db.nextId("settings"))
                        .append("key", key)
                        .append("value", value != null ? value : ""));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Setting> findAll() {
        List<Setting> list = new ArrayList<>();
        for (Document doc : settings.find().sort(new Document("key", 1))) {
            Setting s = new Setting();
            s.setId(doc.getInteger("_id"));
            s.setKey(doc.getString("key"));
            s.setValue(doc.getString("value"));
            list.add(s);
        }
        return list;
    }

    public int count() {
        return (int) settings.countDocuments();
    }
}
