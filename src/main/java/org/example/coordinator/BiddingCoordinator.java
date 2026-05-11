package org.example.coordinator;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.manager.AutoBidManager;
import org.example.domain.user.Bidder;
import org.example.observer.AuctionNotifier;
import org.example.domain.auction.BidTransaction.BidType;
import org.example.util.AutoBid;

import java.util.function.Consumer;

public class BiddingCoordinator {
    private Auction auction;
    private AutoBidManager autoBidManager;
    private AuctionNotifier auctionNotifier;
    private Consumer<BidTransaction> onBidPerSisted;

    public BiddingCoordinator(Auction auction){
        this.auction = auction;
        this.autoBidManager = new AutoBidManager(auction);
        this.auctionNotifier = new AuctionNotifier(auction);
    }

    public void setOnBidPersisted(Consumer<BidTransaction> logic){
        this.onBidPerSisted = logic;
    }

    public synchronized void placeBid(Bidder bidder, double amount) {
        placeBidInternal(bidder, amount, true, BidTransaction.BidType.MANUAL);
    }

    private void placeBidInternal(Bidder bidder, double amount, boolean triggerAuto, BidTransaction.BidType type) {
        auction.validateBid(bidder,amount, type );
        BidTransaction bid = auction.recordBid(bidder,amount,type);

        if(onBidPerSisted!=null){
            onBidPerSisted.accept(bid);
        }

        auctionNotifier.notifyObservers(bid);

        if(triggerAuto){
            processAutoBids();
        }
    }

    private void processAutoBids(){
        AutoBidManager.AutoBidResult result = autoBidManager.processAuto();
        if(result != null){
            placeBidInternal(result.getBidder(), result.getAmount(), false, BidType.AUTO);
        }
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

    public  Auction getAuction(){
        return auction;
    }


}
