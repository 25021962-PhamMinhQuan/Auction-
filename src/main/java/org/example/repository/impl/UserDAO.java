
package org.example.repository.impl;

import org.example.domain.user.Admin;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.repository.UserRepository;

import java.sql.*;

import static org.example.repository.impl.DBConnection.getConnection;

public class UserDAO implements UserRepository {


    @Override
    public void save(User user){
        String sqlInsert = "insert into account (id,username,password,role) values (?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlInsert);){
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.executeUpdate();
        }
        catch (SQLException e){
            System.out.println(e);
            e.printStackTrace();
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
                String id = rs.getString("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");
                String fullName  = rs.getString("full_name");
                String email     = rs.getString("email");
                String phone     = rs.getString("phone");
                String avatarUrl = rs.getString("avatar_url");
                switch (User.UserRole.valueOf(role)) {
                    case ADMIN:
                        return new Admin(id, username, password, fullName, email, phone, avatarUrl);

                    case SELLER:
                        return new Seller(id, username, password, fullName, email, phone, avatarUrl);

                    case BIDDER:
                        return new Bidder(id, username, password, fullName, email, phone, avatarUrl);

                    default:
                        throw new IllegalArgumentException("Unknown role: " + role);
                }
            }
            // tao ra 1 bien user tam thoi de gui no di
        }
        catch (SQLException e){
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
                // map giống findByUsername
            }
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
    }
    @Override
    public void updatePassword(String userId, String hashedPassword) {
        String sql = "UPDATE account SET password=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}


