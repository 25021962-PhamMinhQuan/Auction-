package org.example.repository;

import org.example.domain.auction.Auction;

import java.util.List;

public interface AuctionRepository {
    void save(Auction auction, String status);

    void update(Auction auction, String status);

    void updateStatus(Auction auction, String status);

    List<Auction> findByStatus(String status);

    void updateEndTime(Auction auction);

    void updateScheduleAndPrice(Auction auction);

    List<Auction> findByName(String keyword);

    public List<Auction> findByType(String type);
    List<Auction> findAll();

    Auction findById(int id);

    Auction findByItemId(String itemId);

    List<Auction> findBySellerId(String sellerId);

    List<Auction> findWonByBidderId(String bidderId);

    void delete(int id);
}
