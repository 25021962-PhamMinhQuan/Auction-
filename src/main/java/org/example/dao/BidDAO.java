package org.example.dao;
import org.example.model.auction.BidTransaction;
import org.example.model.auction.Auction;
import org.example.model.user.Bidder;

import java.time.LocalDateTime;
import java.sql.*;

import static org.example.dao.DBConnection.getConnection;

public class BidDAO {
    public void save(Auction auction){
        String sqlINSERT = "INSERT INTO BID_TRANSACTION (auction_id,bidder_id,amount,time) VALUES (?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);){
            pstmt.setInt(1,auction.getId());
            pstmt.setString(2,auction.getHighestBidder().getId());
            pstmt.setDouble(3,auction.getCurrentPrice());
            pstmt.setObject(4,LocalDateTime.now());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
