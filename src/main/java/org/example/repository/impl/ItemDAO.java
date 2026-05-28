package org.example.repository.impl;

import org.example.factory.ItemFactory;
import org.example.domain.item.Item;
import org.example.repository.ItemRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.example.repository.impl.DBConnection.getConnection;

public class ItemDAO implements ItemRepository {

    private Item buildItem(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startPrice = rs.getDouble("start_price");
        String imageUrl    = rs.getString("image_url");
        java.sql.Timestamp startTs = rs.getTimestamp("start_time");
        java.sql.Timestamp endTs   = rs.getTimestamp("end_time");
        LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : null;
        LocalDateTime endTime   = endTs   != null ? endTs.toLocalDateTime()   : null;

        return ItemFactory.createItemFromDAO(type, id, name, description, startPrice, startTime, endTime,imageUrl);
    }

    @Override
    public void save(Item item,String seller_id){
        String sqlINSERT = "INSERT INTO item (id, name, description, start_price, type, " + "seller_id, start_time, end_time, image_url) VALUES (?,?,?,?,?,?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);){
            pstmt.setString(1,item.getId());
            pstmt.setString(2,item.getName());
            pstmt.setString(3,item.getDescription());
            pstmt.setDouble(4,item.getStartPrice());
            pstmt.setString(5,item.getType());
            pstmt.setString(6,seller_id);
            pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(item.getStartTime()));
            pstmt.setTimestamp(8, java.sql.Timestamp.valueOf(item.getEndTime()));
            pstmt.setString(9, item.getImageUrl());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Item findById(String id){
        String sqlSELECT = "select * from item where id = ?";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlSELECT);){
            pstmt.setString(1,id);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return buildItem(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<Item> findBySeller(String sellerId){
        String sqlSELECT = "select * from item where seller_id = ?";
        List<Item> items = new ArrayList<>();
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlSELECT);){
            pstmt.setString(1,sellerId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                items.add(buildItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    @Override
    public void update(Item item) {
        String sqlUPDATE = "update item set name=?, description=?, start_price=? where id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStartPrice());
            pstmt.setString(4, item.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStatus(Item item,String status){
        String sqlUPDATE = "update item set status = ? where id = ?";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE)){
            pstmt.setString(1,status);
            pstmt.setString(2,item.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String id) {
        String sqlDELETE = "delete from item where id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlDELETE)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public List<Item> findAll() {
        String sql = "SELECT * FROM item ORDER BY start_time DESC";
        List<Item> items = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                items.add(buildItem(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return items;
    }
}
