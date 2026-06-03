package org.example.server;

import org.example.domain.auction.Auction;
import org.example.domain.auction.BidTransaction;
import org.example.coordinator.BiddingCoordinator;
import org.example.domain.item.Item;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;
import org.example.domain.user.User;
import org.example.observer.AuctionObserver;
import org.example.service.AuctionService;
import org.example.factory.ServiceFactory;
import org.example.service.ItemService;
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
            if (currentUser != null) {
                AuctionServer.loggedInUsers.remove(currentUser.getUsername());
            }
            AuctionServer.connectClient.remove(this);
            closeConnection();
        }
    }

    public void update(Auction auction, BidTransaction bid, double minIncrement) {
        String updateMessage = "UPDATE|" + auction.getId() + "|"
                + bid.getAmount() + "|"
                + bid.getBidder().getUsername() + "|"
                + auction.getItem().getEndTime();
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
                    } else if (userService.isLocked(user)) {
                        sendMessage("ERROR|Tài khoản đã bị khóa");
                    } else if (!AuctionServer.loggedInUsers.add(user.getUsername())) {
                        sendMessage("ERROR|Tài khoản đã đăng nhập ở nơi khác");
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

                        BiddingCoordinator coordinator = getAuctionService().getCoordinator(auctionId);
                        if (coordinator == null) { sendMessage("ERROR|Auction not found"); return; }

                        getAuctionService().placeBid(coordinator.getAuction(), (Bidder) currentUser, amount);
                        // Sau placeBid, autobid loop co the da chay -> lay highestBidder thuc su
                        Auction a = coordinator.getAuction();
                        String highestName = a.getHighestBidder() != null
                                ? a.getHighestBidder().getUsername()
                                : currentUser.getUsername();
                        AuctionServer.broadCast("UPDATE|" + auctionId
                                + "|" + a.getCurrentPrice()
                                + "|" + highestName + "|" + a.getItem().getEndTime());
                        // Gửi balance mới về cho client vừa bid
                        sendMessage("BALANCE_UPDATE|" + currentUser.getBalance());
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

                        BiddingCoordinator coordinator = getAuctionService().getCoordinator(auctionId);
                        if (coordinator == null) { sendMessage("ERROR|Auction not found"); return; }

                        if (!(currentUser instanceof Bidder)) {
                            sendMessage("ERROR|Chỉ Bidder mới được dùng AutoBid");
                            return;
                        }

                        AutoBid autoBid = new AutoBid((Bidder) currentUser, maxBid, increment);
                        getAuctionService().registerAutoBid(coordinator.getAuction(), autoBid);
                        coordinator.getNotifier().addObserver(this);

                        // Trigger autobid ngay nếu giá hiện tại thấp hơn maxBid
                        Auction a = coordinator.getAuction();
                        if (a.getCurrentPrice() < maxBid) {
                            coordinator.triggerAutoBid();
                            // Lay highest bidder THUC SU sau khi autobid loop ket thuc
                            String winner = a.getHighestBidder() != null
                                    ? a.getHighestBidder().getUsername()
                                    : currentUser.getUsername();
                            AuctionServer.broadCast("UPDATE|" + auctionId
                                    + "|" + a.getCurrentPrice()
                                    + "|" + winner
                                    + "|" + a.getItem().getEndTime());
                        }

                        sendMessage("AUTOBID_OK|AutoBid đã được đăng ký");
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
                    String imageUrl    = parts.length > 7 ? parts[7] : null;

                    org.example.service.ItemService itemService =
                            ServiceFactory.getInstance().getItemService();
                    org.example.domain.item.Item item = itemService.CreateItem(
                            type, name, description, startPrice, startTime, endTime, imageUrl,
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
                    if (item.getStartTime() == null || item.getEndTime() == null) {
                        sendMessage("ERROR|Item chưa có lịch đấu giá");
                        return;
                    }
                    if (item.getEndTime().isBefore(AuctionService.now())) {
                        sendMessage("ERROR|Thời gian kết thúc đã qua");
                        return;
                    }
                    if (!"APPROVED".equalsIgnoreCase(item.getStatus())) {
                        sendMessage("ERROR|Item đang chờ Admin duyệt");
                        return;
                    }

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
                            + "|" + item.getStartTime()
                            + "|" + auction.getStatus().name());
                    break;
                }
                case "DELETE_ITEM": {
                    if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.SELLER.name())
                            && !currentUser.getRole().equals(User.UserRole.ADMIN.name())) {
                        sendMessage("DELETE_ERROR|Không có quyền xóa item"); return;
                    }
                    if (parts.length < 2) { sendMessage("DELETE_ERROR|Thiếu itemId"); return; }

                    String deleteId = parts[1];
                    ItemService deleteService = ServiceFactory.getInstance().getItemService();
                    Item toDelete = deleteService.getItemById(deleteId);
                    if (toDelete == null) { sendMessage("DELETE_ERROR|Không tìm thấy item"); return; }

                    try {
                        deleteService.deleteItem(deleteId, currentUser);
                        sendMessage("ITEM_DELETED|" + deleteId);
                    } catch (Exception ex) {
                        sendMessage("DELETE_ERROR|" + ex.getMessage());
                    }
                    break;
                }
                case "MY_ITEMS": {
                    // "MY_ITEMS" — lấy danh sách item của seller hiện tại
                    if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                    if (!currentUser.getRole().equals(org.example.domain.user.User.UserRole.SELLER.name())) {
                        sendMessage("ERROR|Chỉ Seller mới có item"); return;
                    }
                    List<Auction> myAuctions = getAuctionService().findAuctionsBySeller(currentUser.getId());
                    sendMessage("MY_ITEMS_LIST|" + myAuctions.size());
                    for (Auction a : myAuctions) {
                        sendAuctionPayload("MY_ITEM", a);
                    }
                    sendMessage("MY_ITEMS_END");
                    break;
                }

                case "UPDATE_ITEM": {
                    if (currentUser == null) { sendMessage("ERROR|ChÆ°a Ä‘Äƒng nháº­p"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.SELLER.name())) {
                        sendMessage("ITEM_UPDATE_ERROR|Only seller can edit item");
                        return;
                    }
                    if (parts.length < 7) { sendMessage("ITEM_UPDATE_ERROR|Missing item info"); return; }
                    int auctionId = Integer.parseInt(parts[1]);
                    Auction auction = getAuctionService().findbyId(auctionId);
                    if (auction == null) { sendMessage("ITEM_UPDATE_ERROR|Auction not found"); return; }
                    Item item = auction.getItem();
                    item.setName(parts[2]);
                    item.setDescription(parts[3]);
                    item.setStartPrice(Double.parseDouble(parts[4]));
                    item.setCurrentPrice(Double.parseDouble(parts[4]));
                    item.setStartTime(java.time.LocalDateTime.parse(parts[5]));
                    item.setEndTime(java.time.LocalDateTime.parse(parts[6]));
                    getAuctionService().updateScheduledAuction(auction, currentUser);
                    sendMessage("ITEM_UPDATED|" + auctionId);
                    AuctionServer.broadCast("NEW_AUCTION|" + auctionId
                            + "|" + item.getName()
                            + "|" + auction.getCurrentPrice()
                            + "|" + item.getEndTime()
                            + "|" + item.getStartTime()
                            + "|" + auction.getStatus().name());
                    break;
                }

                case "CANCEL_AUCTION": {
                    if (currentUser == null) { sendMessage("ERROR|ChÆ°a Ä‘Äƒng nháº­p"); return; }
                    if (parts.length < 2) { sendMessage("AUCTION_ACTION_ERROR|Missing auctionId"); return; }
                    int auctionId = Integer.parseInt(parts[1]);
                    Auction auction = getAuctionService().findbyId(auctionId);
                    if (auction == null) { sendMessage("AUCTION_ACTION_ERROR|Auction not found"); return; }
                    getAuctionService().cancelAuction(auction, currentUser);
                    sendMessage("AUCTION_CANCELLED|" + auctionId);
                    AuctionServer.broadCast("NEW_AUCTION|" + auctionId
                            + "|" + auction.getItem().getName()
                            + "|" + auction.getCurrentPrice()
                            + "|" + auction.getItem().getEndTime()
                            + "|" + auction.getItem().getStartTime()
                            + "|" + auction.getStatus().name());
                    break;
                }

                case "CLOSE_AUCTION": {
                    if (currentUser == null) { sendMessage("ERROR|ChÆ°a Ä‘Äƒng nháº­p"); return; }
                    if (parts.length < 2) { sendMessage("AUCTION_ACTION_ERROR|Missing auctionId"); return; }
                    int auctionId = Integer.parseInt(parts[1]);
                    Auction auction = getAuctionService().findbyId(auctionId);
                    if (auction == null) { sendMessage("AUCTION_ACTION_ERROR|Auction not found"); return; }
                    getAuctionService().markPaid(auction, currentUser);
                    sendMessage("AUCTION_CLOSED|" + auctionId);
                    AuctionServer.broadCast("FINISHED|" + auctionId + "|"
                            + (auction.getHighestBidder() != null ? auction.getHighestBidder().getUsername() : "")
                            + "|" + auction.getCurrentPrice());
                    break;
                }

                case "WON_AUCTIONS": {
                    if (currentUser == null) { sendMessage("ERROR|ChÆ°a Ä‘Äƒng nháº­p"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.BIDDER.name())) {
                        sendMessage("ERROR|Only bidder can view won auctions"); return;
                    }
                    List<Auction> wonAuctions = getAuctionService().findWonAuctions(currentUser.getId());
                    sendMessage("WON_AUCTIONS_LIST|" + wonAuctions.size());
                    for (Auction a : wonAuctions) {
                        sendAuctionPayload("WON_AUCTION", a);
                    }
                    sendMessage("WON_AUCTIONS_END");
                    break;
                }

                case "SEARCH_AUCTIONS": {
                    // "SEARCH_AUCTIONS|keyword"
                    String keyword = parts[1];
                    List<Auction> results = getAuctionService().searchByName(keyword);
                    sendMessage("AUCTION_LIST|" + results.size());
                    for (Auction a : results) {
                        sendMessage("AUCTION_ITEM|"
                                + a.getId()          + "|"
                                + a.getItem().getName()        + "|"
                                + a.getCurrentPrice()          + "|"
                                + a.getItem().getEndTime()         + "|"
                                + a.getStatus().name()                + "|"
                                + a.getItem().getStartTime()       + "|"
                                + a.getItem().getDescription() + "|"
                                + (a.getItem().getImageUrl() != null ? a.getItem().getImageUrl() : ""));
                    }
                    sendMessage("AUCTION_LIST_END|SEARCH");
                    break;
                }

                case "SUGGEST_AUCTIONS": {
                    String keyword = parts.length > 1 ? parts[1] : "";
                    List<Auction> suggestions = getAuctionService().searchByName(keyword);
                    StringBuilder sb = new StringBuilder("SUGGEST_RESULT");
                    int max = Math.min(suggestions.size(), 8);
                    for (int i = 0; i < max; i++) {
                        sb.append("|").append(suggestions.get(i).getItem().getName());
                    }
                    sendMessage(sb.toString());
                    break;
                }

                case "LIST_BY_CATEGORY": {
                    String type = parts.length > 1 ? parts[1] : "";
                    List<Auction> results = getAuctionService().searchByType(type);
                    sendMessage("AUCTION_LIST|" + results.size());
                    for (Auction a : results) {
                        sendMessage("AUCTION_ITEM|"
                                + a.getId()                    + "|"
                                + a.getItem().getName()        + "|"
                                + a.getCurrentPrice()          + "|"
                                + a.getItem().getEndTime()     + "|"
                                + a.getStatus().name()         + "|"
                                + a.getItem().getStartTime()   + "|"
                                + a.getItem().getDescription() + "|"
                                + (a.getItem().getImageUrl() != null ? a.getItem().getImageUrl() : ""));
                    }
                    sendMessage("AUCTION_LIST_END|CATEGORY");
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
                                + a.getItem().getDescription() + "|"
                                + (a.getItem().getImageUrl() != null ? a.getItem().getImageUrl() : "")
                        );
                    }
                    sendMessage("AUCTION_LIST_END|" + status);  // ← gửi kèm status
                    break;
                }

                case "GET_BID_HISTORY": {
                    if (parts.length < 2) { sendMessage("ERROR|Thiếu auctionId"); return; }
                    int auctionId = Integer.parseInt(parts[1]);
                    List<String[]> history = getAuctionService().getBidHistory(auctionId);
                    sendMessage("BID_HISTORY_START|" + auctionId + "|" + history.size());
                    for (String[] row : history) {
                        // row = [username, amount, time, type]
                        String type = row.length > 3 && row[3] != null ? row[3] : "MANUAL";
                        sendMessage("BID_HISTORY_ITEM|" + row[0] + "|" + row[1] + "|" + row[2] + "|" + type);
                    }
                    sendMessage("BID_HISTORY_END|" + auctionId);
                    break;
                }
                case "APPROVE_DEPOSIT": {
                    // "APPROVE_DEPOSIT|requestId"
                    if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.ADMIN.name())) {
                        sendMessage("ERROR|Chỉ Admin mới được duyệt nạp tiền"); return;
                    }
                    if (parts.length < 2) { sendMessage("ERROR|Thiếu requestId"); return; }

                    int depositReqId = Integer.parseInt(parts[1]);
                    org.example.service.DepositService depositSvc =
                            ServiceFactory.getInstance().getDepositService();

                    // Lấy thông tin request để biết userId và amount trước khi approve
                    org.example.domain.user.DepositRequest depositReq =
                            depositSvc.getAllRequests().stream()
                                    .filter(r -> r.getId() == depositReqId)
                                    .findFirst().orElse(null);

                    if (depositReq == null) { sendMessage("ERROR|Không tìm thấy yêu cầu nạp tiền"); return; }
                    if (depositReq.getStatus() != org.example.domain.user.DepositRequest.Status.PENDING) {
                        sendMessage("ERROR|Yêu cầu không ở trạng thái chờ duyệt"); return;
                    }

                    depositSvc.approve(depositReqId);
                    sendMessage("DEPOSIT_APPROVED|" + depositReqId);

                    // Lấy balance mới từ DB rồi push BALANCE_UPDATE về đúng client của user đó
                    UserService depositUserSvc = ServiceFactory.getInstance().getUserService();
                    User depositTargetUser = depositUserSvc.findUserById(depositReq.getUserId());
                    if (depositTargetUser != null) {
                        double newBalance = depositTargetUser.getBalance();
                        for (ClientHandler client : AuctionServer.connectClient) {
                            if (client.currentUser != null
                                    && client.currentUser.getId().equals(depositReq.getUserId())) {
                                client.currentUser.setBalance(newBalance);
                                client.sendMessage("BALANCE_UPDATE|" + newBalance);
                                break;
                            }
                        }
                    }
                    break;
                }

                case "REJECT_DEPOSIT": {
                    // "REJECT_DEPOSIT|requestId"
                    if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.ADMIN.name())) {
                        sendMessage("ERROR|Chỉ Admin mới được từ chối nạp tiền"); return;
                    }
                    if (parts.length < 2) { sendMessage("ERROR|Thiếu requestId"); return; }

                    int rejectReqId = Integer.parseInt(parts[1]);
                    org.example.service.DepositService rejectDepositSvc =
                            ServiceFactory.getInstance().getDepositService();

                    org.example.domain.user.DepositRequest rejectReq =
                            rejectDepositSvc.getAllRequests().stream()
                                    .filter(r -> r.getId() == rejectReqId)
                                    .findFirst().orElse(null);

                    if (rejectReq == null) { sendMessage("ERROR|Không tìm thấy yêu cầu nạp tiền"); return; }
                    if (rejectReq.getStatus() != org.example.domain.user.DepositRequest.Status.PENDING) {
                        sendMessage("ERROR|Yêu cầu không ở trạng thái chờ duyệt"); return;
                    }

                    rejectDepositSvc.reject(rejectReqId);
                    sendMessage("DEPOSIT_REJECTED|" + rejectReqId);

                    // Notify user bị từ chối (tuỳ chọn — client có thể dùng để hiển thị thông báo)
                    for (ClientHandler client : AuctionServer.connectClient) {
                        if (client.currentUser != null
                                && client.currentUser.getId().equals(rejectReq.getUserId())) {
                            client.sendMessage("DEPOSIT_REQUEST_REJECTED|" + rejectReqId);
                            break;
                        }
                    }
                    break;
                }

                case "APPROVE_ITEM": {
                    // "APPROVE_ITEM|itemId"
                    if (currentUser == null) { sendMessage("ERROR|Chưa đăng nhập"); return; }
                    if (!currentUser.getRole().equals(User.UserRole.ADMIN.name())) {
                        sendMessage("ERROR|Chỉ Admin mới được duyệt item"); return;
                    }
                    if (parts.length < 2) { sendMessage("ERROR|Thiếu itemId"); return; }

                    String itemId = parts[1];
                    ItemService itemSvc = ServiceFactory.getInstance().getItemService();
                    itemSvc.approveItem(itemId, currentUser);

                    Item approvedItem = itemSvc.getItemById(itemId);
                    getAuctionService().activateApprovedItem(approvedItem);

                    sendMessage("ITEM_APPROVED|" + itemId);
                    // Broadcast để các client khác biết có auction mới/sắp tới
                    Auction newAuction = getAuctionService().findByItemId(itemId);
                    if (newAuction != null) {
                        AuctionServer.broadCast("NEW_AUCTION|" + newAuction.getId()
                                + "|" + approvedItem.getName()
                                + "|" + newAuction.getCurrentPrice()
                                + "|" + approvedItem.getEndTime()
                                + "|" + approvedItem.getStartTime()
                                + "|" + newAuction.getStatus().name());
                    }
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

    private void sendAuctionPayload(String prefix, Auction auction) {
        Item item = auction.getItem();
        sendMessage(prefix + "|"
                + auction.getId() + "|"
                + item.getId() + "|"
                + safe(item.getName()) + "|"
                + auction.getCurrentPrice() + "|"
                + safe(item.getType()) + "|"
                + (item.getStartTime() != null ? item.getStartTime() : "") + "|"
                + (item.getEndTime() != null ? item.getEndTime() : "") + "|"
                + auction.getStatus().name() + "|"
                + safe(item.getDescription()) + "|"
                + safe(item.getImageUrl()) + "|"
                + (auction.getHighestBidder() != null ? safe(auction.getHighestBidder().getUsername()) : ""));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", " ");
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
        return getAuctionService().findbyId(id);
    }
}