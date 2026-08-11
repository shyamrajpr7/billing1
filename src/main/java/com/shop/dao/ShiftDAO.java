package com.shop.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.shop.model.Shift;
import com.shop.model.ShiftReport;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShiftDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final MongoCollection<Document> shifts = db.getCollection("shifts");

    public boolean openShift(Shift shift) {
        try {
            int id = db.nextId("shifts");
            shift.setId(id);
            shift.setStatus(Shift.STATUS_OPEN);
            shift.setOpenedAt(LocalDateTime.now());
            shifts.insertOne(new Document("_id", id)
                    .append("cashier_id", shift.getCashierId())
                    .append("cashier_name", shift.getCashierName())
                    .append("opening_balance", shift.getOpeningBalance())
                    .append("status", shift.getStatus())
                    .append("opened_at", shift.getOpenedAt().toString())
                    .append("closed_at", null)
                    .append("counted_cash", 0.0)
                    .append("expected_cash", 0.0)
                    .append("variance", 0.0)
                    .append("notes", ""));
            new ActivityLogDAO().log("SHIFT_OPEN", shift.getCashierName()
                    + " opened shift with ₹" + String.format("%.2f", shift.getOpeningBalance()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Shift findOpenShift() {
        Document doc = shifts.find(Filters.eq("status", Shift.STATUS_OPEN)).sort(new Document("opened_at", -1)).first();
        return doc != null ? mapRow(doc) : null;
    }

    public List<Shift> findAll() {
        List<Shift> list = new ArrayList<>();
        for (Document doc : shifts.find().sort(new Document("opened_at", -1))) {
            list.add(mapRow(doc));
        }
        return list;
    }

    public boolean closeShift(int shiftId, double countedCash, String notes) {
        try {
            Document doc = shifts.find(Filters.eq("_id", shiftId)).first();
            if (doc == null) return false;
            if (!Shift.STATUS_OPEN.equals(doc.getString("status"))) return false;

            Shift shift = mapRow(doc);
            ShiftReport report = computeReport(shift);
            double expected = report.getExpectedCash();
            double variance = countedCash - expected;

            shifts.updateOne(Filters.eq("_id", shiftId), Updates.combine(
                    Updates.set("status", Shift.STATUS_CLOSED),
                    Updates.set("closed_at", LocalDateTime.now().toString()),
                    Updates.set("counted_cash", countedCash),
                    Updates.set("expected_cash", expected),
                    Updates.set("variance", variance),
                    Updates.set("notes", notes != null ? notes : "")));
            new ActivityLogDAO().log("SHIFT_CLOSE", "Shift #" + shiftId + " closed. Expected ₹"
                    + String.format("%.2f", expected) + ", counted ₹" + String.format("%.2f", countedCash)
                    + " (variance ₹" + String.format("%.2f", variance) + ")");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public ShiftReport computeReport(Shift shift) {
        ShiftReport report = new ShiftReport();
        report.setShiftId(shift.getId());
        report.setCashierName(shift.getCashierName());
        report.setOpenedAtLabel(shift.getOpenedAtLabel());
        report.setClosedAtLabel(shift.getClosedAtLabel());
        report.setOpeningBalance(shift.getOpeningBalance());

        String start = shift.getOpenedAt().toString();

        for (Document sale : db.getCollection("sales").find(Filters.gte("created_at", start))) {
            report.setSalesCount(report.getSalesCount() + 1);
            double total = sale.getDouble("total");
            double giftCard = sale.getDouble("gift_card_amount") != null ? sale.getDouble("gift_card_amount") : 0;
            report.setGiftCardAmount(report.getGiftCardAmount() + giftCard);
            String method = sale.getString("payment_method");
            double cashIn = "Cash".equalsIgnoreCase(method) ? total - giftCard : 0;
            if (cashIn > 0) report.setCashSales(report.getCashSales() + cashIn);
            else if ("Card".equalsIgnoreCase(method)) report.setCardSales(report.getCardSales() + total);
            else if ("UPI".equalsIgnoreCase(method)) report.setUpiSales(report.getUpiSales() + total);
            else if ("Net Banking".equalsIgnoreCase(method)) report.setNetBankingSales(report.getNetBankingSales() + total);
            else if (Boolean.TRUE.equals(sale.getBoolean("credit_sale"))) report.setCreditSales(report.getCreditSales() + total);
        }

        for (Document ret : db.getCollection("returns").find(Filters.gte("created_at", start))) {
            double refund = ret.getDouble("refund_amount") != null ? ret.getDouble("refund_amount") : 0;
            report.setCashRefunds(report.getCashRefunds() + refund);
        }

        String startDate = shift.getOpenedAt().toLocalDate().toString();
        for (Document exp : db.getCollection("expenses").find(Filters.gte("expense_date", startDate))) {
            report.setCashExpenses(report.getCashExpenses() + (exp.getDouble("amount") != null ? exp.getDouble("amount") : 0));
        }

        double expected = report.getOpeningBalance() + report.getCashSales()
                - report.getCashRefunds() - report.getCashExpenses();
        report.setExpectedCash(expected);
        report.setCountedCash(shift.getCountedCash());
        report.setVariance(shift.getVariance());
        return report;
    }

    private Shift mapRow(Document doc) {
        Shift shift = new Shift();
        shift.setId(doc.getInteger("_id"));
        shift.setCashierId(doc.getInteger("cashier_id") != null ? doc.getInteger("cashier_id") : 0);
        shift.setCashierName(doc.getString("cashier_name"));
        shift.setOpeningBalance(doc.getDouble("opening_balance") != null ? doc.getDouble("opening_balance") : 0);
        shift.setStatus(doc.getString("status"));
        shift.setCountedCash(doc.getDouble("counted_cash") != null ? doc.getDouble("counted_cash") : 0);
        shift.setExpectedCash(doc.getDouble("expected_cash") != null ? doc.getDouble("expected_cash") : 0);
        shift.setVariance(doc.getDouble("variance") != null ? doc.getDouble("variance") : 0);
        shift.setNotes(doc.getString("notes"));
        String opened = doc.getString("opened_at");
        if (opened != null) {
            try { shift.setOpenedAt(LocalDateTime.parse(opened)); } catch (Exception ignored) {}
        }
        String closed = doc.getString("closed_at");
        if (closed != null) {
            try { shift.setClosedAt(LocalDateTime.parse(closed)); } catch (Exception ignored) {}
        }
        return shift;
    }
}
