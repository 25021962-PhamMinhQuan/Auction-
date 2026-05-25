package org.example.domain.item;

import java.time.LocalDateTime;

public class Vehicles extends Item {

    public Vehicles(String name,
                  String description,
                  double price,
                  LocalDateTime start,
                  LocalDateTime end) {
        super(name, description, price, start, end);
    }


    // contructor load từ database
    public Vehicles(String id,
                  String name,
                  String description,
                  double price,
                  LocalDateTime start,
                  LocalDateTime end) {
        super(id, name, description, price, start, end);
    }

    @Override
    public String getType(){
        return "VEHICLES";
    }
}


