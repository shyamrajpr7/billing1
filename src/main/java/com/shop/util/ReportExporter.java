package com.shop.util;

import com.shop.model.Expense;
import com.shop.model.Sale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReportExporter {

    private ReportExporter() {
    }

    // ---------- Sales CSV ----------

    public static void exportSalesCsv(List<Sale> sales, File file) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeRow(w, "Invoice #", "Date & Time", "Customer", "Cashier", "Payment",
                    "Subtotal", "Discount", "Tax", "Total");
            for (Sale s : sales) {
                writeRow(w, s.getInvoiceNumber(), s.getFormattedDate(), s.getCustomerName(),
                        s.getUserName(), s.getPaymentMethod(),
                        fmt(s.getSubtotal()), fmt(s.getDiscountAmount()), fmt(s.getTax()), fmt(s.getTotal()));
            }
        }
    }

    // ---------- Expenses CSV ----------

    public static void exportExpensesCsv(List<Expense> expenses, File file) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeRow(w, "ID", "Date", "Description", "Category", "Amount");
            for (Expense e : expenses) {
                writeRow(w, String.valueOf(e.getId()), String.valueOf(e.getExpenseDate()),
                        e.getDescription(), e.getCategory(), fmt(e.getAmount()));
            }
        }
    }

    // ---------- Sales Excel (.xlsx) ----------

    public static void exportSalesExcel(List<Sale> sales, File file) throws IOException {
        String[][] data = new String[sales.size() + 1][9];
        data[0] = new String[]{"Invoice #", "Date & Time", "Customer", "Cashier", "Payment",
                "Subtotal", "Discount", "Tax", "Total"};
        for (int i = 0; i < sales.size(); i++) {
            Sale s = sales.get(i);
            data[i + 1] = new String[]{s.getInvoiceNumber(), s.getFormattedDate(), s.getCustomerName(),
                    s.getUserName(), s.getPaymentMethod(),
                    fmt(s.getSubtotal()), fmt(s.getDiscountAmount()), fmt(s.getTax()), fmt(s.getTotal())};
        }
        writeXlsx(data, file, "Sales Report");
    }

    // ---------- Expenses Excel (.xlsx) ----------

    public static void exportExpensesExcel(List<Expense> expenses, File file) throws IOException {
        String[][] data = new String[expenses.size() + 1][5];
        data[0] = new String[]{"ID", "Date", "Description", "Category", "Amount"};
        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            data[i + 1] = new String[]{String.valueOf(e.getId()), String.valueOf(e.getExpenseDate()),
                    e.getDescription(), e.getCategory(), fmt(e.getAmount())};
        }
        writeXlsx(data, file, "Expense Report");
    }

    // ---------- Helpers ----------

    private static void writeRow(Writer w, String... cells) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            String cell = cells[i] == null ? "" : cells[i];
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
                sb.append('"').append(cell.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(cell);
            }
        }
        sb.append('\n');
        w.write(sb.toString());
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static String xmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void writeXlsx(String[][] data, File file, String sheetTitle) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            putEntry(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>");

            putEntry(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>");

            putEntry(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"" + xmlEsc(sheetTitle) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                    "</workbook>");

            putEntry(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "</Relationships>");

            StringBuilder sheet = new StringBuilder();
            sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            sheet.append("<sheetData>");
            for (String[] row : data) {
                sheet.append("<row>");
                for (String cell : row) {
                    sheet.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(xmlEsc(cell))
                            .append("</t></is></c>");
                }
                sheet.append("</row>");
            }
            sheet.append("</sheetData></worksheet>");

            putEntry(zos, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    private static void putEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
