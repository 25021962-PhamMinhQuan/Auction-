
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
                String role = rs.getString("role");
                if (role.equals(User.UserRole.ADMIN.name())) {
                    return new Admin(rs.getString("id"), rs.getString("username"), rs.getString("password"));
                } else if (role.equals(User.UserRole.SELLER.name())) {
                    return new Seller(rs.getString("id"), rs.getString("username"), rs.getString("password"));
                } else if (role.equals(User.UserRole.BIDDER.name())) {
                    return new Bidder(rs.getString("id"), rs.getString("username"), rs.getString("password"));

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
}


