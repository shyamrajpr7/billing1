package com.shop.util;

import com.shop.dao.SupplierDAO;
import com.shop.model.Product;
import com.shop.model.Supplier;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvImportUtil {

    public static class ImportResult {
        public final List<Product> products = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public int skipped;
    }

    public static ImportResult parse(Path file) {
        ImportResult result = new ImportResult();
        Map<String, Supplier> suppliersByName = new HashMap<>();
        for (Supplier s : new SupplierDAO().findAll()) {
            suppliersByName.put(s.getCompanyName().toLowerCase(), s);
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(splitCsv(line));
            }
        } catch (IOException e) {
            result.errors.add("Could not read file: " + e.getMessage());
            return result;
        }

        if (rows.isEmpty()) {
            result.errors.add("File is empty.");
            return result;
        }

        boolean hasHeader = isHeaderRow(rows.get(0));
        Map<String, Integer> headerIndex = new HashMap<>();
        if (hasHeader) {
            String[] header = rows.get(0);
            for (int i = 0; i < header.length; i++) {
                headerIndex.put(normalize(header[i]), i);
            }
            rows.remove(0);
        }

        for (int i = 0; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            try {
                String name = val(hasHeader ? get(headerIndex, cols, "name") : getCol(cols, 0));
                String category = val(hasHeader ? get(headerIndex, cols, "category") : getCol(cols, 1));
                String barcode = val(hasHeader ? get(headerIndex, cols, "barcode") : getCol(cols, 2));
                int quantity = (int) parseNumber(
                        hasHeader ? get(headerIndex, cols, "quantity", "stock") : getCol(cols, 3), 0);
                double buyPrice = parseNumber(
                        hasHeader ? get(headerIndex, cols, "buyprice", "costprice", "buy") : getCol(cols, 4), 0);
                double sellPrice = parseNumber(
                        hasHeader ? get(headerIndex, cols, "sellprice", "price", "selling") : getCol(cols, 5), 0);
                int minStock = (int) parseNumber(
                        hasHeader ? get(headerIndex, cols, "minstock", "min") : getCol(cols, 6), 5);
                String supplierName = val(
                        hasHeader ? get(headerIndex, cols, "supplier") : getCol(cols, 7));
                String expiry = val(hasHeader ? get(headerIndex, cols, "expiry", "expirydate") : getCol(cols, 8));

                if (name.isEmpty()) {
                    result.errors.add("Row " + (i + 1) + ": product name is empty.");
                    result.skipped++;
                    continue;
                }
                if (barcode.isEmpty()) barcode = null;

                Product p = new Product();
                p.setName(name);
                p.setCategory(category);
                p.setBarcode(barcode);
                p.setQuantity(quantity);
                p.setBuyPrice(buyPrice);
                p.setSellPrice(sellPrice);
                p.setMinStockLevel(minStock);
                if (!supplierName.isEmpty()) {
                    Supplier s = suppliersByName.get(supplierName.toLowerCase());
                    p.setSupplierId(s != null ? s.getId() : 0);
                    p.setSupplierName(s != null ? s.getCompanyName() : supplierName);
                }
                if (!expiry.isEmpty()) {
                    try {
                        p.setExpiryDate(parseDate(expiry));
                    } catch (Exception ex) {
                        result.errors.add("Row " + (i + 1) + ": invalid expiry date '" + expiry + "'.");
                        result.skipped++;
                        continue;
                    }
                }
                result.products.add(p);
            } catch (NumberFormatException ex) {
                result.errors.add("Row " + (i + 1) + ": invalid numeric value.");
                result.skipped++;
            }
        }
        return result;
    }

    private static boolean isHeaderRow(String[] row) {
        int matched = 0;
        for (String cell : row) {
            String n = normalize(cell);
            if (n.contains("name") || n.contains("category") || n.contains("barcode")
                    || n.contains("quantity") || n.contains("stock") || n.contains("price")
                    || n.contains("supplier")) {
                matched++;
            }
        }
        return matched >= 2;
    }

    private static String get(Map<String, Integer> headerIndex, String[] cols, String... keys) {
        for (String key : keys) {
            Integer idx = headerIndex.get(key);
            if (idx != null && idx < cols.length) return cols[idx];
        }
        return "";
    }

    private static String getCol(String[] cols, int idx) {
        return idx < cols.length ? cols[idx] : "";
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String val(String s) {
        return s == null ? "" : s.trim();
    }

    private static double parseNumber(String s, double fallback) {
        String v = val(s);
        if (v.isEmpty()) return fallback;
        return Double.parseDouble(v.replace(",", "").replace("₹", ""));
    }

    private static LocalDate parseDate(String s) {
        String v = val(s);
        for (String fmt : new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy"}) {
            try {
                return LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unparseable date: " + v);
    }

    private static String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }
}
