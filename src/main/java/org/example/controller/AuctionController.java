package org.example.controller;
import org.example.domain.auction.Auction;
import org.example.domain.user.Bidder;
import org.example.domain.user.User;
import org.example.service.AuctionService;
import org.example.factory.ServiceFactory;
import org.example.util.AutoBid;

import java.util.List;

public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController() {
        this.auctionService = ServiceFactory.getInstance().getAuctionService();
    }

    public List<Auction> getAuctionsByStatus(String status) {
        return auctionService.getAuctionsByStatus(status);
    }

    public void placeBid(int auctionId, double amount, User currentUser) {
        if (!(currentUser instanceof Bidder)) {
            throw new IllegalStateException("Only bidders can place bids");
        }
        Auction auction = requireAuction(auctionId);
        auctionService.placeBid(auction, (Bidder) currentUser, amount);
    }

    public void registerAutoBid(int auctionId, double maxBid, double increment, User currentUser) {
        if (!(currentUser instanceof Bidder)) {
            throw new IllegalStateException("Only bidders can register auto-bids");
        }
        Auction auction = requireAuction(auctionId);
        auctionService.registerAutoBid(auction, new AutoBid((Bidder) currentUser, maxBid, increment));
    }

    public void cancelAuction(int auctionId, User requester) {
        auctionService.cancelAuction(requireAuction(auctionId), requester);
    }

    public void markPaid(int auctionId, User requester) {
        auctionService.markPaid(requireAuction(auctionId), requester);
    }

    private Auction requireAuction(int id) {
        Auction auction = auctionService.findbyId(id);
        if (auction == null) {
            throw new IllegalArgumentException("Auction " + id + " not found");
        }
        return auction;
    }
}
