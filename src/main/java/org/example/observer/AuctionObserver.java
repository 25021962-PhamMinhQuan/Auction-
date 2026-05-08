package org.example.observer;

import org.example.domain.auction.BidTransaction;
import org.example.domain.auction.Auction;

public interface AuctionObserver {
    void update(Auction auction, BidTransaction bid, double MinIncreament);
}

