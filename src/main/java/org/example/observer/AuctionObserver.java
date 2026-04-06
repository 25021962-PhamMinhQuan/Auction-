package org.example.observer;

import org.example.model.auction.BidTransaction;
import org.example.model.auction.Auction;

public interface AuctionObserver {
    void update(Auction auction, BidTransaction bid, double MinIncreament);
}

