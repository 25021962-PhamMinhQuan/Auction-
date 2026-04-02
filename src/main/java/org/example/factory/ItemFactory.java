package org.example.factory;
import org.example.model.item.Art;
import org.example.model.item.Electronics;
import org.example.model.item.Item;

import java.time.LocalDateTime;


public class ItemFactory {

    public static Item createItem(String type,
                                  String id,
                                  String name,
                                  String describe,
                                  double price,
                                  LocalDateTime start,
                                  LocalDateTime end) {

        switch (type) {
            case "ELECTRONICS":
                return new Electronics(id, name, describe, price, start, end);
            case "ART":
                return new Art(id, name, describe, price, start, end);
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }
}



