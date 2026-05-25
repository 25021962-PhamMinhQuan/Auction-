package org.example.domain.item;

import java.time.LocalDateTime;

public class Estate extends Item {

    public Estate(String name,
                    String description,
                    double price,
                    LocalDateTime start,
                    LocalDateTime end) {
        super(name, description, price, start, end);
    }


    // contructor load từ database
    public Estate(String id,
                    String name,
                    String description,
                    double price,
                    LocalDateTime start,
                    LocalDateTime end) {
        super(id, name, description, price, start, end);
    }

    @Override
    public String getType(){
        return "ESTATE";
    }
}

