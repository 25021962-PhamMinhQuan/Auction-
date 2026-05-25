package org.example.domain.item;

import java.time.LocalDateTime;


public class Art extends Item {

    public Art(String name,
               String description,
               double price,
               LocalDateTime start,
               LocalDateTime end,
               String imageUrl) {
        super(name, description, price, start, end, imageUrl);
    }

    // load từ database
    public Art(String id,
               String name,
               String description,
               double price,
               LocalDateTime start,
               LocalDateTime end,
               String imageUrl) {
        super(id, name, description, price, start, end, imageUrl);
    }

    @Override
    public String getType(){
        return "ART";
    }
}

