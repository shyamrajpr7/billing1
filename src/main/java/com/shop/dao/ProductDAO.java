package com.shop.dao;

import com.shop.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = """
            SELECT p.*, COALESCE(s.company_name, '') as supplier_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            ORDER BY p.name
            """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Product> search(String query) {
        List<Product> products = new ArrayList<>();
        String sql = """
            SELECT p.*, COALESCE(s.company_name, '') as supplier_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            WHERE LOWER(p.name) LIKE ? OR p.barcode LIKE ? OR LOWER(p.category) LIKE ?
            ORDER BY p.name
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + query.toLowerCase() + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public Product findById(int id) {
        String sql = """
            SELECT p.*, COALESCE(s.company_name, '') as supplier_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            WHERE p.id = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Product findByBarcode(String barcode) {
        String sql = """
            SELECT p.*, COALESCE(s.company_name, '') as supplier_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            WHERE p.barcode = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(Product product) {
        if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
            product.setBarcode(generateBarcode());
        }
        String sql = """
            INSERT INTO products (name, barcode, category, buy_price, sell_price, quantity, min_stock_level, supplier_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getBarcode());
            pstmt.setString(3, product.getCategory());
            pstmt.setDouble(4, product.getBuyPrice());
            pstmt.setDouble(5, product.getSellPrice());
            pstmt.setInt(6, product.getQuantity());
            pstmt.setInt(7, product.getMinStockLevel());
            pstmt.setInt(8, product.getSupplierId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) product.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Product product) {
        String sql = """
            UPDATE products SET name=?, barcode=?, category=?, buy_price=?, sell_price=?,
            quantity=?, min_stock_level=?, supplier_id=? WHERE id=?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getBarcode());
            pstmt.setString(3, product.getCategory());
            pstmt.setDouble(4, product.getBuyPrice());
            pstmt.setDouble(5, product.getSellPrice());
            pstmt.setInt(6, product.getQuantity());
            pstmt.setInt(7, product.getMinStockLevel());
            pstmt.setInt(8, product.getSupplierId());
            pstmt.setInt(9, product.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateQuantity(int productId, int quantityChange) {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ? AND quantity + ? >= 0";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantityChange);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantityChange);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countLowStock() {
        String sql = "SELECT COUNT(*) FROM products WHERE quantity <= min_stock_level";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Product> findLowStock() {
        List<Product> products = new ArrayList<>();
        String sql = """
            SELECT p.*, COALESCE(s.company_name, '') as supplier_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            WHERE p.quantity <= p.min_stock_level
            ORDER BY p.quantity ASC
            """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    private String generateBarcode() {
        return "P" + UUID.randomUUID().toString().substring(0, 12).toUpperCase().replace("-", "");
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setBarcode(rs.getString("barcode"));
        p.setCategory(rs.getString("category"));
        p.setBuyPrice(rs.getDouble("buy_price"));
        p.setSellPrice(rs.getDouble("sell_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        p.setSupplierId(rs.getInt("supplier_id"));
        try {
            p.setSupplierName(rs.getString("supplier_name"));
        } catch (SQLException ignored) {}
        return p;
    }
}
