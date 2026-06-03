package org.example.manager;

import org.example.domain.auction.Auction;
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
    private Bidder lastAutoBidder = null;

    public AutoBidManager(Auction auction) {
        this.auction = auction;
        autoBids = new PriorityQueue<>(
                (a, b) -> {
                    int logic = Double.compare(b.getMaxBid(), a.getMaxBid());
                    if (logic == 0) return a.getTime().compareTo(b.getTime());
                    return logic;
                }
        );
    }

    public void addAutoBid(AutoBid autoBid) {
        lock.lock();
        try {
            autoBids.add(autoBid);
        } finally {
            lock.unlock();
        }
    }

    public void resetLastBidder() {
        lock.lock();
        try { lastAutoBidder = null; }
        finally { lock.unlock(); }
    }

    public AutoBidResult processAuto() {
        lock.lock();
        try {
            double minIncrement = auction.getMinIncrement();
            double currentPrice = auction.getItem().getCurrentPrice();

            if (autoBids.isEmpty()) return null;

            List<AutoBid> temp = new ArrayList<>(autoBids);
            autoBids.clear();

            // Lọc các bid còn hợp lệ (maxBid > currentPrice)
            List<AutoBid> valid = temp.stream()
                    .filter(ab -> ab.getMaxBid() > currentPrice)
                    .collect(java.util.stream.Collectors.toList());
            List<AutoBid> invalid = temp.stream()
                    .filter(ab -> ab.getMaxBid() <= currentPrice)
                    .collect(java.util.stream.Collectors.toList());

            autoBids.addAll(invalid); // invalid vẫn giữ lại để sau này check

            if (valid.isEmpty()) return null;

            // Chọn người KHÁC lastAutoBidder, ưu tiên maxBid cao nhất
            AutoBid chosen = valid.stream()
                    .filter(ab -> !ab.getBidder().equals(lastAutoBidder))
                    .findFirst() // valid đã được sort theo maxBid desc từ PriorityQueue
                    .orElse(null);

            autoBids.addAll(valid);

            if (chosen == null) return null; // chỉ còn 1 người đang dẫn đầu

            double effectiveIncrement = Math.max(chosen.getIncrement(), minIncrement);
            double nextPrice = currentPrice + effectiveIncrement;

            if (nextPrice > chosen.getMaxBid()) return null;

            lastAutoBidder = chosen.getBidder();
            return new AutoBidResult(chosen.getBidder(), nextPrice);

        } finally {
            lock.unlock();
        }
    }

    public static class AutoBidResult {
        private final Bidder bidder;
        private final double amount;
        public AutoBidResult(Bidder bidder, double amount) { this.bidder = bidder; this.amount = amount; }
        public Bidder getBidder() { return bidder; }
        public double getAmount() { return amount; }
    }
}