package org.example.repository.impl;
import org.example.domain.auction.Auction;
import org.example.repository.AuctionRepository;

import java.sql.*;

import static org.example.repository.impl.DBConnection.getConnection;

public class AuctionDAO implements AuctionRepository {
    @Override
    public void save(Auction auction,String status){
        String sqlINSERT = "insert into auction (current_price,start_time,end_time,status,item_id) values (?,?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT, Statement.RETURN_GENERATED_KEYS);){
            pstmt.setDouble(1,auction.getCurrentPrice());
            pstmt.setObject(2,auction.getItem().getStartTime());
            pstmt.setObject(3,auction.getItem().getEndTime());
            pstmt.setString(4,status);
            pstmt.setString(5,auction.getItem().getId());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                auction.setId(id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void update(Auction auction,String status){
        String sqlUPDATE = "update auction set current_price = ?,highest_bidder_id = ?,status = ? where id = ?";

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE);){

            pstmt.setDouble(1,auction.getCurrentPrice());
            pstmt.setString(2,auction.getHighestBidder().getId());
            pstmt.setString(3,status);
            pstmt.setInt(4,auction.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // chỉ dùng khi cancel hoặc paid
    @Override
    public void updateStatus(Auction auction, String status) {
        String sql = "update auction set status=? where id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, auction.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

