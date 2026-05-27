package org.example.repository.impl;
import org.example.domain.auction.Auction;
import org.example.domain.item.Item;
import org.example.factory.ItemFactory;
import org.example.repository.AuctionRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.example.repository.impl.DBConnection.getConnection;

public class AuctionDAO implements AuctionRepository {
    @Override
    public void save(Auction auction, String status) {
        String sqlINSERT = "insert into auction (current_price,start_time,end_time,status,item_id) values (?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlINSERT, Statement.RETURN_GENERATED_KEYS);) {
            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setObject(2, auction.getItem().getStartTime());
            pstmt.setObject(3, auction.getItem().getEndTime());
            pstmt.setString(4, status);
            pstmt.setString(5, auction.getItem().getId());
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
    public void update(Auction auction, String status) {
        String sqlUPDATE = "update auction set current_price = ?,highest_bidder_id = ?,status = ? where id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlUPDATE);) {

            pstmt.setDouble(1, auction.getCurrentPrice());
            if (auction.getHighestBidder() == null) {
                throw new IllegalStateException("Cannot update auction without highest bidder");
            }
            pstmt.setString(2, auction.getHighestBidder().getId());
            pstmt.setString(3, status);
            pstmt.setInt(4, auction.getId());

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

    @Override
    public List<Auction> findByStatus(String status) {
        String sql = "select a.id, a.current_price, a.status, " +
                "i.id as item_id, i.name, i.description, i.start_price, i.type, i.image_url, " +
                "i.start_time, i.end_time " +
                "from auction a join item i on a.item_id = i.id " +
                "where a.status = ?";
        List<Auction> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(parseAuction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public List<Auction> findByType(String type) {
        List<Auction> result = new ArrayList<>();
        String sql = "SELECT a.id, a.current_price, a.status, " +
                "i.id as item_id, i.name, i.description, i.start_price, i.type, i.image_url, " +
                "i.start_time, i.end_time " +
                "FROM auction a JOIN item i ON a.item_id = i.id " +
                "WHERE UPPER(i.type) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(parseAuction(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<Auction> findByName(String keyword) {
        List<Auction> result = new ArrayList<>();
        String sql =
                "SELECT a.id, a.current_price, a.status, " +
                        "i.id as item_id, i.name, i.description, i.start_price, i.type, i.image_url, " +
                        "i.start_time, i.end_time " +
                        "FROM auction a JOIN item i ON a.item_id = i.id " +
                        "WHERE i.name ILIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // parse giống hệt findByStatus()
                result.add(parseAuction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Auction parseAuction(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");

        LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : null;
        LocalDateTime endTime = endTs != null ? endTs.toLocalDateTime() : null;

        Item item = ItemFactory.createItemFromDAO(
                rs.getString("type"),
                rs.getString("item_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("start_price"),
                startTime,
                endTime,
                rs.getString("image_url")
        );

        item.setCurrentPrice(rs.getDouble("current_price"));

        Auction auction = new Auction(item);
        auction.setId(rs.getInt("id"));
        String dbStatus = rs.getString("status");
        switch (dbStatus) {
            case "RUNNING" -> auction.start();
            case "FINISHED" -> {
                auction.start();
                auction.finish();
            }
            case "CANCELED" -> auction.cancel();
            case "PAID" -> {
                auction.start();
                auction.finish();
                auction.markPaid();
            }
        }

            return auction;
        }
    }




