package org.example.model.item;

import java.time.LocalDateTime;


public class Art extends Item {

    public Art(String id,
               String name,
               String description,
               double price,
               LocalDateTime start,
               LocalDateTime end) {
        super(id, name, description, price, start, end);
    }
}

