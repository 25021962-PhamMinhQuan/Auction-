package org.example.repository.impl;
import org.example.domain.auction.BidTransaction;
import org.example.repository.BidRepository;

import java.sql.*;

import static org.example.repository.impl.DBConnection.getConnection;

public class BidDAO implements BidRepository {
    @Override
    public void save(BidTransaction bids, int auctionId){
        String sqlINSERT = "insert into bid_transaction (auction_id,bidder_id,amount,time,type) values (?,?,?,?,?)";
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
