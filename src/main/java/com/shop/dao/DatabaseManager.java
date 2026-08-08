package com.shop.dao;

import com.shop.model.Role;
import com.shop.util.PasswordUtil;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:shopdata.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        createTables();
        seedDefaultAdmin();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Enable foreign keys for SQLite
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    private void createTables() {
        String[] tables = {
            // Users table
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                full_name TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('ADMIN','MANAGER','CASHIER')),
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """,
            // Products table
            """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                barcode TEXT UNIQUE,
                category TEXT DEFAULT '',
                buy_price REAL NOT NULL DEFAULT 0,
                sell_price REAL NOT NULL DEFAULT 0,
                quantity INTEGER NOT NULL DEFAULT 0,
                min_stock_level INTEGER NOT NULL DEFAULT 10,
                supplier_id INTEGER DEFAULT 0,
                FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
            )
            """,
            // Customers table
            """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT DEFAULT '',
                email TEXT DEFAULT '',
                address TEXT DEFAULT '',
                loyalty_points INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """,
            // Suppliers table
            """
            CREATE TABLE IF NOT EXISTS suppliers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                company_name TEXT NOT NULL,
                contact_person TEXT DEFAULT '',
                phone TEXT DEFAULT '',
                email TEXT DEFAULT '',
                address TEXT DEFAULT ''
            )
            """,
            // Sales table
            """
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_number TEXT NOT NULL UNIQUE,
                customer_id INTEGER DEFAULT 0,
                user_id INTEGER NOT NULL,
                subtotal REAL NOT NULL DEFAULT 0,
                discount_amount REAL NOT NULL DEFAULT 0,
                tax REAL NOT NULL DEFAULT 0,
                total REAL NOT NULL DEFAULT 0,
                payment_method TEXT NOT NULL DEFAULT 'Cash',
                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            // Sale items table
            """
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unit_price REAL NOT NULL,
                discount REAL NOT NULL DEFAULT 0,
                total REAL NOT NULL,
                FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """,
            // Discounts table
            """
            CREATE TABLE IF NOT EXISTS discounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL UNIQUE,
                description TEXT DEFAULT '',
                type TEXT NOT NULL CHECK(type IN ('PERCENTAGE','FLAT')),
                value REAL NOT NULL DEFAULT 0,
                min_purchase REAL NOT NULL DEFAULT 0,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1
            )
            """
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedDefaultAdmin() {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        String insertSql = """
            INSERT INTO users (username, password_hash, full_name, role, active)
            VALUES (?, ?, ?, ?, 1)
            """;

        try (Connection conn = getConnection()) {
            // Check if admin already exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Admin already exists
                }
            }

            // Create default admin
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "admin");
                pstmt.setString(2, PasswordUtil.hash("admin123"));
                pstmt.setString(3, "System Administrator");
                pstmt.setString(4, Role.ADMIN.name());
                pstmt.executeUpdate();
                System.out.println("Default admin user created (admin / admin123)");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding admin: " + e.getMessage());
        }
    }
}
