package org.example.domain.auction;

import org.example.domain.item.Item;
import org.example.domain.user.Bidder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {
        private int id;
        // tạo một mảng Status đặc biệt các giai đoạn của quá trình đấu giá
        public enum Status {OPEN, RUNNING, FINISHED, CANCELED, PAID}

        private Item item;

        // cnay để cập nhận trạng thái của giao dịch
        private volatile Status status;

        // cái list này để lưu lịch sử giao dịch
        private final List<BidTransaction> bids;

        private Bidder highestBidder;

        private final ReentrantLock bidLock = new ReentrantLock();  // Fine-grained lock


        public Auction(Item item) {
            this.item = item;
            this.status = Status.OPEN;
            this.bids = new CopyOnWriteArrayList<>();
        }

        public void validateBid(Bidder bidder, double amount, BidTransaction.BidType type) {
            if (bidder == null) throw new IllegalArgumentException("Bidder cannot be null");

            if (status == Status.CANCELED) {
                throw new IllegalStateException("Auction cancelled");
            }
            if (status == Status.PAID) {
                throw new IllegalStateException("Auction finished");
            }
            if (status != Status.RUNNING) {
                throw new IllegalStateException("Auction not running");
            }
            if (LocalDateTime.now().isAfter(item.getEndTime())) {
                throw new IllegalStateException("Auction ended");
            }
            if (amount <= item.getCurrentPrice() || amount < item.getCurrentPrice() + getMinIncrement()) {
                throw new IllegalArgumentException("Bid too low");
            }
            if (highestBidder != null &&
                    highestBidder.getId().equals(bidder.getId()) &&
                    type == BidTransaction.BidType.MANUAL) {
                throw new IllegalArgumentException("You are still the highest");
            }
        }

        public BidTransaction recordBid(Bidder bidder, double amount, BidTransaction.BidType type) {
            bidLock.lock(); // thread safe
            try {
                item.setCurrentPrice(amount);
                highestBidder = bidder;

                BidTransaction bid = new BidTransaction(bidder, amount, type);
                bids.add(bid);

                // Anti-sniping
                AntiSniping();

                return bid;
            } finally {
                bidLock.unlock();
            }
        }

    private LocalDateTime lastExtensionTime = null;

    public void AntiSniping() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime thirtySecBeforeEnd = item.getEndTime().minusSeconds(30);

        // Chỉ extend nếu:
        // 1. Đang trong 30s cuối
        // 2. Chưa extend trong vòng 30s gần nhất (tránh extend liên tục)
        if (now.isAfter(thirtySecBeforeEnd) &&
                (lastExtensionTime == null || now.isAfter(lastExtensionTime.plusSeconds(30)))) {
            item.extendTime(60);
            lastExtensionTime = now;
        }
    }


        public double getMinIncrement(){
            return this.item.getCurrentPrice() * 0.05;
        }

        public void start() {
            if (status == Status.OPEN) {
                status = Status.RUNNING;
            }
        }

        public void finish() {
            if (status == Status.RUNNING) {
                status = Status.FINISHED;
            }
        }

        public Bidder getWinner() {
            if (status == Status.FINISHED) {
                return highestBidder;
            } else {
                return null;
            }
        }

        public void markPaid(){
            if(status == Status.FINISHED){
                status = Status.PAID;
            }
        }

        public void cancel(){
            if(status == Status.OPEN || status == Status.RUNNING){
                status = Status.CANCELED;
            }
        }

        public double getCurrentPrice() {
            return item.getCurrentPrice();
        }

        public List<BidTransaction> getBids() {
            return Collections.unmodifiableList(bids);
        }

        public void setId(int id) {
            this.id = id;
        }
        public int getId(){
            return this.id;
        }
        public Bidder getHighestBidder() {
            return highestBidder;
        }

        public Item getItem() {
            return item;
        }
        public Status getStatus(){
            return status;
        }
}


