package org.example.server;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.coordinator.BiddingCoordinator;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.factory.ServiceFactory;
import org.example.service.UserService;
import org.example.util.AutoBid;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, AuctionObserver {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private AuctionService auctionService;

    private AuctionService getAuctionService() {
        if (auctionService == null) {
            auctionService = ServiceFactory.getInstance().getAuctionService();
        }
        return auctionService;
    }


    public ClientHandler(Socket socket){
        this.socket = socket;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // FIX: out phai dung socket.getOutputStream() chu khong phai System.out
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String line;
        try {
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

    public void update(Auction auction, BidTransaction bid, double minIncrement) {
        String updateMessage = "UPDATE|" + auction.getId() + "|"
                + bid.getAmount() + "|"
                + bid.getBidder().getUsername() + "|"
                + bid.getType();
        sendMessage(updateMessage);
    }

    private void handleCommand(String rawline) {
        String[] parts = rawline.split("\\|");
        String command = parts[0];
        try {
        switch (command) {

            case "LOGIN": {
                // "LOGIN|username|password"
                if (parts.length < 3) { sendMessage("ERROR|Thiếu thông tin đăng nhập"); return; }
                String username = parts[1];
                String password = parts[2];

                UserService userService = ServiceFactory.getInstance().getUserService();
                User user = userService.findUser(username);

                if (user == null) {
                    sendMessage("ERROR|Không tìm thấy tài khoản");
                } else if (!BCrypt.checkpw(password, user.getPassword())) {
                    sendMessage("ERROR|Sai mật khẩu");
                } else {
                    currentUser = user;
                    sendMessage("OK|" + user.getRole() + "|" + user.getUsername());
                }
                break;
            }

            case "REGISTER": {
                // "REGISTER|username|password|role"  (role: BIDDER hoặc SELLER)
                if (parts.length < 4) { sendMessage("ERROR|Thiếu thông tin đăng ký"); return; }
                String username = parts[1];
                String password = parts[2];
                String role     = parts[3];

                User newUser;
                if ("SELLER".equalsIgnoreCase(role)) {
                    newUser = new Seller(username, password);
                } else {
                    newUser = new Bidder(username, password);
                }

                UserService userService = ServiceFactory.getInstance().getUserService();
                String result = userService.register(newUser);

                if ("Register success".equals(result)) {
                    sendMessage("REGISTER_OK");
                } else {
                    // tra ve loi cu the tu UserService (username ton tai, password yeu, v.v.)
                    sendMessage("REGISTER_ERROR|" + result);
                }
                break;
            }

            case "BID": {
                if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                if (parts.length < 3)    { sendMessage("ERROR|Thiếu thông tin bid"); return; }
                try {
                    int    auctionId = Integer.parseInt(parts[1]);
                    double amount    = Double.parseDouble(parts[2]);

                    BiddingCoordinator coordinator = auctionService.getCoordinator(auctionId);
                    if (coordinator == null) { sendMessage("ERROR|Auction not found"); return; }

                    auctionService.placeBid(coordinator.getAuction(), (Bidder) currentUser, amount);
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "AUTOBID": {
                // "AUTOBID|auctionId|maxbid|increment"
                if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                if (parts.length < 4)    { sendMessage("ERROR|Thiếu thông tin autobid"); return; }
                try {
                    int    auctionId = Integer.parseInt(parts[1]);
                    double maxBid    = Double.parseDouble(parts[2]);
                    double increment = Double.parseDouble(parts[3]);

                    BiddingCoordinator coordinator = auctionService.getCoordinator(auctionId);
                    if (coordinator == null) { sendMessage("ERROR|Auction not found"); return; }

                    AutoBid autoBid = new AutoBid((Bidder) currentUser, maxBid, increment);
                    auctionService.registerAutoBid(coordinator.getAuction(), autoBid);
                    coordinator.getNotifier().addObserver(this);
                    sendMessage("SUCCESS|AutoBid registered");
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "STATUS": {
                if (parts.length < 2) { sendMessage("ERROR|Thiếu auction ID"); return; }
                int     auctionId = Integer.parseInt(parts[1]);
                Auction auction   = findbyId(auctionId);
                if (auction != null) {
                    sendMessage("STATUS|" + auction.getStatus() + "|" + auction.getCurrentPrice());
                } else {
                    sendMessage("ERROR|Auction not found");
                }
                break;
            }
            case "ADD_ITEM": {
                // "ADD_ITEM|type|name|description|startPrice|startTime|endTime"
                if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                if (!currentUser.getRole().equals(org.example.domain.user.User.UserRole.SELLER.name())) {
                    sendMessage("ERROR|Chỉ Seller mới được thêm item");
                    return;
                }
                if (parts.length < 7) { sendMessage("ERROR|Thiếu thông tin item"); return; }

                String type        = parts[1];
                String name        = parts[2];
                String description = parts[3];
                double startPrice  = Double.parseDouble(parts[4]);
                java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(parts[5]);
                java.time.LocalDateTime endTime   = java.time.LocalDateTime.parse(parts[6]);

                org.example.service.ItemService itemService =
                        ServiceFactory.getInstance().getItemService();
                org.example.domain.item.Item item = itemService.CreateItem(
                        type, name, description, startPrice, startTime, endTime,
                        (org.example.domain.user.Seller) currentUser);

                sendMessage("ITEM_ADDED|" + item.getId() + "|" + item.getName());
                break;
            }

            case "START_AUCTION": {
                // "START_AUCTION|itemId"
                if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                if (!currentUser.getRole().equals(org.example.domain.user.User.UserRole.SELLER.name())) {
                    sendMessage("ERROR|Chỉ Seller mới được tạo phiên đấu giá");
                    return;
                }
                if (parts.length < 2) { sendMessage("ERROR|Thiếu itemId"); return; }

                String itemId = parts[1];
                org.example.service.ItemService itemService =
                        ServiceFactory.getInstance().getItemService();
                org.example.domain.item.Item item = itemService.getItemById(itemId);
                if (item == null) { sendMessage("ERROR|Không tìm thấy item"); return; }

                org.example.domain.auction.Auction auction =
                        new org.example.domain.auction.Auction(item);
                getAuctionService().StartAuction(auction);

                // Thêm tất cả client đang kết nối làm observer để nhận UPDATE
                for (ClientHandler client : AuctionServer.connectClient) {
                    getAuctionService().addObserverToAuction(auction, client);
                }

                sendMessage("AUCTION_STARTED|" + auction.getId());
                // Broadcast để tất cả client biết có phiên mới
                AuctionServer.broadCast("NEW_AUCTION|" + auction.getId()
                        + "|" + item.getName()
                        + "|" + item.getCurrentPrice()
                        + "|" + item.getEndTime()
                        + "|" + item.getStartTime());
                break;
            }

            case "MY_ITEMS": {
                // "MY_ITEMS" — lấy danh sách item của seller hiện tại
                if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                if (!currentUser.getRole().equals(org.example.domain.user.User.UserRole.SELLER.name())) {
                    sendMessage("ERROR|Chỉ Seller mới có item"); return;
                }
                org.example.service.ItemService itemService =
                        ServiceFactory.getInstance().getItemService();
                java.util.List<org.example.domain.item.Item> myItems =
                        itemService.getItemsBySeller((org.example.domain.user.Seller) currentUser);
                sendMessage("MY_ITEMS_LIST|" + myItems.size());
                for (org.example.domain.item.Item it : myItems) {
                    sendMessage("MY_ITEM|" + it.getId()
                            + "|" + it.getName()
                            + "|" + it.getStartPrice()
                            + "|" + it.getType()
                            + "|" + (it.getStartTime() != null ? it.getStartTime() : "")
                            + "|" + (it.getEndTime()   != null ? it.getEndTime()   : ""));
                }
                sendMessage("MY_ITEMS_END");
                break;
            }

            case "LIST_AUCTIONS": {
                // "LIST_AUCTIONS|status"  (OPEN = upcoming, RUNNING = ongoing)
                String status = parts.length > 1 ? parts[1] : "RUNNING";
                List<Auction> auctions = getAuctionService().getAuctionsByStatus(status);
                sendMessage("AUCTION_LIST|" + auctions.size());
                for (Auction a : auctions) {
                    // "AUCTION_ITEM|id|name|currentPrice|endTime|status"
                    sendMessage("AUCTION_ITEM|"
                            + a.getId() + "|"
                            + a.getItem().getName() + "|"
                            + a.getCurrentPrice() + "|"
                            + a.getItem().getEndTime() + "|"
                            + a.getStatus().name() + "|"
                            + a.getItem().getStartTime() + "|"
                            + a.getItem().getDescription()
                    );
                }
                sendMessage("AUCTION_LIST_END");
                break;
            }

            default:
                sendMessage("ERROR|Unknown command: " + command);
        }
    } catch (Exception e) {
        System.err.println("[SERVER ERROR] Exception handling command '" + command + "': " + e.getMessage());
        e.printStackTrace();
        sendMessage("ERROR|" + e.getMessage());
    }
}



    public void sendMessage(String msg) {
        // FIX: truoc do chi in ra console, khong gui ve client
        out.println(msg);
        System.out.println("[→ client] " + msg);
    }

    private void closeConnection() {
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Auction findbyId(int id) {
        return auctionService.findbyId(id);
    }
}