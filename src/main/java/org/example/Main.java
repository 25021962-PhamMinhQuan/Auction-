package org.example;

import org.example.controller.AuthController;
import org.example.domain.auction.Auction;
import org.example.domain.item.Item;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.observer.BidderClient;
import org.example.service.AuctionService;
import org.example.service.ItemService;
import org.example.factory.ServiceFactory;
import org.example.util.AutoBid;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        AuthController auth = new AuthController();
        ItemService itemService = ServiceFactory.getInstance().getItemService();

        // register
        auth.register(new Bidder( "user_account_A", "Password@123"));
        auth.register(new Bidder( "user_account_B", "Password@123"));
        auth.register(new Seller( "user_account_C", "Password@123"));

        // login
        User a = auth.login("user_account_A", "Password@123");
        User b = auth.login("user_account_B", "Password@123");
        User c = auth.login("user_account_C", "Password@123");

        System.out.println("Login success: " + a.getUsername());

        // đấu giá
        Item item = itemService.CreateItem(
                "ELECTRONIC",
                "Laptop",
                "Gaming",
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(2),
                "cgi do",
                (Seller) c
        );
        Auction auction = new Auction(item);
        AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
        auctionService.StartAuction(auction);

        auctionService.addObserverToAuction(auction,new BidderClient("Client1"));

        auctionService.registerAutoBid(auction, new AutoBid((Bidder) b, 2000, 100));


        auctionService.placeBid(auction,(Bidder) a, 1100);

        System.out.println("Current price: " + auction.getCurrentPrice());
        System.out.println(auction.getBids());
    }
}