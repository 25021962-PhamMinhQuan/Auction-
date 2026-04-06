package org.example;

import org.example.controller.AuthController;
import org.example.factory.ItemFactory;
import org.example.model.auction.Auction;
import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.model.user.User;
import org.example.observer.BidderClient;
import org.example.service.AuctionService;
import org.example.util.AutoBid;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        AuthController auth = new AuthController();

        // register
        auth.register(new Bidder("1", "userA", "Password@123"));
        auth.register(new Bidder("2", "userB", "Password@123"));

        // login
        User a = auth.login("userA", "Password@123");
        User b = auth.login("userB", "Password@123");

        System.out.println("Login success: " + a.getUsername());

        // đấu giá
        Item item = ItemFactory.createItem(
                "ELECTRONICS",
                "i1",
                "Laptop",
                "Gaming",
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(2)
        );

        Auction auction = new Auction(item);
        AuctionService auctionService = new AuctionService();
        auctionService.StartAuction(auction);

        auction.addObserver(new BidderClient("Client1"));

        auctionService.registerAutoBid(auction, new AutoBid((Bidder) b, 2000, 100));


        auctionService.placeBid(auction,(Bidder) a, 1100);

        System.out.println("Current price: " + auction.getCurrentPrice());
        System.out.println(auction.getBids());
    }
}