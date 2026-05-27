package org.example.repository;

import org.example.domain.auction.Auction;

import java.util.List;

public interface AuctionRepository {
    void save(Auction auction, String status);

    void update(Auction auction, String status);

    void updateStatus(Auction auction, String status);

    List<Auction> findByStatus(String status);

    List<Auction> findByName(String keyword);

    public List<Auction> findByType(String type);
}
