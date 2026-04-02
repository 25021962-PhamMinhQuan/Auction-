package org.example.model.auction;

import org.example.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime time;

    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = LocalDateTime.now();
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

    @Override
    public String toString() {
        return "BidTransaction{" +
                "bidder=" + this.bidder.getUsername() +
                ", amount=" + this.amount +
                ", time=" + this.time +
                '}';
    }
}

