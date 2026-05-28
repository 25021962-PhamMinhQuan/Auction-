package org.example;

import org.example.controller.AuthController;
import org.example.domain.auction.Auction;
import org.example.domain.item.Item;
import org.example.domain.user.Admin;
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
        auth.register(new Admin( "user_account_A", "Password@123"));
        auth.register(new Admin( "user_account_B", "Password@123"));
        auth.register(new Admin( "user_account_C", "Password@123"));
        auth.register(new Admin( "user_account_D", "Password@123"));

    }
}