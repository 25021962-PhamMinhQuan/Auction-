package org.example.service;

import org.example.dao.*;
import org.example.model.user.User;
import org.example.repository.*;

public class ServiceFactory {
    private final UserRepository userDAO;
    private final AuctionRepository auctionDAO;
    private final BidRepository bidDAO;
    private final AutoBidRepository autoBidDAO;
    private final ItemRepository itemDAO;

    private final AuctionService auctionService;
    private final ItemService itemService;
    private final UserService userService;

    private static volatile ServiceFactory instance;

    private ServiceFactory(){
        this.userDAO = new UserDAO();
        this.autoBidDAO = new AutoBidDao();
        this.auctionDAO = new AuctionDAO();
        this.itemDAO = new ItemDao();
        this.bidDAO = new BidDAO();

        this.auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO, autoBidDAO);
        this.itemService = new ItemService(itemDAO);
        this.userService = new UserService(userDAO);
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

