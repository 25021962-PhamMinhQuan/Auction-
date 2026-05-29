package org.example.repository.impl;

import org.example.domain.auction.BidTransaction;
import org.example.repository.BidRepository;

import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.example.repository.impl.DBConnection.getConnection;

public class BidDAO implements BidRepository {

    @Override
    public void save(BidTransaction bids, int auctionId) {
        String sqlINSERT = "insert into bid_transaction (auction_id,bidder_id,amount,time,type) values (?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlINSERT)) {
            pstmt.setInt(1, auctionId);
            pstmt.setString(2, bids.getBidder().getId());
            pstmt.setDouble(3, bids.getAmount());
            pstmt.setObject(4, bids.getTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
                    .atZone(ZoneId.of("UTC")).toLocalDateTime());
            pstmt.setString(5, bids.getType().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String[]> getBidHistory(int auctionId) {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT a.username, b.amount, b.time " +
                "FROM bid_transaction b " +
                "JOIN account a ON b.bidder_id = a.id " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            ZoneId hcm = ZoneId.of("Asia/Ho_Chi_Minh");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("time");
                String timeStr = ts != null
                        ? ts.toInstant().atZone(hcm).toLocalDateTime().format(fmt)
                        : "";
                result.add(new String[]{
                        rs.getString("username"),
                        rs.getString("amount"),
                        timeStr
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}