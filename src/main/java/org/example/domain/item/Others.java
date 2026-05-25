package org.example.domain.item;

import java.time.LocalDateTime;

public class Others extends Item {

    public Others(String name,
                    String description,
                    double price,
                    LocalDateTime start,
                    LocalDateTime end,
                    String imageUrl) {
        super(name, description, price, start, end, imageUrl);
    }


    // contructor load từ database
    public Others(String id,
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
        return "OTHERS";
    }
}



