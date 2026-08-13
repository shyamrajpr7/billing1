package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Attendance;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> attendance = db.getCollection("attendance");

    public boolean record(Attendance a) {
        try {
            Document existing = attendance.find(Filters.and(
                    Filters.eq("user_id", a.getUserId()),
                    Filters.eq("date", a.getDate().toString()))).first();
            if (existing != null) return false;

            int id = db.nextId("attendance");
            a.setId(id);
            Document doc = new Document("_id", id)
                    .append("user_id", a.getUserId())
                    .append("employee_name", a.getEmployeeName() != null ? a.getEmployeeName() : "")
                    .append("date", a.getDate().toString())
                    .append("check_in_time", a.getCheckInTime() != null ? a.getCheckInTime().toString() : null)
                    .append("check_out_time", null)
                    .append("status", a.getStatus())
                    .append("notes", a.getNotes() != null ? a.getNotes() : "");
            attendance.insertOne(doc);
            new ActivityLogDAO().log("ATTENDANCE", a.getEmployeeName() + " marked " + a.getStatus()
                    + " on " + a.getDateLabel());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkOut(int id) {
        var result = attendance.updateOne(
                Filters.and(Filters.eq("_id", id), Filters.eq("check_out_time", null)),
                Updates.set("check_out_time", LocalTime.now().toString()));
        return result.getMatchedCount() > 0;
    }

    public boolean updateStatus(int id, String status) {
        return attendance.updateOne(Filters.eq("_id", id), Updates.set("status", status)).getMatchedCount() > 0;
    }

    public List<Attendance> findByDate(LocalDate date) {
        List<Attendance> list = new ArrayList<>();
        for (Document doc : attendance.find(Filters.eq("date", date.toString()))
                .sort(new Document("employee_name", 1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public List<Attendance> findAll() {
        List<Document> docs = new ArrayList<>();
        for (Document doc : attendance.find().sort(new Document("date", -1))) {
            docs.add(doc);
        }
        return mapRows(docs);
    }

    public Attendance findByUserAndDate(int userId, LocalDate date) {
        Document doc = attendance.find(Filters.and(
                Filters.eq("user_id", userId),
                Filters.eq("date", date.toString()))).first();
        return doc != null ? mapRow(doc) : null;
    }

    public int countPresentOn(LocalDate date) {
        return (int) attendance.countDocuments(Filters.and(
                Filters.eq("date", date.toString()),
                Filters.in("status", Attendance.STATUS_PRESENT, Attendance.STATUS_HALF_DAY)));
    }

    public double monthlyHours(int userId, String yearMonth) {
        double hours = 0;
        for (Document doc : attendance.find(Filters.eq("user_id", userId))) {
            String date = doc.getString("date");
            if (date != null && date.startsWith(yearMonth)) {
                LocalTime in = parseTime(doc.getString("check_in_time"));
                LocalTime out = parseTime(doc.getString("check_out_time"));
                if (in != null && out != null) {
                    hours += java.time.Duration.between(in, out).toMinutes() / 60.0;
                }
            }
        }
        return Math.round(hours * 100.0) / 100.0;
    }

    public int countForMonth(String yearMonth) {
        int count = 0;
        for (Document doc : attendance.find()) {
            String date = doc.getString("date");
            if (date != null && date.startsWith(yearMonth)) count++;
        }
        return count;
    }

    private List<Attendance> mapRows(List<Document> docs) {
        Map<Integer, String> names = new HashMap<>();
        for (Document doc : docs) {
            Integer uid = doc.getInteger("user_id");
            String empName = doc.getString("employee_name");
            if (uid != null && (empName == null || empName.isEmpty())) {
                names.putIfAbsent(uid, loadEmployeeName(uid));
            }
        }
        List<Attendance> list = new ArrayList<>();
        for (Document doc : docs) {
            Attendance a = mapRow(doc);
            Integer uid = doc.getInteger("user_id");
            String empName = doc.getString("employee_name");
            if ((empName == null || empName.isEmpty()) && uid != null && names.containsKey(uid)) {
                a.setEmployeeName(names.get(uid));
            }
            list.add(a);
        }
        return list;
    }

    private String loadEmployeeName(int userId) {
        Document u = db.getCollection("users").find(Filters.eq("_id", userId)).first();
        return u != null ? u.getString("full_name") : "";
    }

    private Attendance mapRow(Document doc) {
        Attendance a = new Attendance();
        a.setId(doc.getInteger("_id"));
        a.setUserId(doc.getInteger("user_id") != null ? doc.getInteger("user_id") : 0);
        a.setEmployeeName(doc.getString("employee_name"));
        String date = doc.getString("date");
        if (date != null) {
            try { a.setDate(LocalDate.parse(date)); } catch (Exception ignored) {}
        }
        a.setCheckInTime(parseTime(doc.getString("check_in_time")));
        a.setCheckOutTime(parseTime(doc.getString("check_out_time")));
        a.setStatus(doc.getString("status") != null ? doc.getString("status") : Attendance.STATUS_PRESENT);
        a.setNotes(doc.getString("notes"));
        return a;
    }

    private LocalTime parseTime(String value) {
        if (value == null) return null;
        try { return LocalTime.parse(value); } catch (Exception ignored) {}
        return null;
    }
}
