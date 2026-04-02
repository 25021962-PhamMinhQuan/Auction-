package org.example.dao;
import org.example.model.auction.BidTransaction;
import org.example.model.auction.auction;
import org.example.model.user.Bidder;

import java.time.LocalDateTime;
import java.sql.*;

import static org.example.dao.DBConnection.getConnection;

public class BidDAO {
    public void save(auction Auction){
        String sqlINSERT = "INSERT INTO BID_TRANSACTION (auction_id,bidder_id,amount,time) VALUES (?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);){
            pstmt.setInt(1,Auction.getId());
            pstmt.setString(2,Auction.getHighestBidder().getId());
            pstmt.setDouble(3,Auction.getCurrentPrice());
            pstmt.setObject(4,LocalDateTime.now());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
