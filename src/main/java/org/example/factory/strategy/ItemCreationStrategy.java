package org.example.factory.strategy;

import org.example.domain.item.Item;

import java.time.LocalDateTime;

public interface ItemCreationStrategy {
    Item createItem(
                    String name,
                    String describe,
                    double price,
                    LocalDateTime start,
                    LocalDateTime end);

    Item createItemFromDatabase(
            String id,
            String name,
            String describe,
            double price,
            LocalDateTime start,
            LocalDateTime end
    );

    String getType();
}
