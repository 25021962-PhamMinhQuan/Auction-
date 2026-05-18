package org.example.server;

import org.example.domain.auction.Auction;
import org.example.domain.user.Bidder;
import org.example.domain.user.User;
import org.example.service.AuctionService;
import org.example.factory.ServiceFactory;
import org.example.service.UserService;
import org.example.util.AutoBid;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private final AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
    private final UserService userService = ServiceFactory.getInstance().getUserService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("System handle: " + line);
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            AuctionServer.connectClient.remove(this);
            closeConnection();
        }
    }

    private void handleCommand(String rawLine) {
        String[] parts   = rawLine.split("\\|");
        String   command = parts[0];

        switch (command) {

            case "LOGIN": {
                // LOGIN|username|password
                String username = parts[1];
                String password = parts[2];
                try {
                    // dùng UserService để login — có BCrypt check, không bypass
                    User user = userService.login(username, password);
                    currentUser = user;
                    // AuctionClient expect: "CONFIRMED|role|username"
                    sendMessage("CONFIRMED|" + user.getRole() + "|" + user.getUsername());
                } catch (RuntimeException e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "BID": {
                // BID|auctionId|amount
                if (!requireLoggedIn()) break;
                int    auctionId = Integer.parseInt(parts[1]);
                double amount    = Double.parseDouble(parts[2]);
                Auction auction  = findAuction(auctionId);
                if (auction == null) break;

                try {
                    auctionService.placeBid(auction, (Bidder) currentUser, amount);
                    String update = "UPDATE|" + auctionId + "|"
                            + auction.getCurrentPrice() + "|"
                            + currentUser.getUsername();
                    AuctionServer.broadCast(update);
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "AUTOBID": {
                // AUTOBID|auctionId|maxBid|increment
                if (!requireLoggedIn()) break;
                int    auctionId = Integer.parseInt(parts[1]);
                double maxBid    = Double.parseDouble(parts[2]);
                double increment = Double.parseDouble(parts[3]);
                Auction auction  = findAuction(auctionId);
                if (auction == null) break;

                try {
                    AutoBid autoBid = new AutoBid((Bidder) currentUser, maxBid, increment);
                    auctionService.registerAutoBid(auction, autoBid);
                    sendMessage("AUTOBID_REGISTERED");
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "STATUS": {
                // STATUS|auctionId
                int     auctionId = Integer.parseInt(parts[1]);
                Auction auction   = auctionService.findbyId(auctionId);
                if (auction != null) {
                    sendMessage("STATUS|" + auction.getStatus() + "|" + auction.getCurrentPrice());
                } else {
                    sendMessage("ERROR|Auction not found");
                }
                break;
            }

            default:
                sendMessage("ERROR|Unknown command: " + command);
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private boolean requireLoggedIn() {
        if (currentUser == null) {
            sendMessage("ERROR|Not authenticated");
            return false;
        }
        return true;
    }

    private Auction findAuction(int id) {
        Auction a = auctionService.findbyId(id);
        if (a == null) sendMessage("ERROR|Auction #" + id + " not found");
        return a;
    }

    private void closeConnection() {
        try {
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}