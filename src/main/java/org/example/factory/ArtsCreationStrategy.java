package org.example.factory;

import org.example.model.item.Art;
import org.example.model.item.Electronics;
import org.example.model.item.Item;

import java.time.LocalDateTime;

public class ArtsCreationStrategy implements ItemCreationStrategy{
    public Item createItem(
                           String name,
                           String describe,
                           double price,
                           LocalDateTime start,
                           LocalDateTime end){
        return new Art(name, describe, price, start, end);
    }

    public String getType(){
        return "ART";
    }
}
