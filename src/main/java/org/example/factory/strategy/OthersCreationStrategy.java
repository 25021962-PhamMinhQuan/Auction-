package org.example.factory.strategy;

import org.example.domain.item.Electronics;
import org.example.domain.item.Item;

import java.time.LocalDateTime;

public class OthersCreationStrategy implements ItemCreationStrategy{
    public Item createItem(
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end){
        return new Electronics(name, describe, price, start, end);
    }

    public Item createItemFromDatabase(
            String id,
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end){
        return new Electronics(name, describe, price, start, end);
    }

    public String getType(){
        return "OTHERS";
    }
}
