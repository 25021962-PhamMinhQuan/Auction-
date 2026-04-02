package org.example.observer;

import org.example.model.auction.BidTransaction;
import org.example.model.auction.auction;

public class BidderClient implements AuctionObserver {

    private String name;

    public BidderClient(String name) {
        this.name = name;
    }

    @Override
    public void update(auction Auction, BidTransaction bid, double MinIncreament) {
        System.out.println(name + " nhận update: giá mới = " + bid.getAmount());
        System.out.println("Muc tang toi thieu cap nhat thanh: " + MinIncreament);
    }
}

