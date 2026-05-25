package org.example.domain.item;

import java.time.LocalDateTime;

public class Vehicles extends Item {

    public Vehicles(String name,
                  String description,
                  double price,
                  LocalDateTime start,
                  LocalDateTime end,
                    String imangeUrl) {
        super(name, description, price, start, end, imangeUrl);
    }


    // contructor load từ database
    public Vehicles(String id,
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
        return "VEHICLES";
    }
}


