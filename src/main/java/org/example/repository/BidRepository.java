package org.example.repository;

import org.example.domain.auction.BidTransaction;

public interface BidRepository {
    void save(BidTransaction bids, int auctionId);
}
