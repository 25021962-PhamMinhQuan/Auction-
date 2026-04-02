package org.example.dao;
import org.example.model.auction.BidTransaction;
import org.example.model.auction.auction;
import org.example.model.user.Bidder;

import java.sql.*;
import java.time.LocalDateTime;

import static org.example.dao.DBConnection.getConnection;

public class AuctionDAO {
    public void save(auction Auction,String status){
        String sqlINSERT = "INSERT INTO AUCTION (current_price,start_time,end_time,status) VALUES (?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT, Statement.RETURN_GENERATED_KEYS);){
            pstmt.setDouble(1,Auction.getCurrentPrice());
            pstmt.setObject(2,Auction.getItem().getStartTime());
            pstmt.setObject(3,Auction.getItem().getEndTime());
            pstmt.setString(4,status);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                Auction.setId(id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public void update(auction Auction,String status){
        String sqlUPDATE = "UPDATE AUCTION SET current_price = ?,highest_bidder_id = ?,status = ? WHERE id = ?";

        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE);){

            pstmt.setDouble(1,Auction.getCurrentPrice());
            pstmt.setString(2,Auction.getHighestBidder().getId());
            pstmt.setString(3,status);
            pstmt.setInt(4,Auction.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

