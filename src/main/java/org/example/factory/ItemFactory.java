package org.example.factory;
import org.example.model.item.Art;
import org.example.model.item.Electronics;
import org.example.model.item.Item;
import java.util.*;

import java.time.LocalDateTime;


public class ItemFactory {
    private static final Map<String, ItemCreationStrategy> strategies = new HashMap<>();
    public static void validateItemParameters(String name,
                                  String describe,
                                  double price,
                                  LocalDateTime start,
                                  LocalDateTime end) {
        if(end.isBefore(start)){
            throw new IllegalArgumentException("The start time must be before the end time.");
        }
        //if(start.isBefore(LocalDateTime.now())){
        //    throw new IllegalArgumentException("Bidding time must starts from now");
        //}

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Product name must not be blank.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("The price must be greater than 0.");
        }
    }

    public static void registerStrategy(ItemCreationStrategy strategy){
        strategies.put(strategy.getType(), strategy);
    }

    static {
        ItemFactory.registerStrategy(new ArtsCreationStrategy());
        ItemFactory.registerStrategy(new ElectronicsCreationStrategy());
    }

    public static Item createItem(String type,
                                  String name,
                                  String description,
                                  double price,
                                  LocalDateTime start,
                                  LocalDateTime end){
        ItemFactory.validateItemParameters(name,description,price,start,end);

        ItemCreationStrategy strategy = strategies.get(type);
        if(strategy == null){
            throw new IllegalArgumentException("Unknow item type: " + type);
        }

        return strategy.createItem(name,description,price,start,end);
    }

    public static Item createItemFromDAO(String type,
                                  String id,
                                  String name,
                                  String description,
                                  double price,
                                  LocalDateTime start,
                                  LocalDateTime end){

        ItemCreationStrategy strategy = strategies.get(type);
        if(strategy == null){
            throw new IllegalArgumentException("Unknow item type: " + type);
        }

        return strategy.createItemFromDatabase(id,name,description,price,start,end);
    }

    public static java.util.Set<String> getAvailableTypes() {
        return strategies.keySet();
    }
}



