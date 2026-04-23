package org.example.model.item;

import java.time.LocalDateTime;

public class Electronics extends Item {

    public Electronics(String name,
                       String description,
                       double price,
                       LocalDateTime start,
                       LocalDateTime end) {
        super(name, description, price, start, end);
    }


    // contructor load từ database
    public Electronics(String id,
                       String name,
                       String description,
                       double price,
                       LocalDateTime start,
                       LocalDateTime end) {
        super(id, name, description, price, start, end);
    }

    @Override
    public String getType(){
        return "Electronics";
    }
}

