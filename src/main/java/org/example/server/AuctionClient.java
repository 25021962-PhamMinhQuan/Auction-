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
    private Consumer<List<String[]>>    auctionListCallback;
    private Consumer<List<String[]>>    myItemsCallback;

    private final List<String[]> pendingAuctions = new ArrayList<>();
    private final List<String[]> pendingMyItems  = new ArrayList<>();

    private org.example.uicontroller.ItemBidingUIController activeBidController;
    private Runnable newAuctionListener;

    // ── Singleton ──────────────────────────────────────────────────────────────

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


    public void connect(javafx.stage.Stage stage) throws IOException {
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

    private void handleServerMessage(String line) {
        System.out.println("[← server] " + line);
        String[] parts = line.split("\\|");
        String   type  = parts[0];

        switch (type) {

            case "OK": {
                // "OK|ROLE|username"
                if (parts.length < 3) return;
                currentRole     = parts[1];
                currentUsername = parts[2];
                // FIX 4: wrap Platform.runLater — callback thường cập nhật UI
                BiConsumer<Boolean, String> cb = loginCallback;
                loginCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, null));
                break;
            }

            case "REGISTER_OK": {
                BiConsumer<Boolean, String> cb = registerCallback;
                registerCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, "Đăng ký thành công"));
                break;
            }

            case "REGISTER_ERROR": {
                String reason = parts.length > 1 ? parts[1] : "Đăng ký thất bại";
                BiConsumer<Boolean, String> cb = registerCallback;
                registerCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(false, reason));
                break;
            }

            case "ERROR": {
                String msg = parts.length > 1 ? parts[1] : "Lỗi không xác định";
                if (loginCallback != null) {
                    BiConsumer<Boolean, String> cb = loginCallback;
                    loginCallback = null;
                    Platform.runLater(() -> cb.accept(false, msg));
                } else if (addItemCallback != null) {
                    BiConsumer<Boolean, String> cb = addItemCallback;
                    addItemCallback = null;
                    Platform.runLater(() -> cb.accept(false, msg));
                } else if (startAuctionCallback != null) {
                    BiConsumer<Boolean, String> cb = startAuctionCallback;
                    startAuctionCallback = null;
                    Platform.runLater(() -> cb.accept(false, msg));
                } else if (registerCallback != null) {
                    BiConsumer<Boolean, String> cb = registerCallback;
                    registerCallback = null;
                    Platform.runLater(() -> cb.accept(false, msg));
                } else {
                    Platform.runLater(() -> System.err.println("Server error: " + msg));
                }
                break;
            }

            case "UPDATE": {
                if (parts.length < 4) return;
                int    auctionId = Integer.parseInt(parts[1]);
                double newPrice  = Double.parseDouble(parts[2]);
                String bidder    = parts[3];
                Platform.runLater(() -> {
                    if (activeBidController != null)
                        activeBidController.updatePrice(newPrice, bidder);
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.updateAuctionPrice(auctionId, newPrice, bidder);
                });
                break;
            }

            case "FINISHED": {
                if (parts.length < 4) return;
                int    auctionId  = Integer.parseInt(parts[1]);
                String winner     = parts[2];
                double finalPrice = Double.parseDouble(parts[3]);
                Platform.runLater(() -> {
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.onAuctionFinished(auctionId, winner, finalPrice);
                });
                break;
            }

            case "ITEM_ADDED": {
                String itemId   = parts.length > 1 ? parts[1] : "";
                String itemName = parts.length > 2 ? parts[2] : "";
                BiConsumer<Boolean, String> cb = addItemCallback;
                addItemCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, itemId + "|" + itemName));
                break;
            }

            case "AUCTION_STARTED": {
                String auctionId = parts.length > 1 ? parts[1] : "";
                BiConsumer<Boolean, String> cb = startAuctionCallback;
                startAuctionCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(true, auctionId));
                break;
            }

            case "NEW_AUCTION": {
                Platform.runLater(() -> {
                    MainScreenController ctrl = MainScreenController.getInstance();
                    if (ctrl != null) ctrl.onNewAuction();
                    if (newAuctionListener != null) newAuctionListener.run();
                });
                break;
            }

            case "MY_ITEM": {
                if (parts.length < 2) return;
                synchronized (pendingMyItems) { pendingMyItems.add(parts); }
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

            case "AUCTION_ITEM": {
                if (parts.length < 2) return;
                synchronized (pendingAuctions) { pendingAuctions.add(parts); }
                break;
            }

            case "AUCTION_LIST_END": {
                List<String[]> snapshot;
                synchronized (pendingAuctions) {
                    snapshot = new ArrayList<>(pendingAuctions);
                    pendingAuctions.clear();
                }
                Consumer<List<String[]>> cb = auctionListCallback;
                auctionListCallback = null;
                if (cb != null) Platform.runLater(() -> cb.accept(snapshot));
                break;
            }
        }
    }


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

    public void checkStatus(int auctionId) {
        sendCommand("STATUS|" + auctionId);
    }

    public void requestAuctions(String status, Consumer<List<String[]>> callback) {
        // FIX 3: clear pending trước khi gửi request mới
        synchronized (pendingAuctions) { pendingAuctions.clear(); }
        auctionListCallback = callback;
        sendCommand("LIST_AUCTIONS|" + status);
    }

    public void addItem(String type, String name, String description,
                        double startPrice, String startTime, String endTime,
                        BiConsumer<Boolean, String> callback) {
        addItemCallback = callback;
        sendCommand("ADD_ITEM|" + type + "|" + name + "|" + description
                + "|" + startPrice + "|" + startTime + "|" + endTime);
    }

    public void startAuction(String itemId, BiConsumer<Boolean, String> callback) {
        startAuctionCallback = callback;
        sendCommand("START_AUCTION|" + itemId);
    }

    public void requestMyItems(Consumer<List<String[]>> callback) {
        synchronized (pendingMyItems) { pendingMyItems.clear(); }
        myItemsCallback = callback;
        sendCommand("MY_ITEMS");
    }

    public void setLoginCallback(BiConsumer<Boolean, String> callback)    { this.loginCallback = callback; }
    public void setRegisterCallback(BiConsumer<Boolean, String> callback) { this.registerCallback = callback; }
    public void setNewAuctionListener(Runnable listener)                  { this.newAuctionListener = listener; }
    public void setActiveBidController(org.example.uicontroller.ItemBidingUIController ctrl) { this.activeBidController = ctrl; }
    public void clearActiveBidController()                                { this.activeBidController = null; }

    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            System.out.println("[→ server] " + command);
        }
    }

    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentRole()     { return currentRole; }
}