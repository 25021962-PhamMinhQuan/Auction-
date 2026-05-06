package org.example.service;

import org.example.dao.ItemDao;
import org.example.factory.ItemFactory;
import org.example.model.item.Item;
import org.example.model.user.Seller;
import org.example.model.user.User;
import org.example.repository.ItemRepository;

import java.time.LocalDateTime;
import java.util.List;

public class ItemService {
    private final ItemRepository itemDAO;
    public ItemService(ItemRepository itemDAO){
        this.itemDAO = itemDAO;
    }

    public Item CreateItem(String type, String name, String description, double price, LocalDateTime start, LocalDateTime end, Seller seller){
        if(!seller.getRole().equals(User.UserRole.SELLER.name())){
            throw new IllegalStateException("Only seller can add item");
        }
        Item item = ItemFactory.createItem(type,name,description,price,start,end);

        itemDAO.save(item,seller.getId());

        return item;
    }

    public void updateItem(Item item, Seller seller) {
        if (!seller.getRole().equals(User.UserRole.SELLER.name())) {
            throw new IllegalStateException("This item can only update by seller");
        }
        itemDAO.update(item);
    }

    public void deleteItem(String itemId, User requester) {
        if (!requester.getRole().equals(User.UserRole.SELLER.name()) && !requester.getRole().equals(User.UserRole.ADMIN.name())) {
            throw new IllegalStateException("You can not delete this item");
        }
        itemDAO.delete(itemId);
    }

    public List<Item> getItemsBySeller(Seller seller) {
        return itemDAO.findBySeller(seller.getId());
    }

    public Item getItemById(String id) {
        return itemDAO.findById(id);
    }

}
