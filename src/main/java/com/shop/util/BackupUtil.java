package com.shop.util;

import com.shop.dao.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BackupUtil {
    private static final JsonWriterSettings SETTINGS =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    public static String exportDatabase(Path targetDir) throws IOException {
        MongoDatabase db = DatabaseManager.getInstance().getDatabase();
        Files.createDirectories(targetDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = targetDir.resolve("shop-backup-" + stamp + ".json");

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        List<String> collectionNames = db.listCollectionNames().into(new ArrayList<>());
        boolean first = true;
        for (String name : collectionNames) {
            MongoCollection<Document> coll = db.getCollection(name);
            long count = coll.countDocuments();
            if (count == 0) continue;
            if (!first) sb.append(",\n");
            first = false;
            sb.append("\"").append(name).append("\": [");
            boolean firstDoc = true;
            for (Document doc : coll.find()) {
                if (!firstDoc) sb.append(", ");
                firstDoc = false;
                sb.append(doc.toJson(SETTINGS));
            }
            sb.append("]");
        }
        sb.append("\n}");

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        return file.toString();
    }

    public static int restoreDatabase(Path file) throws IOException {
        MongoDatabase db = DatabaseManager.getInstance().getDatabase();
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Document backup = Document.parse(json);
        int restored = 0;

        for (String collectionName : backup.keySet()) {
            MongoCollection<Document> coll = db.getCollection(collectionName);
            coll.deleteMany(new Document());
            List<Document> docs = backup.getList(collectionName, Document.class);
            if (docs == null || docs.isEmpty()) continue;
            coll.insertMany(docs);
            restored += docs.size();
        }
        return restored;
    }

    public static void wipeAllData() {
        MongoDatabase db = DatabaseManager.getInstance().getDatabase();
        for (String name : db.listCollectionNames().into(new ArrayList<>())) {
            db.getCollection(name).deleteMany(new Document());
        }
    }
}
