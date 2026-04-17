package org.example;

import org.example.controller.AuthController;
import org.example.factory.ItemFactory;
import org.example.model.auction.Auction;
import org.example.model.auction.BiddingCoordinator;
import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.model.user.Seller;
import org.example.model.user.User;
import org.example.observer.BidderClient;
import org.example.service.AuctionService;
import org.example.service.ItemService;
import org.example.util.AutoBid;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        AuthController auth = new AuthController();
        ItemService itemService = new ItemService();

        // register
        auth.register(new Bidder("5", "user_account_A", "Password@123"));
        auth.register(new Bidder("6", "user_account_B", "Password@123"));
        auth.register(new Seller("7", "user_account_C", "Password@123"));

        // login
        User a = auth.login("user_account_A", "Password@123");
        User b = auth.login("user_account_B", "Password@123");
        User c = auth.login("user_account_C", "Password@123");

        System.out.println("Login success: " + a.getUsername());

        // đấu giá
        Item item = itemService.CreateItem(
                "ELECTRONIC",
                "i1",
                "Laptop",
                "Gaming",
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(2),
                (Seller) c
        );
        Auction auction = new Auction(item);
        AuctionService auctionService = new AuctionService();
        auctionService.StartAuction(auction);

        auctionService.addObserverToAuction(auction,new BidderClient("Client1"));

        auctionService.registerAutoBid(auction, new AutoBid((Bidder) b, 2000, 100));


        auctionService.placeBid(auction,(Bidder) a, 1100);

        System.out.println("Current price: " + auction.getCurrentPrice());
        System.out.println(auction.getBids());
    }
}