package org.example.observer;

import org.example.domain.auction.BidTransaction;
import org.example.domain.auction.Auction;

public class BidderClient implements AuctionObserver {

    private String name;

    public BidderClient(String name) {
        this.name = name;
    }

    @Override
    public void update(Auction auction, BidTransaction bid, double MinIncreament) {
        System.out.println(name + " nhận update: giá mới = " + bid.getAmount());
        System.out.println("Muc tang toi thieu cap nhat thanh: " + MinIncreament);
    }
}

