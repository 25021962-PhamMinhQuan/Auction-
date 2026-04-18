package org.example.repository;

import org.example.model.auction.BidTransaction;

public interface BidRepository {
    void save(BidTransaction bids, int auctionId);
}
