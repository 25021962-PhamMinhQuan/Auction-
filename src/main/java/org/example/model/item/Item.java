package org.example.model.item;

import org.example.model.Entity;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentPrice;

    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public Item(String id,
                String name,
                String description,
                double startPrice,
                LocalDateTime startTime,
                LocalDateTime endTime) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void extendTime(long seconds) {
        this.endTime = this.endTime.plusSeconds(seconds);
    }

    public LocalDateTime getStartTime(){
        return this.startTime;
    }
}

