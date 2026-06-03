package org.example.factory.strategy;

import org.example.domain.item.Estate;
import org.example.domain.item.Item;

import java.time.LocalDateTime;

public class EstateCreationStrategy implements ItemCreationStrategy{
    public Item createItem(
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end,
            String imageUrl){
        return new Estate(name, describe, price, start, end, imageUrl);
    }

    public Item createItemFromDatabase(
            String id,
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end,
            String imageUrl){
        return new Estate(id,name, describe, price, start, end, imageUrl);
    }

    public String getType(){
        return "ESTATE";
    }
}
