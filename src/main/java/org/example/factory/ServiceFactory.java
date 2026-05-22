package org.example.factory;

import org.example.repository.*;
import org.example.repository.impl.*;
import org.example.service.AuctionService;
import org.example.service.ItemService;
import org.example.service.UserService;

public class ServiceFactory {
    private final UserRepository UserRepositoryImpl;
    private final AuctionRepository AuctionRepositoryImpl;
    private final BidRepository BidRepositoryImpl;
    private final AutoBidRepository AutoBidRepositoryImpl;
    private final ItemRepository ItemRepositoryImpl;

    private final AuctionService auctionService;
    private final ItemService itemService;
    private final UserService userService;

    private static volatile ServiceFactory instance;

    private ServiceFactory(){
        this.UserRepositoryImpl = new UserDAO();
        this.AutoBidRepositoryImpl = new AutoBidDAO();
        this.AuctionRepositoryImpl = new AuctionDAO();
        this.ItemRepositoryImpl = new ItemDAO();
        this.BidRepositoryImpl = new BidDAO();

        this.auctionService = new AuctionService(AuctionRepositoryImpl, BidRepositoryImpl, ItemRepositoryImpl, AutoBidRepositoryImpl);
        this.itemService = new ItemService(ItemRepositoryImpl);
        this.userService = new UserService(UserRepositoryImpl);
    }

    public static synchronized ServiceFactory getInstance(){
        if(instance == null){
            instance = new ServiceFactory();
        }
        return instance;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public UserService getUserService() {
        return userService;
    }
}

