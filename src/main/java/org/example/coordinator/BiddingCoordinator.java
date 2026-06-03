package org.example.coordinator;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.auction.BidTransaction.BidType;
import org.example.domain.user.Bidder;
import org.example.manager.AutoBidManager;
import org.example.observer.AuctionNotifier;
import org.example.repository.AuctionRepository;
import org.example.repository.impl.ItemDAO;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class BiddingCoordinator {
    private Auction auction;
    private AutoBidManager autoBidManager;
    private AuctionNotifier auctionNotifier;
    private Consumer<BidTransaction> onBidPerSisted;
    private AuctionRepository auctionRepository;

    public BiddingCoordinator(Auction auction) {
        this.auction = auction;
        this.autoBidManager = new AutoBidManager(auction);
        this.auctionNotifier = new AuctionNotifier(auction);
    }

    public void setAuctionRepository(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    public void setOnBidPersisted(Consumer<BidTransaction> logic) {
        this.onBidPerSisted = logic;
    }

    public synchronized void placeBid(Bidder bidder, double amount) {
        placeBidInternal(bidder, amount, true, BidTransaction.BidType.MANUAL);
    }

    private void placeBidInternal(Bidder bidder, double amount, boolean triggerAuto, BidTransaction.BidType type) {
        LocalDateTime endTimeBefore = auction.getItem().getEndTime();
        auction.AntiSniping();
        auction.validateBid(bidder, amount, type);
        BidTransaction bid = auction.recordBid(bidder, amount, type);
        if (onBidPerSisted != null) {
            onBidPerSisted.accept(bid);
        }
        if (auction.getItem().getEndTime().isAfter(endTimeBefore)) {
            new ItemDAO().updateEndTime(auction.getItem());
            if (auctionRepository != null) auctionRepository.updateEndTime(auction);
        }
        auctionNotifier.notifyObservers(bid);

        if (triggerAuto) {
            processAutoBids();
        }
    }

    private void processAutoBids() {
        AutoBidManager.AutoBidResult result;
        while ((result = autoBidManager.processAuto()) != null) {
            placeBidInternal(result.getBidder(), result.getAmount(), false, BidType.AUTO);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public synchronized void triggerAutoBid() {
        processAutoBids();
    }

    public synchronized void registerAutoBid(AutoBid autoBid) {
        autoBidManager.addAutoBid(autoBid);
    }

    public AuctionNotifier getNotifier() {
        return auctionNotifier;
    }

    public AutoBidManager getAutoBidManager() {
        return autoBidManager;
    }

    public Auction getAuction() {
        return auction;
    }
}
