package org.example.server;

import javafx.application.Platform;
import org.example.uicontroller.MainScreenController;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AuctionClient {
    private static final String HOST = "localhost";
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
    private BiConsumer<Boolean, String> deleteItemCallback;
    private final List<String[]> pendingAuctions = new ArrayList<>();
    private final List<String[]> pendingMyItems  = new ArrayList<>();
    private org.example.uicontroller.ItemBidingUIController activeBidController;
    private Runnable newAuctionListener;

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
        if (socket != null && !socket.isClosed()) return;

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
                // "OK|ROLE|username"
                if (parts.length < 3) return;
                currentRole = parts[1];
                currentUsername = parts[2];

                // FIX: chỉ gọi callback, không tự openMainScreen()
                // LoginController sẽ tự chuyển scene trên đúng stage
                BiConsumer<Boolean, String> cb = loginCallback;
                loginCallback = null;
                if (cb != null) cb.accept(true, null);
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
                } else {
                    Platform.runLater(() -> System.err.println("Server error: " + msg));
                }
                break;
            }
            case "UPDATE": {
                if (parts.length < 4) return;
                int auctionId = Integer.parseInt(parts[1]);
                double newPrice = Double.parseDouble(parts[2]);
                String bidder = parts[3];
                Platform.runLater(() -> {
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.updateAuctionPrice(auctionId, newPrice, bidder);
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
            case "AUCTION_LIST": {
                // "AUCTION_LIST|count" followed by individual "AUCTION_ITEM|id|name|price|endTime|status" lines
                // handled by accumulation — nothing to do here; items arrive as AUCTION_ITEM
                break;
            }
            case "AUCTION_ITEM": {
                // "AUCTION_ITEM|id|name|price|endTime|status"
                if (parts.length < 6) return;
                synchronized (pendingAuctions) {
                    pendingAuctions.add(parts);
                }
                break;
            }
            case "AUCTION_LIST_END": {
                String status = parts.length > 1 ? parts[1] : "";  // ← đọc từ message
                List<String[]> snapshot;
                synchronized (pendingAuctions) {
                    snapshot = new ArrayList<>(pendingAuctions);
                    pendingAuctions.clear();
                }
                Consumer<List<String[]>> cb;
                if ("OPEN".equals(status)) {
                    cb = openAuctionCallback;
                    openAuctionCallback = null;
                } else {
                    cb = runningAuctionCallback;
                    runningAuctionCallback = null;
                }
                if (cb != null) Platform.runLater(() -> cb.accept(snapshot));
                break;
            }
        }
    }

    // ──────────────── Gửi lệnh ────────────────

    public void login(String username, String password) {
        sendCommand("LOGIN|" + username + "|" + password);
    }

    public void register(String username, String password, String role) {
        sendCommand("REGISTER|" + username + "|" + password + "|" + role);
    }

    public void placeBid(int auctionId, double amount) {
        sendCommand("BID|" + auctionId + "|" + amount);
    }

    public void registerAutoBid(int auctionId, double maxBid, double increment) {
        sendCommand("AUTOBID|" + auctionId + "|" + maxBid + "|" + increment);
    }

    public void requestAuctions(String status, Consumer<List<String[]>> callback) {
        if ("OPEN".equals(status))    this.openAuctionCallback    = callback;
        else                          this.runningAuctionCallback  = callback;
        sendCommand("LIST_AUCTIONS|" + status);
    }

    public void addItem(String type, String name, String description,
                        double startPrice, String startTime, String endTime,
                        BiConsumer<Boolean, String> callback) {
        this.addItemCallback = callback;
        sendCommand("ADD_ITEM|" + type + "|" + name + "|" + description
                + "|" + startPrice + "|" + startTime + "|" + endTime);
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

    /** Lấy danh sách item của seller đang đăng nhập */
    public void requestMyItems(Consumer<List<String[]>> callback) {
        this.myItemsCallback = callback;
        sendCommand("MY_ITEMS");
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

    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentRole()     { return currentRole; }
    /** Gọi khi mở màn hình bid để nhận UPDATE real-time */
    public void setActiveBidController(org.example.uicontroller.ItemBidingUIController ctrl) {
        this.activeBidController = ctrl;
    }

    /** Gọi khi đóng màn hình bid (back về main) */
    public void clearActiveBidController() {
        this.activeBidController = null;
    }
}
