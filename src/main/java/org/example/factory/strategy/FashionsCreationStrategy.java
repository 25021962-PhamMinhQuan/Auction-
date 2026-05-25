package org.example.factory.strategy;

import org.example.domain.item.Fashions;
import org.example.domain.item.Item;

import java.time.LocalDateTime;

public class FashionsCreationStrategy implements ItemCreationStrategy{
    public Item createItem(
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end){
        return new Fashions(name, describe, price, start, end);
    }

    public Item createItemFromDatabase(
            String id,
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end){
        return new Fashions(name, describe, price, start, end);
    }

    public String getType(){
        return "FASHIONS";
    }
}
