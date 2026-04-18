package org.example.dao;
import org.example.model.auction.BidTransaction;
import org.example.model.auction.Auction;
import org.example.model.user.Bidder;
import org.example.repository.BidRepository;

import java.time.LocalDateTime;
import java.sql.*;

import static org.example.dao.DBConnection.getConnection;

public class BidDAO implements BidRepository {
    @Override
    public void save(BidTransaction bids, int auctionId){
        String sqlINSERT = "INSERT INTO BID_TRANSACTION (auction_id,bidder_id,amount,time,type) VALUES (?,?,?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);){
            pstmt.setInt(1,auctionId);
            pstmt.setString(2,bids.getBidder().getId());
            pstmt.setDouble(3,bids.getAmount());
            pstmt.setObject(4, bids.getTime());
            pstmt.setString(5, bids.getType().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
