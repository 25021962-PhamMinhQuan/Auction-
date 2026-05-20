package org.example.server;

import org.example.coordinator.BiddingCoordinator;
import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.domain.item.Item;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.factory.ServiceFactory;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.service.ItemService;
import org.example.service.UserService;
import org.example.util.AutoBid;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket       socket;
    private BufferedReader     in;
    private PrintWriter        out;
    private User               currentUser;

    private final AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
    private final UserService    userService    = ServiceFactory.getInstance().getUserService();
    private final ItemService    itemService    = ServiceFactory.getInstance().getItemService();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    // Observer: khi có bid mới thì gửi UPDATE về client này
    @Override
    public void update(Auction auction, BidTransaction bid, double minIncrement) {
        sendMessage("UPDATE|" + auction.getId()
                + "|" + bid.getAmount()
                + "|" + bid.getBidder().getUsername());
    }

    private void handleCommand(String rawLine) {
        String[] parts   = rawLine.split("\\|");
        String   command = parts[0];

        switch (command) {

            case "LOGIN": {
                if (parts.length < 3) { sendMessage("ERROR|Thiếu thông tin"); return; }
                String username = parts[1];
                String password = parts[2];

                User user = userService.findUser(username);
                if (user == null) {
                    sendMessage("ERROR|Không tìm thấy tài khoản");
                } else if (!BCrypt.checkpw(password, user.getPassword())) {
                    sendMessage("ERROR|Sai mật khẩu");
                } else {
                    currentUser = user;
                    // Đăng ký observer để nhận UPDATE real-time
                    registerAsObserverForRunningAuctions();
                    sendMessage("OK|" + user.getRole() + "|" + user.getUsername());
                }
                break;
            }

            case "REGISTER": {
                // "REGISTER|username|password|role"
                if (parts.length < 4) { sendMessage("ERROR|Thiếu thông tin"); return; }
                String username = parts[1];
                String password = parts[2];
                String role     = parts[3];

                User newUser = "SELLER".equalsIgnoreCase(role)
                        ? new Seller(username, password)
                        : new Bidder(username, password);

                String result = userService.register(newUser);
                if ("Register success".equals(result)) {
                    sendMessage("REGISTER_OK");
                } else {
                    sendMessage("REGISTER_ERROR|" + result);
                }
                break;
            }

            case "LIST_AUCTIONS": {
                // "LIST_AUCTIONS|status"  (OPEN hoặc RUNNING)
                if (parts.length < 2) { sendMessage("ERROR|Thiếu status"); return; }
                String status = parts[1];

                List<Auction> auctions = auctionService.getAuctionsByStatus(status);
                for (Auction a : auctions) {
                    Item item = a.getItem();
                    // format: AUCTION_ITEM|id|name|price|endTime|status|startTime|description
                    sendMessage("AUCTION_ITEM"
                            + "|" + a.getId()
                            + "|" + item.getName()
                            + "|" + item.getCurrentPrice()
                            + "|" + item.getEndTime().format(ISO)
                            + "|" + a.getStatus().name()
                            + "|" + item.getStartTime().format(ISO)
                            + "|" + item.getDescription());
                }
                sendMessage("AUCTION_LIST_END");
                break;
            }

            case "MY_ITEMS": {
                // Chỉ seller mới gọi được
                if (!requireLoggedIn()) return;
                if (!(currentUser instanceof Seller)) {
                    sendMessage("ERROR|Chỉ seller mới có item");
                    return;
                }
                List<Item> items = itemService.getItemsBySeller((Seller) currentUser);
                for (Item item : items) {
                    // format: MY_ITEM|id|name|startPrice|type|startTime|endTime
                    sendMessage("MY_ITEM"
                            + "|" + item.getId()
                            + "|" + item.getName()
                            + "|" + item.getStartPrice()
                            + "|" + item.getType()
                            + "|" + item.getStartTime().format(ISO)
                            + "|" + item.getEndTime().format(ISO));
                }
                sendMessage("MY_ITEMS_END");
                break;
            }

            case "ADD_ITEM": {
                // "ADD_ITEM|type|name|description|price|startTime|endTime"
                if (!requireLoggedIn()) return;
                if (!(currentUser instanceof Seller)) {
                    sendMessage("ERROR|Chỉ seller mới có thể thêm item");
                    return;
                }
                if (parts.length < 7) { sendMessage("ERROR|Thiếu thông tin item"); return; }
                try {
                    String        type        = parts[1];
                    String        name        = parts[2];
                    String        description = parts[3];
                    double        price       = Double.parseDouble(parts[4]);
                    LocalDateTime startTime   = LocalDateTime.parse(parts[5], ISO);
                    LocalDateTime endTime     = LocalDateTime.parse(parts[6], ISO);

                    Item item = itemService.CreateItem(
                            type, name, description, price,
                            startTime, endTime, (Seller) currentUser);

                    sendMessage("ITEM_ADDED|" + item.getId() + "|" + item.getName());
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "START_AUCTION": {
                // "START_AUCTION|itemId"
                if (!requireLoggedIn()) return;
                if (!(currentUser instanceof Seller)) {
                    sendMessage("ERROR|Chỉ seller mới có thể bắt đầu đấu giá");
                    return;
                }
                if (parts.length < 2) { sendMessage("ERROR|Thiếu item ID"); return; }
                try {
                    String itemId = parts[1];
                    Item   item   = itemService.getItemById(itemId);
                    if (item == null) { sendMessage("ERROR|Không tìm thấy item"); return; }

                    Auction auction = new Auction(item);
                    auctionService.StartAuction(auction);

                    // Đăng ký observer để nhận cập nhật
                    auctionService.addObserverToAuction(auction, this);

                    sendMessage("AUCTION_STARTED|" + auction.getId());

                    // Broadcast để tất cả client refresh danh sách
                    AuctionServer.broadCast("NEW_AUCTION");
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "BID": {
                if (!requireLoggedIn()) return;
                if (parts.length < 3) { sendMessage("ERROR|Thiếu thông tin bid"); return; }
                try {
                    int    auctionId = Integer.parseInt(parts[1]);
                    double amount    = Double.parseDouble(parts[2]);

                    BiddingCoordinator coord = auctionService.getCoordinator(auctionId);
                    if (coord == null) { sendMessage("ERROR|Auction not found"); return; }

                    auctionService.placeBid(coord.getAuction(), (Bidder) currentUser, amount);

                    // Broadcast UPDATE cho tất cả client
                    Auction a = coord.getAuction();
                    AuctionServer.broadCast("UPDATE|" + auctionId
                            + "|" + a.getCurrentPrice()
                            + "|" + currentUser.getUsername());
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "AUTOBID": {
                if (!requireLoggedIn()) return;
                if (parts.length < 4) { sendMessage("ERROR|Thiếu thông tin autobid"); return; }
                try {
                    int    auctionId = Integer.parseInt(parts[1]);
                    double maxBid    = Double.parseDouble(parts[2]);
                    double increment = Double.parseDouble(parts[3]);

                    BiddingCoordinator coord = auctionService.getCoordinator(auctionId);
                    if (coord == null) { sendMessage("ERROR|Auction not found"); return; }

                    AutoBid autoBid = new AutoBid((Bidder) currentUser, maxBid, increment);
                    auctionService.registerAutoBid(coord.getAuction(), autoBid);

                    // Đăng ký observer để nhận UPDATE
                    coord.getNotifier().addObserver(this);
                    sendMessage("SUCCESS|AutoBid registered");
                } catch (Exception e) {
                    sendMessage("ERROR|" + e.getMessage());
                }
                break;
            }

            case "STATUS": {
                if (parts.length < 2) { sendMessage("ERROR|Thiếu auction ID"); return; }
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
        if (out != null) {
            out.println(msg);
            System.out.println("[→ client] " + msg);
        }
    }

    private boolean requireLoggedIn() {
        if (currentUser == null) {
            sendMessage("ERROR|Chưa đăng nhập");
            return false;
        }
        return true;
    }

    // Đăng ký observer cho tất cả auction đang RUNNING khi login
    private void registerAsObserverForRunningAuctions() {
        List<Auction> running = auctionService.getAuctionsByStatus("RUNNING");
        for (Auction a : running) {
            auctionService.addObserverToAuction(a, this);
        }
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