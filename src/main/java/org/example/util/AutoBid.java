package org.example.util;

import org.example.domain.user.Bidder;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class AutoBid {

    private Bidder bidder;
    private double maxBid;
    private double increment;
    private double registeredMinIncrement;
    private LocalDateTime time;

    public AutoBid(Bidder bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredMinIncrement = 0;
        this.time = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public LocalDateTime getTime() { return time; }
    public Bidder getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public double getRegisteredMinIncrement() { return registeredMinIncrement; }
    public void setRegisteredMinIncrement(double v) { this.registeredMinIncrement = v; }
}