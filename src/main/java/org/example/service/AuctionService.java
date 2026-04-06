package org.example.service;

import org.example.dao.AuctionDAO;
import org.example.dao.BidDAO;
import org.example.model.auction.Auction;
import org.example.model.user.Bidder;

public class AuctionService {
    private AuctionDAO auctionDAO = new AuctionDAO();
    private BidDAO bidDAO = new BidDAO();

    public void StartAuction(Auction auction){
        auction.start();

        auction.setOnBidPersisted(bid -> {
            auctionDAO.update(auction, "RUNNING");
            bidDAO.save(bid, auction.getId());
        });
        auctionDAO.save(auction, "START");
    }
    public void placeBid(Auction auction, Bidder bidder,double amount){
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder null");
        }

        auction.placeBid(bidder, amount);

    }

    public void FinishAuction(Auction auction){
        auction.finish();
        auctionDAO.update(auction, "FINISHED");
    }
}

