package org.example.repository;

import org.example.domain.auction.Auction;

public interface AuctionRepository {
    void save(Auction auction, String status);
    void update(Auction auction,String status);
    void updateStatus(Auction auction, String status);
}
