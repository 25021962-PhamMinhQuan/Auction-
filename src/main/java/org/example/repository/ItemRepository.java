package org.example.repository;

import org.example.domain.item.Item;

import java.util.List;

public interface ItemRepository {
    void save(Item item, String seller_id);
    Item findById(String id);
    List<Item> findBySeller(String sellerId);
    String findSellerIdByItemId(String itemId);
    void update(Item item);
    void updateStatus(Item item,String status);
    void delete(String id);
    List<Item> findAll();
}
