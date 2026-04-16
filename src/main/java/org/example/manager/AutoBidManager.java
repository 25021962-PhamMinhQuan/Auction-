package org.example.manager;

import org.example.model.auction.Auction;
import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.util.AutoBid;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class AutoBidManager {
    private Auction auction;
    private PriorityQueue<AutoBid> autoBids;

    public AutoBidManager(Auction auction){
        this.auction = auction;
        autoBids = new PriorityQueue<>(
                (a, b) -> {
                    int logic = Double.compare(b.getMaxBid(), a.getMaxBid());
                    if(logic == 0){
                        return a.getTime().compareTo(b.getTime());
                    }
                    return logic;
                }
        );
    }

    public void addAutoBid(AutoBid autoBid) {
        autoBids.add(autoBid);
    }

    public AutoBidResult processAuto() {
        Double minIncreament = auction.getMinIncrement();
        Item item = auction.getItem();
        if (autoBids.isEmpty()) return null;

        AutoBid first=null,second = null;
        List<AutoBid> skipped = new ArrayList<>();

        while(!autoBids.isEmpty()){
            AutoBid candiate = autoBids.poll();
            if(candiate.getMaxBid() > auction.getCurrentPrice() && candiate.getIncrement() >= minIncreament){
                first = candiate;
                break;
            }
            skipped.add(candiate);
        }
        if(first == null){
            autoBids.addAll(skipped);
            return null;
        }

        while(!autoBids.isEmpty()){
            AutoBid candiate = autoBids.poll();
            if(candiate.getMaxBid() > item.getCurrentPrice() && candiate.getIncrement() >= minIncreament){
                second = candiate;
                break;
            }
            skipped.add(candiate);
        }
        double priceAfterBid;

        if(second == null){
            priceAfterBid = Math.min(first.getMaxBid(), first.getIncrement() + item.getCurrentPrice());
        }
        else{
            priceAfterBid = Math.min(first.getMaxBid(), second.getMaxBid() + first.getIncrement());
            autoBids.add(second);
        }

        autoBids.add(first);
        autoBids.addAll(skipped);

        return new AutoBidResult(first.getBidder(), priceAfterBid);
    }

    public static class AutoBidResult{
        private Bidder bidder;
        private double amount;

        public AutoBidResult(Bidder bidder, double amount){
            this.bidder = bidder;
            this.amount = amount;
        }

        public Bidder getBidder(){
            return bidder;
        }

        public double getAmount(){
            return amount;
        }
    }
}
