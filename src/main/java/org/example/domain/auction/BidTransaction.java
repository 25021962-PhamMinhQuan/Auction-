package org.example.domain.auction;

import org.example.domain.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime time;
    public enum BidType { MANUAL, AUTO }
    private BidType type;
    public BidTransaction(Bidder bidder, double amount,BidType type) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = LocalDateTime.now();
        this.type = type;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public BidType getType() { return type; }

    @Override
    public String toString() {
        return "BidTransaction{" +
                "bidder=" + this.bidder.getUsername() +
                ", amount=" + this.amount +
                ", time=" + this.time +
                ", type=" + this.type +
                '}';
    }
}

