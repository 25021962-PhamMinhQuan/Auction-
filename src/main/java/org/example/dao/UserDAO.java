
package org.example.dao;

import org.example.model.user.Admin;
import org.example.model.user.Bidder;
import org.example.model.user.Seller;
import org.example.model.user.User;

import java.sql.*;

import static org.example.dao.DBConnection.getConnection;

public class UserDAO {
    //Cloud chạy chậm ác nên khi nào nộp bài mới nên dùng
    //private Connection getConnection() throws SQLException {
    //    String url = "jdbc:mysql://bft2owl42sz3v5srsfqb-mysql.services.clever-cloud.com:3306/bft2owl42sz3v5srsfqb?useSSL=true&requireSSL=true";
    //    return DriverManager.getConnection(url, "usb9ok88u4klv3pr", "KYgtktFg5ixHskW5l2Ez");
    //}
    public void save(User user){
        String sqlInsert = "INSERT INTO ACCOUNT (id,username,password,role) VALUES (?,?,?,?)";
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
    public User findByUsername(String name) {
        String sql = "SELECT * FROM ACCOUNT WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if (role.equals("ADMIN")) {
                    return new Admin(rs.getString("id"), rs.getString("username"), rs.getString("password"));
                } else if (role.equals("SELLER")) {
                    return new Seller(rs.getString("id"), rs.getString("username"), rs.getString("password"));
                } else if (role.equals("BIDDER")) {
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


