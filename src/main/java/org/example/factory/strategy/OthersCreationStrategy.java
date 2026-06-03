package org.example.factory.strategy;

import org.example.domain.item.Others;
import org.example.domain.item.Item;

import java.time.LocalDateTime;

public class OthersCreationStrategy implements ItemCreationStrategy{
    public Item createItem(
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end,
            String imageUrl){
        return new Others(name, describe, price, start, end, imageUrl);
    }

    public Item createItemFromDatabase(
            String id,
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end,
            String imageUrl){
        return new Others(id,name, describe, price, start, end, imageUrl);
    }

    public String getType(){
        return "OTHERS";
    }
}
