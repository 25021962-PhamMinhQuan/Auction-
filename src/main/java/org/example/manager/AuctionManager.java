package org.example.manager;

import org.example.domain.auction.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionManager {

    // cnay là thuật toán singleton
    //tác dụng là để lưu tất cả cái auction cho 1 auctionManager quản lí thôi tránh trg hợp có 2 auction manager khác nhau
    private static AuctionManager instance;
    private final List<Auction> auctions;

    private AuctionManager() {
        auctions =  new CopyOnWriteArrayList<>();
    }

    // tạo ra một cái static auctionManager duy nhất dùng chung cho tất cả phiên đấu giá
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    public void addAuction(Auction auction) {
        auctions.add(auction);
    }

    public List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }
}

