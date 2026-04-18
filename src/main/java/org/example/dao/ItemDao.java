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

        switch (type) {
            case "ELECTRONICS":
                return new Electronics(id, name, description, startPrice, null, null);
            case "ART":
                return new Art(id, name, description, startPrice, null, null);
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    @Override
    public void save(Item item,String seller_id){
        String sqlINSERT = "INSERT INTO ITEM (ID, NAME, DESCRIPTION, START_PRICE, TYPE, SELLER_ID) VALUES (?,?,?,?,?,?)";
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
        String sqlSELECT = "SELECT * FROM ITEM WHERE ID = ?";
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
        String sqlSELECT = "SELECT * FROM ITEM WHERE SELLER_ID = ?";
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
        String sqlUPDATE = "UPDATE ITEM SET name=?, description=?, start_price=? WHERE id=?";
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
        String sqlUPDATE = "UPDATE ITEM SET STATUS = ? WHERE ID = ?";
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
        String sqlDELETE = "DELETE FROM ITEM WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlDELETE)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
