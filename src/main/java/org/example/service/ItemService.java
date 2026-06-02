package org.example.service;

import org.example.factory.ItemFactory;
import org.example.domain.item.Item;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.repository.ItemRepository;

import java.time.LocalDateTime;
import java.util.List;

public class ItemService {
    private final ItemRepository ItemRepositoryImpl;
    public ItemService(ItemRepository ItemRepositoryImpl){
        this.ItemRepositoryImpl = ItemRepositoryImpl;
    }

    public Item CreateItem(String type, String name, String description, double price, LocalDateTime start, LocalDateTime end, String imageUrl, Seller seller){
        if(!seller.getRole().equals(User.UserRole.SELLER.name())){
            throw new IllegalStateException("Only seller can add item");
        }
        Item item = ItemFactory.createItem(type,name,description,price,start,end,imageUrl);
        item.setStatus("PENDING");

        ItemRepositoryImpl.save(item,seller.getId());

        return item;
    }

    public void updateItem(Item item, Seller seller) {
        if (!seller.getRole().equals(User.UserRole.SELLER.name())) {
            throw new IllegalStateException("This item can only update by seller");
        }
        String ownerId = ItemRepositoryImpl.findSellerIdByItemId(item.getId());
        if (ownerId == null || !ownerId.equals(seller.getId())) {
            throw new IllegalStateException("You can only update your own item");
        }
        ItemRepositoryImpl.update(item);
    }

    public void deleteItem(String itemId, User requester) {
        if (!requester.getRole().equals(User.UserRole.SELLER.name()) && !requester.getRole().equals(User.UserRole.ADMIN.name())) {
            throw new IllegalStateException("You can not delete this item");
        }
        ItemRepositoryImpl.delete(itemId);
    }

    public List<Item> getItemsBySeller(Seller seller) {
        return ItemRepositoryImpl.findBySeller(seller.getId());
    }

    public Item getItemById(String id) {
        return ItemRepositoryImpl.findById(id);
    }
    public String getSellerIdByItemId(String itemId) {
        return ItemRepositoryImpl.findSellerIdByItemId(itemId);
    }
    public List<Item> findAllItems() {
        return ItemRepositoryImpl.findAll();
    }

    public void approveItem(String itemId, User requester) {
        if (requester == null || !requester.getRole().equals(User.UserRole.ADMIN.name())) {
            throw new IllegalStateException("Only admin can approve item");
        }
        Item item = ItemRepositoryImpl.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        ItemRepositoryImpl.updateStatus(item, "APPROVED");
        item.setStatus("APPROVED");
    }

    public long countPendingItems() {
        return ItemRepositoryImpl.findAll().stream()
                .filter(item -> "PENDING".equalsIgnoreCase(item.getStatus()))
                .count();
    }

    public long countAllItems() {
        return ItemRepositoryImpl.findAll().size();
    }
}
