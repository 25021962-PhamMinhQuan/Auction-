package org.example.manager;

import org.example.domain.auction.Auction;
import org.example.domain.item.Item;
import org.example.domain.user.Bidder;
import org.example.util.AutoBid;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class AutoBidManager {
    private Auction auction;
    private final PriorityQueue<AutoBid> autoBids;
    private final ReentrantLock lock = new ReentrantLock();

    public AutoBidManager(Auction auction) {
        this.auction = auction;
        autoBids = new PriorityQueue<>((a, b) -> {
            int logic = Double.compare(b.getMaxBid(), a.getMaxBid());
            if (logic == 0) return a.getTime().compareTo(b.getTime());
            return logic;
        });
    }

    public void addAutoBid(AutoBid autoBid) {
        lock.lock();
        try {
            autoBid.setRegisteredMinIncrement(auction.getMinIncrement());
            autoBids.add(autoBid);
        } finally {
            lock.unlock();
        }
    }

    public AutoBidResult processAuto() {
        lock.lock();
        try {
            double minIncrement = auction.getMinIncrement();
            Item item = auction.getItem();
            if (autoBids.isEmpty()) return null;

            AutoBid first = null, second = null;
            List<AutoBid> skipped = new ArrayList<>();

            while (!autoBids.isEmpty()) {
                AutoBid candidate = autoBids.poll();
                if (candidate.getMaxBid() > auction.getCurrentPrice()
                        && candidate.getIncrement() >= candidate.getRegisteredMinIncrement()) {
                    first = candidate;
                    break;
                }
                skipped.add(candidate);
            }
            if (first == null) {
                autoBids.addAll(skipped);
                return null;
            }

            while (!autoBids.isEmpty()) {
                AutoBid candidate = autoBids.poll();
                if (candidate.getMaxBid() > auction.getCurrentPrice()
                        && candidate.getIncrement() >= candidate.getRegisteredMinIncrement()) {
                    second = candidate;
                    break;
                }
                skipped.add(candidate);
            }

            double effectiveIncrement = Math.max(first.getIncrement(), minIncrement);
            double priceAfterBid;
            if (second == null) {
                priceAfterBid = Math.min(first.getMaxBid(), effectiveIncrement + item.getCurrentPrice());
            } else {
                priceAfterBid = Math.min(first.getMaxBid(), second.getMaxBid() + effectiveIncrement);
                autoBids.add(second);
            }

            autoBids.add(first);
            autoBids.addAll(skipped);

            return new AutoBidResult(first.getBidder(), priceAfterBid);
        } finally {
            lock.unlock();
        }
    }

    public static class AutoBidResult {
        private Bidder bidder;
        private double amount;

        public AutoBidResult(Bidder bidder, double amount) {
            this.bidder = bidder;
            this.amount = amount;
        }

        public Bidder getBidder() { return bidder; }
        public double getAmount() { return amount; }
    }
}