package com.shop.dao;

import com.shop.model.Sale;
import com.shop.model.SaleItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public boolean createSale(Sale sale) {
        String insertSaleSql = """
            INSERT INTO sales (invoice_number, customer_id, user_id, subtotal, discount_amount, tax, total, payment_method)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String insertItemSql = """
            INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, discount, total)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String updateStockSql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false); // Transaction

            // 1. Insert Sale header
            try (PreparedStatement pstmt = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, sale.getInvoiceNumber());
                pstmt.setInt(2, sale.getCustomerId());
                pstmt.setInt(3, sale.getUserId());
                pstmt.setDouble(4, sale.getSubtotal());
                pstmt.setDouble(5, sale.getDiscountAmount());
                pstmt.setDouble(6, sale.getTax());
                pstmt.setDouble(7, sale.getTotal());
                pstmt.setString(8, sale.getPaymentMethod());
                pstmt.executeUpdate();

                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    sale.setId(keys.getInt(1));
                } else {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Insert Sale Items & update stock
            try (PreparedStatement itemPstmt = conn.prepareStatement(insertItemSql);
                 PreparedStatement stockPstmt = conn.prepareStatement(updateStockSql)) {

                for (SaleItem item : sale.getItems()) {
                    item.setSaleId(sale.getId());

                    itemPstmt.setInt(1, item.getSaleId());
                    itemPstmt.setInt(2, item.getProductId());
                    itemPstmt.setString(3, item.getProductName());
                    itemPstmt.setInt(4, item.getQuantity());
                    itemPstmt.setDouble(5, item.getUnitPrice());
                    itemPstmt.setDouble(6, item.getDiscount());
                    itemPstmt.setDouble(7, item.getTotal());
                    itemPstmt.addBatch();

                    stockPstmt.setInt(1, item.getQuantity());
                    stockPstmt.setInt(2, item.getProductId());
                    stockPstmt.addBatch();
                }

                itemPstmt.executeBatch();
                stockPstmt.executeBatch();
            }

            // 3. Add loyalty points if customer selected (1 point per ₹100 spent)
            if (sale.getCustomerId() > 0) {
                int points = (int) (sale.getTotal() / 100);
                if (points > 0) {
                    String updatePointsSql = "UPDATE customers SET loyalty_points = loyalty_points + ? WHERE id = ?";
                    try (PreparedStatement pointsPstmt = conn.prepareStatement(updatePointsSql)) {
                        pointsPstmt.setInt(1, points);
                        pointsPstmt.setInt(2, sale.getCustomerId());
                        pointsPstmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return false;
    }

    public List<Sale> findAll() {
        List<Sale> sales = new ArrayList<>();
        String sql = """
            SELECT s.*, COALESCE(c.name, 'Walk-in Customer') as customer_name, u.full_name as user_name
            FROM sales s
            LEFT JOIN customers c ON s.customer_id = c.id
            JOIN users u ON s.user_id = u.id
            ORDER BY s.created_at DESC
            """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sales.add(mapSaleRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sales;
    }

    public Sale findById(int id) {
        String sql = """
            SELECT s.*, COALESCE(c.name, 'Walk-in Customer') as customer_name, u.full_name as user_name
            FROM sales s
            LEFT JOIN customers c ON s.customer_id = c.id
            JOIN users u ON s.user_id = u.id
            WHERE s.id = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Sale sale = mapSaleRow(rs);
                sale.setItems(findItemsBySaleId(sale.getId()));
                return sale;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SaleItem> findItemsBySaleId(int saleId) {
        List<SaleItem> items = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SaleItem item = new SaleItem();
                item.setId(rs.getInt("id"));
                item.setSaleId(rs.getInt("sale_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getDouble("unit_price"));
                item.setDiscount(rs.getDouble("discount"));
                item.setTotal(rs.getDouble("total"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public double getTotalRevenueToday() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE date(created_at) = date('now', 'localtime')";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getSalesCountToday() {
        String sql = "SELECT COUNT(*) FROM sales WHERE date(created_at) = date('now', 'localtime')";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getTotalRevenueThisMonth() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', 'localtime')";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String generateNextInvoiceNumber() {
        String sql = "SELECT COUNT(*) + 1 FROM sales";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("INV-%06d", rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "INV-" + System.currentTimeMillis();
    }

    private Sale mapSaleRow(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setCustomerId(rs.getInt("customer_id"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setUserId(rs.getInt("user_id"));
        s.setUserName(rs.getString("user_name"));
        s.setSubtotal(rs.getDouble("subtotal"));
        s.setDiscountAmount(rs.getDouble("discount_amount"));
        s.setTax(rs.getDouble("tax"));
        s.setTotal(rs.getDouble("total"));
        s.setPaymentMethod(rs.getString("payment_method"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            s.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }
        return s;
    }
}
