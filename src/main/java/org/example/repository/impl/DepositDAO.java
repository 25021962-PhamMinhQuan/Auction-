package org.example.repository.impl;

import org.example.domain.user.DepositRequest;
import org.example.repository.DepositRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.example.repository.impl.DBConnection.getConnection;

public class DepositDAO implements DepositRepository {

    private DepositRequest mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String userId = rs.getString("user_id");
        String username = rs.getString("username");
        double amount = rs.getDouble("amount");
        String note = rs.getString("note");
        DepositRequest.Status status = DepositRequest.Status.valueOf(rs.getString("status"));
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime() : null;
        LocalDateTime resolvedAt = rs.getTimestamp("resolved_at") != null
                ? rs.getTimestamp("resolved_at").toLocalDateTime() : null;
        return new DepositRequest(id, userId, username, amount, note, status, createdAt, resolvedAt);
    }

    @Override
    public void save(DepositRequest request) {
        String sql = "INSERT INTO deposit_request (user_id, username, amount, note, status, created_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getUserId());
            stmt.setString(2, request.getUsername());
            stmt.setDouble(3, request.getAmount());
            stmt.setString(4, request.getNote());
            stmt.setString(5, DepositRequest.Status.PENDING.name());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DepositRequest> findAll() {
        List<DepositRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM deposit_request ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<DepositRequest> findByUserId(String userId) {
        List<DepositRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM deposit_request WHERE user_id=? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<DepositRequest> findPending() {
        List<DepositRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM deposit_request WHERE status='PENDING' ORDER BY created_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public void approve(int requestId) {
        // Get request info first
        String getSql = "SELECT * FROM deposit_request WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(getSql)) {
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String userId = rs.getString("user_id");
                double amount = rs.getDouble("amount");
                // Update balance
                String updateBalance = "UPDATE account SET balance = balance + ? WHERE id=?";
                try (PreparedStatement upStmt = conn.prepareStatement(updateBalance)) {
                    upStmt.setDouble(1, amount);
                    upStmt.setString(2, userId);
                    upStmt.executeUpdate();
                }
                // Update request status
                String updateReq = "UPDATE deposit_request SET status='APPROVED', resolved_at=? WHERE id=?";
                try (PreparedStatement upStmt = conn.prepareStatement(updateReq)) {
                    upStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    upStmt.setInt(2, requestId);
                    upStmt.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void reject(int requestId) {
        String sql = "UPDATE deposit_request SET status='REJECTED', resolved_at=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
