package org.example.service;

import org.example.dao.AuctionDAO;
import org.example.dao.BidDAO;
import org.example.model.auction.auction;
import org.example.model.user.Bidder;

public class AuctionService {
    private AuctionDAO auctionDAO = new AuctionDAO();
    private BidDAO bidDAO = new BidDAO();

    public void StartAuction(auction Auction){
        Auction.start();
        auctionDAO.save(Auction, "START");
    }
    public void placeBid(auction Auction, Bidder bidder,double amount){
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder null");
        }

        Auction.placeBid(bidder, amount);

        //Nếu cái placeBid qua được hết logic trong auction thì
        auctionDAO.update(Auction, "RUNNING");
        bidDAO.save(Auction);

    }

    public void FinishAuction(auction Auction){
        Auction.finish();
        auctionDAO.update(Auction, "FINISHED");
    }
}
