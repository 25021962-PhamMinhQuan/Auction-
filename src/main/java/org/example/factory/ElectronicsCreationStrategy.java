package org.example.factory;

import org.example.model.item.Electronics;
import org.example.model.item.Item;

import java.time.LocalDateTime;

public class ElectronicsCreationStrategy implements ItemCreationStrategy{
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
        return "ELECTRONIC";
    }
}
