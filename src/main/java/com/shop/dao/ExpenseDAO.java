package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.shop.model.Expense;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> expenses = db.getCollection("expenses");

    public boolean insert(Expense expense) {
        try {
            int id = db.nextId("expenses");
            expense.setId(id);
            Document doc = new Document("_id", id)
                    .append("description", expense.getDescription())
                    .append("category", expense.getCategory() != null && !expense.getCategory().isEmpty()
                            ? expense.getCategory() : "General")
                    .append("amount", expense.getAmount())
                    .append("expense_date", expense.getExpenseDate().toString())
                    .append("user_id", expense.getUserId())
                    .append("created_at", LocalDateTime.now().toString());
            expenses.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(int id) {
        try {
            expenses.deleteOne(Filters.eq("_id", id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Expense> findAll() {
        List<Expense> list = new ArrayList<>();
        for (Document doc : expenses.find().sort(new Document("expense_date", -1)).sort(new Document("created_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Expense> findToday() {
        String today = LocalDate.now().toString();
        List<Expense> list = new ArrayList<>();
        for (Document doc : expenses.find(Filters.regex("expense_date", "^" + today))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public double getTotalToday() {
        return sum(LocalDate.now().toString());
    }

    public double getTotalThisMonth() {
        return sum(LocalDate.now().toString().substring(0, 7));
    }

    public int countToday() {
        String today = LocalDate.now().toString();
        return (int) expenses.countDocuments(Filters.regex("expense_date", "^" + today));
    }

    public int count() {
        return (int) expenses.countDocuments();
    }

    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        for (String c : expenses.distinct("category", String.class).into(new ArrayList<>())) {
            if (c != null && !c.isEmpty()) categories.add(c);
        }
        categories.sort(String::compareTo);
        return categories;
    }

    private double sum(String prefix) {
        double total = 0;
        for (Document doc : expenses.find(Filters.regex("expense_date", "^" + prefix))) {
            total += doc.getDouble("amount");
        }
        return total;
    }

    private Expense mapRow(Document doc) {
        Expense e = new Expense();
        e.setId(doc.getInteger("_id"));
        e.setDescription(doc.getString("description"));
        e.setCategory(doc.getString("category"));
        e.setAmount(doc.getDouble("amount"));
        String date = doc.getString("expense_date");
        if (date != null) {
            e.setExpenseDate(LocalDate.parse(date));
        }
        Integer uid = doc.getInteger("user_id");
        e.setUserId(uid != null ? uid : 0);
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            e.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        return e;
    }
}
