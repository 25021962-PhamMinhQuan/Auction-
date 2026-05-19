package org.example.server;

import javafx.application.Platform;
import org.example.uicontroller.MainScreenController;

import java.io.*;
import java.net.*;
import java.util.function.BiConsumer;

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
        String   type  = parts[0];

        switch (type) {
            case "OK": {
                // "OK|ROLE|username"
                if (parts.length < 3) return;
                currentRole     = parts[1];
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
                int    auctionId = Integer.parseInt(parts[1]);
                double newPrice  = Double.parseDouble(parts[2]);
                String bidder    = parts[3];
                Platform.runLater(() -> {
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

    public void checkStatus(int auctionId) {
        sendCommand("STATUS|" + auctionId);
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
}