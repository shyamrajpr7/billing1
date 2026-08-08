package com.shop.dao;

import com.shop.model.Discount;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiscountDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public List<Discount> findAll() {
        List<Discount> discounts = new ArrayList<>();
        String sql = "SELECT * FROM discounts ORDER BY end_date DESC";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                discounts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return discounts;
    }

    public Discount findByCode(String code) {
        String sql = "SELECT * FROM discounts WHERE UPPER(code) = UPPER(?) AND active = 1";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(Discount discount) {
        String sql = """
            INSERT INTO discounts (code, description, type, value, min_purchase, start_date, end_date, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, discount.getCode().toUpperCase());
            pstmt.setString(2, discount.getDescription());
            pstmt.setString(3, discount.getType());
            pstmt.setDouble(4, discount.getValue());
            pstmt.setDouble(5, discount.getMinPurchase());
            pstmt.setString(6, discount.getStartDate().toString());
            pstmt.setString(7, discount.getEndDate().toString());
            pstmt.setInt(8, discount.isActive() ? 1 : 0);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) discount.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Discount discount) {
        String sql = """
            UPDATE discounts SET code=?, description=?, type=?, value=?, min_purchase=?,
            start_date=?, end_date=?, active=? WHERE id=?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discount.getCode().toUpperCase());
            pstmt.setString(2, discount.getDescription());
            pstmt.setString(3, discount.getType());
            pstmt.setDouble(4, discount.getValue());
            pstmt.setDouble(5, discount.getMinPurchase());
            pstmt.setString(6, discount.getStartDate().toString());
            pstmt.setString(7, discount.getEndDate().toString());
            pstmt.setInt(8, discount.isActive() ? 1 : 0);
            pstmt.setInt(9, discount.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM discounts WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Discount mapRow(ResultSet rs) throws SQLException {
        Discount d = new Discount();
        d.setId(rs.getInt("id"));
        d.setCode(rs.getString("code"));
        d.setDescription(rs.getString("description"));
        d.setType(rs.getString("type"));
        d.setValue(rs.getDouble("value"));
        d.setMinPurchase(rs.getDouble("min_purchase"));
        d.setStartDate(LocalDate.parse(rs.getString("start_date")));
        d.setEndDate(LocalDate.parse(rs.getString("end_date")));
        d.setActive(rs.getInt("active") == 1);
        return d;
    }
}
