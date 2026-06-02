
package org.example.repository.impl;

import org.example.domain.user.Admin;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.repository.UserRepository;

import java.sql.*;

import static org.example.repository.impl.DBConnection.getConnection;

public class UserDAO implements UserRepository {

    public UserDAO() {
        ensureStatusColumn();
    }

    private void ensureStatusColumn() {
        String sql = "ALTER TABLE account ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Could not ensure account.status column: " + e.getMessage());
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role = rs.getString("role");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String avatarUrl = rs.getString("avatar_url");
        String status;
        try { status = rs.getString("status"); } catch (SQLException e) { status = "ACTIVE"; }
        double balance;
        try { balance = rs.getDouble("balance"); } catch (SQLException e) { balance = 1_000_000_000.0; }

        User user = switch (User.UserRole.valueOf(role)) {
            case ADMIN -> new Admin(id, username, password, fullName, email, phone, avatarUrl);
            case SELLER -> new Seller(id, username, password, fullName, email, phone, avatarUrl);
            case BIDDER -> new Bidder(id, username, password, fullName, email, phone, avatarUrl);
        };

        user.setBalance(balance);
        user.setStatus(status);
        return user;
    }

    @Override
    public void save(User user) {
        String sqlInsert = "insert into account (id,username,password,role) values (?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlInsert);) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    @Override
    public void updateBalance(String userId, double newBalance) {
        String sql = "UPDATE account SET balance=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance); stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateStatus(String userId, String status) {
        String sql = "UPDATE account SET status=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User findByUsername(String name) {
        String sql = "select * from account where username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User findById(String id) {
        String sql = "SELECT * FROM account WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateProfile(User user) {
        String sql = "UPDATE account SET full_name=?, email=?, phone=?, avatar_url=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getAvatarUrl());
            stmt.setString(5, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePassword(String userId, String hashedPassword) {
        String sql = "UPDATE account SET password=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public java.util.List<User> findAll() {
        String sql = "SELECT * FROM account ORDER BY username";
        java.util.List<User> users = new java.util.ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
    @Override
    public void delete(String id) {
        String sql = "DELETE FROM account WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}


