package org.example.dao;
import org.example.model.user.Bidder;
import org.example.repository.AutoBidRepository;
import org.example.util.AutoBid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.example.dao.DBConnection.getConnection;

public class AutoBidDao implements AutoBidRepository {
    @Override
    public void save(AutoBid autoBid, int auctionId) {
        String sqlINSERT = "INSERT INTO AUTO_BID (AUCTION_ID, BIDDER_ID, MAX_BID, INCREMENT_STEP, REGISTERED_AT) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlINSERT);) {
            pstmt.setInt(1, auctionId);
            pstmt.setString(2, autoBid.getBidder().getId());
            pstmt.setDouble(3, autoBid.getMaxBid());
            pstmt.setDouble(4, autoBid.getIncrement());
            pstmt.setObject(5, autoBid.getTime());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // dung khi server loi va phai restart lại từ đầu
    @Override
    public PriorityQueue<AutoBid> findActiveByAuction(int auctionId){
        String sql = "SELECT * FROM AUTO_BID WHERE AUCTION_ID = ? AND IS_ACTIVE IS TRUE";
        PriorityQueue<AutoBid> result = new PriorityQueue<>();
        result = new PriorityQueue<>(
                (a, b) -> {
                    int logic = Double.compare(b.getMaxBid(), a.getMaxBid());
                    if(logic == 0){
                        return a.getTime().compareTo(b.getTime());
                    }
                    return logic;
                }
        );
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                Bidder bidder = new Bidder(rs.getString("BIDDER_ID"),"","");
                result.add(new AutoBid(bidder, rs.getDouble("MAX_BID"), rs.getDouble("INCREMENT_STEP")));

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void deactivateByAuction(int auctionId){
        String sqlUPDATE = "UPDATE AUTO_BID SET IS_ACTIVE = FALSE WHERE AUCTION_ID = ?";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE)){
            pstmt.setInt(1,auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
