package org.example.model.auction;

import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.util.AutoBid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Auction {
        private int id;
        // tạo một mảng Status đặc biệt các giai đoạn của quá trình đấu giá
        public enum Status {OPEN, RUNNING, FINISHED, CANCELED, PAID}

        private Item item;

        // cnay để cập nhận trạng thái của giao dịch
        private Status status;

        // cái list này để lưu lịch sử giao dịch
        private List<BidTransaction> bids;

        private Bidder highestBidder;


        public Auction(Item item) {
            this.item = item;
            this.status = Status.OPEN;
            this.bids = new ArrayList<>();
        }

        public void validateBid(Bidder bidder, double amount, BidTransaction.BidType type) {
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
            item.setCurrentPrice(amount);
            highestBidder = bidder;

            BidTransaction bid = new BidTransaction(bidder, amount, type);
            bids.add(bid);

            // Anti-sniping
            if (item.getEndTime().minusSeconds(30).isBefore(LocalDateTime.now())) {
                item.extendTime(60);
            }

            return bid;
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
            return bids;
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
        public String getStatus(){
            return status.name();
        }
}


