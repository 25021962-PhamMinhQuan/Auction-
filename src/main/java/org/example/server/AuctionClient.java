package org.example.server;

import javafx.application.Platform;
import org.example.domain.user.User;
import org.example.factory.ServiceFactory;
import org.example.uicontroller.MainScreenController;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AuctionClient {
    private static final String HOST = "172.236.140.98";
    private static final int    PORT = 2501;

    private Socket         socket;
    private BufferedReader in;
    private PrintWriter    out;

    private String currentUsername;
    private String currentRole;

    private BiConsumer<Boolean, String> loginCallback;
    private BiConsumer<Boolean, String> registerCallback;
    private BiConsumer<Boolean, String> addItemCallback;
    private BiConsumer<Boolean, String> startAuctionCallback;
    private Consumer<List<String[]>> openAuctionCallback;
    private Consumer<List<String[]>> runningAuctionCallback;
    private Consumer<List<String[]>>    myItemsCallback;
    private Consumer<List<String[]>> wonAuctionsCallback;
    private Consumer<List<String[]>> searchAuctionCallback;
    private BiConsumer<Boolean, String> deleteItemCallback;
    private BiConsumer<Boolean, String> itemUpdateCallback;
    private BiConsumer<Boolean, String> auctionActionCallback;
    private final List<String[]> pendingMyItems  = new ArrayList<>();
    private final List<String[]> pendingWonAuctions = new ArrayList<>();
    private org.example.uicontroller.ItemBidingUIController activeBidController;
    private Runnable newAuctionListener;
    private Consumer<List<String>> suggestCallback;
    private Consumer<List<String[]>> bidHistoryCallback;
    private final List<String[]> pendingHistory = new ArrayList<>();
    private Consumer<List<String[]>> categoryCallback;
    private final List<String[]> pendingOpen    = new ArrayList<>();
    private final List<String[]> pendingRunning = new ArrayList<>();
    private User currentUser;

    // ──────────────── Singleton ────────────────

    public static volatile AuctionClient instance;

    public static AuctionClient getInstance() {
        if (instance == null) {
            synchronized (AuctionClient.class) {
                if (instance == null) instance = new AuctionClient();
            }
        }
        return instance;
    }

    private AuctionClient() {}

    // ──────────────── Kết nối ────────────────

    public void connect(javafx.stage.Stage stage) throws IOException {
        // Chỉ tạo socket mới nếu chưa kết nối
        if (socket != null && !socket.isClosed()) {
            if (out != null && out.checkError()) {
                // Connection chết nhưng socket chưa isClosed() → reconnect
                try { socket.close(); } catch (IOException ignored) {}
                socket = null; in = null; out = null;
            } else {
                return;
            }
        }

        socket = new Socket(HOST, PORT);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Server connected");

        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) handleServerMessage(line);
            } catch (IOException e) {
                System.out.println("Server disconnected");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    // ──────────────── Xử lý phản hồi server ────────────────

    private void handleServerMessage(String line) {
        System.out.println("[← server] " + line);
        String[] parts = line.split("\\|");
        String type = parts[0];

        switch (type) {
            case "OK": {
                if (parts.length < 3) return;

                currentRole = parts[1];
                currentUsername = parts[2];

                BiConsumer<Boolean, String> cb = loginCallback;
                loginCallback = null;

                if (cb != null) cb.accept(true, null);

                try {
                    User user = ServiceFactory.getInstance()
                            .getUserService()
                            .findUser(currentUsername);

                    setCurrentUser(user);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            }
            case "REGISTER_OK": {
                BiConsumer<Boolean, String> cb = registerCallback;
                registerCallback = null;
                if (cb != null) cb.accept(true, "Đăng ký thành công");
                break;
            }
            case "REGISTER_ERROR": {
                String reason = parts.length > 1 ? parts[1] : "Đăng ký thất bại";
                BiConsumer<Boolean, String> cb = registerCallback;
                registerCallback = null;
                if (cb != null) cb.accept(false, reason);
                break;
            }
            case "ERROR": {
                String msg = parts.length > 1 ? parts[1] : "Lỗi không xác định";
                BiConsumer<Boolean, String> cb = loginCallback;
                if (cb != null) {
                    loginCallback = null;
                    cb.accept(false, msg);
                } else if (itemUpdateCallback != null) {
                    BiConsumer<Boolean, String> actionCb = itemUpdateCallback;
                    itemUpdateCallback = null;
                    Platform.runLater(() -> actionCb.accept(false, msg));
                } else if (auctionActionCallback != null) {
                    BiConsumer<Boolean, String> actionCb = auctionActionCallback;
                    auctionActionCallback = null;
                    Platform.runLater(() -> actionCb.accept(false, msg));
                } else {
                    Platform.runLater(() -> {
                        // Ưu tiên hiện trên màn hình bid nếu đang mở
                        if (activeBidController != null) {
                            activeBidController.showError(msg);
                        } else {
                            // Fallback: Alert thông thường
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.WARNING);
                            alert.setTitle("Lỗi");
                            alert.setHeaderText(null);
                            alert.setContentText(msg);
                            alert.showAndWait();
                        }
                    });
                }
                break;
            }
            case "UPDATE": {
                if (parts.length < 4) return;
                int auctionId = Integer.parseInt(parts[1]);
                double newPrice = Double.parseDouble(parts[2]);
                String bidder = parts[3];
                final String newEndTime = parts.length > 4 ? parts[4] : null;
                Platform.runLater(() -> {
                    // Cập nhật màn hình bid
                    if (activeBidController != null) {
                        activeBidController.updatePrice(newPrice, bidder);
                        if (newEndTime != null)
                            activeBidController.updateEndTime(LocalDateTime.parse(newEndTime)); // anti-snipe
                    }
                    // Fix: MainScreenController phải nằm ngoài if ở trên
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.updateAuctionPrice(auctionId, newPrice, bidder,newEndTime);
                });
                break;
            }
            case "FINISHED": {
                if (parts.length < 4) return;
                int auctionId = Integer.parseInt(parts[1]);
                String winner = parts[2];
                double finalPrice = Double.parseDouble(parts[3]);
                Platform.runLater(() -> {
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.onAuctionFinished(auctionId, winner, finalPrice);
                });
                break;
            }
            case "ITEM_ADDED": {
                // "ITEM_ADDED|itemId|itemName"
                String itemId = parts.length > 1 ? parts[1] : "";
                String itemName = parts.length > 2 ? parts[2] : "";
                BiConsumer<Boolean, String> cb = addItemCallback;
                addItemCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, itemId + "|" + itemName));
                break;
            }
            case "AUCTION_STARTED": {
                // "AUCTION_STARTED|auctionId"
                String auctionId = parts.length > 1 ? parts[1] : "";
                BiConsumer<Boolean, String> cb = startAuctionCallback;
                startAuctionCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, auctionId));
                break;
            }
            case "NEW_AUCTION": {
                // Broadcast từ server khi có phiên đấu giá mới
                Platform.runLater(() -> {
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.onNewAuction();
                    if (newAuctionListener != null) newAuctionListener.run();
                });
                break;
            }
            case "ITEM_DELETED": {
                BiConsumer<Boolean, String> cb = deleteItemCallback;
                deleteItemCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, parts.length > 1 ? parts[1] : ""));
                break;
            }
            case "DELETE_ERROR": {
                String reason = parts.length > 1 ? parts[1] : "Xóa thất bại";
                BiConsumer<Boolean, String> cb = deleteItemCallback;
                deleteItemCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(false, reason));
                break;
            }
            case "MY_ITEMS_LIST": {
                synchronized (pendingMyItems) {
                    pendingMyItems.clear();
                }
                break;
            }
            case "MY_ITEM": {
                if (parts.length < 2) return;
                synchronized (pendingMyItems) {
                    pendingMyItems.add(parts);
                }
                break;
            }
            case "MY_ITEMS_END": {
                List<String[]> snapshot;
                synchronized (pendingMyItems) {
                    snapshot = new ArrayList<>(pendingMyItems);
                    pendingMyItems.clear();
                }
                Consumer<List<String[]>> cb = myItemsCallback;
                myItemsCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(snapshot));
                break;
            }
            case "WON_AUCTIONS_LIST": {
                synchronized (pendingWonAuctions) {
                    pendingWonAuctions.clear();
                }
                break;
            }
            case "WON_AUCTION": {
                if (parts.length < 2) return;
                synchronized (pendingWonAuctions) {
                    pendingWonAuctions.add(parts);
                }
                break;
            }
            case "WON_AUCTIONS_END": {
                List<String[]> snapshot;
                synchronized (pendingWonAuctions) {
                    snapshot = new ArrayList<>(pendingWonAuctions);
                    pendingWonAuctions.clear();
                }
                Consumer<List<String[]>> cb = wonAuctionsCallback;
                wonAuctionsCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(snapshot));
                break;
            }
            case "ITEM_UPDATED": {
                BiConsumer<Boolean, String> cb = itemUpdateCallback;
                itemUpdateCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, parts.length > 1 ? parts[1] : ""));
                break;
            }
            case "ITEM_UPDATE_ERROR": {
                String reason = parts.length > 1 ? parts[1] : "Update failed";
                BiConsumer<Boolean, String> cb = itemUpdateCallback;
                itemUpdateCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(false, reason));
                break;
            }
            case "AUCTION_CANCELLED":
            case "AUCTION_CLOSED": {
                BiConsumer<Boolean, String> cb = auctionActionCallback;
                auctionActionCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, parts.length > 1 ? parts[1] : ""));
                break;
            }
            case "AUCTION_ACTION_ERROR": {
                String reason = parts.length > 1 ? parts[1] : "Action failed";
                BiConsumer<Boolean, String> cb = auctionActionCallback;
                auctionActionCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(false, reason));
                break;
            }
            case "AUCTION_LIST": {
                // "AUCTION_LIST|count" followed by individual "AUCTION_ITEM|id|name|price|endTime|status" lines
                // handled by accumulation — nothing to do here; items arrive as AUCTION_ITEM
                break;
            }
            case "AUCTION_ITEM": {
                if (parts.length < 2) return;
                String itemStatus = parts.length > 5 ? parts[5] : "";
                if ("OPEN".equals(itemStatus)) {
                    synchronized (pendingOpen)    { pendingOpen.add(parts); }
                } else {
                    synchronized (pendingRunning) { pendingRunning.add(parts); }
                }
                break;
            }
            case "AUCTION_LIST_END": {
                String status = parts.length > 1 ? parts[1] : "";
                List<String[]> snapshot;
                Consumer<List<String[]>> cb;

                if ("OPEN".equals(status)) {
                    synchronized (pendingOpen) {
                        snapshot = new ArrayList<>(pendingOpen);
                        pendingOpen.clear();
                    }
                    cb = openAuctionCallback;
                    openAuctionCallback = null;
                } else if ("SEARCH".equals(status)) {
                    synchronized (pendingRunning) {
                        snapshot = new ArrayList<>(pendingRunning);
                        pendingRunning.clear();
                    }
                    cb = searchAuctionCallback;
                    searchAuctionCallback = null;
                } else if ("CATEGORY".equals(status)) {
                    synchronized (pendingRunning) {
                        snapshot = new ArrayList<>(pendingRunning);
                        pendingRunning.clear();
                    }
                    cb = categoryCallback;
                    categoryCallback = null;
                } else { // RUNNING
                    synchronized (pendingRunning) {
                        snapshot = new ArrayList<>(pendingRunning);
                        pendingRunning.clear();
                    }
                    cb = runningAuctionCallback;
                    runningAuctionCallback = null;
                }

                if (cb != null) {
                    final List<String[]> snap = snapshot;
                    Platform.runLater(() -> cb.accept(snap));
                }
                break;
            }

            case "AUTOBID_OK": {
                Platform.runLater(() -> {
                    if (activeBidController != null)
                        activeBidController.showAutoBidSuccess();
                });
                break;
            }

            case "BID_HISTORY_START": {
                synchronized (pendingHistory) { pendingHistory.clear(); }
                break;
            }
            case "BID_HISTORY_ITEM": {
                if (parts.length < 4) return;
                synchronized (pendingHistory) {
                    pendingHistory.add(new String[]{parts[1], parts[2], parts[3]});
                }
                break;
            }
            case "BID_HISTORY_END": {
                List<String[]> snapshot;
                synchronized (pendingHistory) {
                    snapshot = new ArrayList<>(pendingHistory);
                    pendingHistory.clear();
                }
                Consumer<List<String[]>> cb = bidHistoryCallback;
                bidHistoryCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(snapshot));
                break;
            }

            case "SUGGEST_RESULT": {
                List<String> names = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) names.add(parts[i]);
                Consumer<List<String>> cb2 = suggestCallback;
                suggestCallback = null;
                if (cb2 != null) Platform.runLater(() -> cb2.accept(names));
                break;
            }
        }
    }

    // ──────────────── Gửi lệnh ────────────────

    public void requestBidHistory(int auctionId, Consumer<List<String[]>> callback) {
        this.bidHistoryCallback = callback;
        sendCommand("GET_BID_HISTORY|" + auctionId);
    }

    public void requestAuctionsByCategory(String type, Consumer<List<String[]>> callback) {
        this.categoryCallback = callback;
        sendCommand("LIST_BY_CATEGORY|" + type);
    }

    public void requestSearchAuctions(String keyword, Consumer<List<String[]>> callback) {
        this.searchAuctionCallback = callback;
        sendCommand("SEARCH_AUCTIONS|" + keyword);
    }

    public void requestSuggestAuctions(String keyword, Consumer<List<String>> callback) {
        this.suggestCallback = callback;
        sendCommand("SUGGEST_AUCTIONS|" + keyword);
    }

    public void login(String username, String password) {
        sendCommand("LOGIN|" + username + "|" + password);
    }

    public void register(String username, String password, String role) {
        sendCommand("REGISTER|" + username + "|" + password + "|" + role);
    }
    public void disconnect() {
        currentUsername = null;
        currentRole     = null;
        activeBidController = null;
        newAuctionListener  = null;
        currentUser = null;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        socket = null; in = null; out = null;
        instance = null; // reset singleton để login lại tạo connection mới
    }

    public void placeBid(int auctionId, double amount) {
        sendCommand("BID|" + auctionId + "|" + amount);
    }

    public void registerAutoBid(int auctionId, double maxBid, double increment) {
        sendCommand("AUTOBID|" + auctionId + "|" + maxBid + "|" + increment);
    }

    public void requestAuctions(String status, Consumer<List<String[]>> callback) {
        if ("OPEN".equals(status)) {
            synchronized (pendingOpen)    { pendingOpen.clear(); }
            this.openAuctionCallback = callback;
        } else {
            synchronized (pendingRunning) { pendingRunning.clear(); }
            this.runningAuctionCallback = callback;
        }
        sendCommand("LIST_AUCTIONS|" + status);
    }

    public void addItem(String type, String name, String description,
                        double startPrice, String startTime, String endTime,  String imageUrl,
                        BiConsumer<Boolean, String> callback) {
        this.addItemCallback = callback;
        sendCommand("ADD_ITEM|" + type + "|" + name + "|" + description
                + "|" + startPrice + "|" + startTime + "|" + endTime
                + "|" + (imageUrl != null ? imageUrl : ""));
    }

    /** Seller khởi động phiên đấu giá cho item đã tạo */
    public void startAuction(String itemId, BiConsumer<Boolean, String> callback) {
        this.startAuctionCallback = callback;
        sendCommand("START_AUCTION|" + itemId);
    }
    public void deleteItem(String itemId, BiConsumer<Boolean, String> callback) {
        this.deleteItemCallback = callback;
        sendCommand("DELETE_ITEM|" + itemId);
    }

    public void updateScheduledItem(int auctionId, String name, String description,
                                    double startPrice, String startTime, String endTime,
                                    BiConsumer<Boolean, String> callback) {
        this.itemUpdateCallback = callback;
        sendCommand("UPDATE_ITEM|" + auctionId + "|" + sanitize(name) + "|" + sanitize(description)
                + "|" + startPrice + "|" + startTime + "|" + endTime);
    }

    public void cancelAuction(int auctionId, BiConsumer<Boolean, String> callback) {
        this.auctionActionCallback = callback;
        sendCommand("CANCEL_AUCTION|" + auctionId);
    }

    public void closeAuction(int auctionId, BiConsumer<Boolean, String> callback) {
        this.auctionActionCallback = callback;
        sendCommand("CLOSE_AUCTION|" + auctionId);
    }

    /** Lấy danh sách item của seller đang đăng nhập */
    public void requestMyItems(Consumer<List<String[]>> callback) {
        this.myItemsCallback = callback;
        sendCommand("MY_ITEMS");
    }

    public void requestWonAuctions(Consumer<List<String[]>> callback) {
        this.wonAuctionsCallback = callback;
        sendCommand("WON_AUCTIONS");
    }
    /** Listener được gọi khi server broadcast NEW_AUCTION */
    public void setNewAuctionListener(Runnable listener) {
        this.newAuctionListener = listener;
    }

    public void setLoginCallback(BiConsumer<Boolean, String> callback) {
        this.loginCallback = callback;
    }

    public void setRegisterCallback(BiConsumer<Boolean, String> callback) {
        this.registerCallback = callback;
    }

    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            System.out.println("[→ server] " + command);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", " ");
    }

    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentRole()     { return currentRole; }
    /** Gọi khi mở màn hình bid để nhận UPDATE real-time */
    public void setActiveBidController(org.example.uicontroller.ItemBidingUIController ctrl) {
        this.activeBidController = ctrl;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    /** Gọi khi đóng màn hình bid (back về main) */
    public void clearActiveBidController() {
        this.activeBidController = null;
    }
}
