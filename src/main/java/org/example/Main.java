package org.example;

import org.example.controller.AuthController;
import org.example.factory.ItemFactory;
import org.example.model.auction.auction;
import org.example.model.item.Item;
import org.example.model.user.Bidder;
import org.example.model.user.User;
import org.example.observer.BidderClient;
import org.example.util.AutoBid;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        AuthController auth = new AuthController();

        // register
        auth.register(new Bidder("1", "userA", "123"));
        auth.register(new Bidder("2", "userB", "123"));

        // login
        User a = auth.login("userA", "123");
        User b = auth.login("userB", "123");

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

        auction Auction = new auction(item);
        Auction.start();

        Auction.addObserver(new BidderClient("Client1"));

        Auction.addAutoBid(new AutoBid((Bidder) b, 2000, 100));

        Auction.placeBid((Bidder) a, 1100);

        System.out.println("Current price: " + Auction.getCurrentPrice());
        System.out.println(Auction.getBids());
    }
}