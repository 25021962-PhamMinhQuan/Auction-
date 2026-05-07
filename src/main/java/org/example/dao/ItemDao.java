package org.example.dao;

import org.example.factory.ItemFactory;
import org.example.model.item.Art;
import org.example.model.item.Electronics;
import org.example.model.item.Item;
import org.example.repository.ItemRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.example.dao.DBConnection.getConnection;

public class ItemDao implements ItemRepository {

    private Item buildItem(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startPrice = rs.getDouble("start_price");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

        return ItemFactory.createItemFromDAO(type, id, name, description, startPrice, startTime, endTime);
    }

    @Override
    public void save(Item item,String seller_id){
        String sqlINSERT = "insert into item (id, name, description, start_price, type, seller_id) values (?,?,?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);){
            pstmt.setString(1,item.getId());
            pstmt.setString(2,item.getName());
            pstmt.setString(3,item.getDescription());
            pstmt.setDouble(4,item.getStartPrice());
            pstmt.setString(5,item.getType());
            pstmt.setString(6,seller_id);
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
}
