package org.example.util;

import org.example.model.user.Bidder;

import java.time.LocalDateTime;

public class AutoBid {

    private Bidder bidder;
    private double maxBid;
    private double increment;
    private LocalDateTime time;

    public AutoBid(Bidder bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.time = LocalDateTime.now();
    }
    public LocalDateTime getTime() { return time; }
    public Bidder getBidder() {
        return bidder;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }
}

