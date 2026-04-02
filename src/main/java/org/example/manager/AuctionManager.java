package org.example.manager;

import org.example.model.auction.auction;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {

    // cnay là thuật toán singleton
    //tác dụng là để lưu tất cả cái auction cho 1 auctionManager quản lí thôi tránh trg hợp có 2 auction manager khác nhau
    private static AuctionManager instance;
    private List<auction> auctions;

    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    // tạo ra một cái static auctionManager duy nhất dùng chung cho tất cả phiên đấu giá
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    public void addAuction(auction Auction) {
        auctions.add(Auction);
    }

    public List<auction> getAuctions() {
        return new ArrayList<>(auctions);
    }
}

