package org.example.domain.item;

import org.example.domain.Entity;
import org.example.util.IDGenerator;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentPrice;

    protected LocalDateTime startTime;
    protected LocalDateTime endTime;


    public enum ItemType {
        ELECTRONICS,
        ART
    }

    public Item(String name,
                String description,
                double startPrice,
                LocalDateTime startTime,
                LocalDateTime endTime) {
        super(IDGenerator.generateItemID());
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // constuctor này để lấy dữ liệu từ database khi id đã có sẵn
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

    public double getStartPrice() {
        return startPrice;
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

    public String getName() { return name; }

    public String getDescription() { return description; }

    public abstract String getType();

    public void setEndTime(LocalDateTime localDateTime) {
        this.endTime = localDateTime;
    }

    public void setStartTime(LocalDateTime localDateTime) {
        this.startTime = localDateTime;
    }

}

