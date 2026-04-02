package org.example.observer;

import org.example.model.auction.BidTransaction;
import org.example.model.auction.auction;

public interface AuctionObserver {
    void update(auction Auction, BidTransaction bid, double MinIncreament);
}

