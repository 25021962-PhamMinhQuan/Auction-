package org.example.uicontroller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.server.AuctionClient;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class ItemBidingUIController {

    @FXML private Label     itemName;
    @FXML private Label     currentPrice;
    @FXML private Label     highestBidder;
    @FXML private Label     timeLeft;
    @FXML private TextField bidInput;
    @FXML private TextField maxBidInput;
    @FXML private TextField incrementInput;
    @FXML private VBox      historyBox;
    @FXML private VBox      historyPopup;
    private Timeline      countdownTimer;
    private LocalDateTime endTime;
    @FXML private Label description;
    private VBox bidSection;
    private int           auctionId;
    private double        latestPrice;
    private boolean       readOnly = false;
    /**
     * Khởi tạo màn hình bid với dữ liệu từ server.
     *
     * @param endTimeStr chuỗi ISO LocalDateTime kết thúc phiên đấu giá
     */
    public void setAuctionData(int id, String name, double price,
                               String endTimeStr, String desc) {
        this.auctionId   = id;
        this.latestPrice = price;

        itemName.setText(name);
        currentPrice.setText(formatVND(price));
        if (description != null) description.setText(desc != null ? desc : "");

        // Parse endTime
        if (endTimeStr != null && !endTimeStr.isBlank()) {
            try {
                this.endTime = LocalDateTime.parse(endTimeStr);
            } catch (Exception ignored) { }
        }

        // Đăng ký nhận UPDATE từ server
        AuctionClient.getInstance().setActiveBidController(this);

        startCountdown();
    }
    public void setData(int id, String name, double price) {
        setAuctionData(id, name, price, null, "");
    }
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (bidSection != null) {
            bidSection.setVisible(!readOnly);
            bidSection.setManaged(!readOnly);
        }
        // Thay đổi countdown label nếu chưa bắt đầu
        if (readOnly && endTime == null && timeLeft != null) {
            timeLeft.setText("Chưa bắt đầu");
        }
    }

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /** Cập nhật giá và lịch sử khi có bid mới từ server */
    public void updatePrice(double price, String bidder) {
        this.latestPrice = price;
        currentPrice.setText(formatVND(price));

        if (highestBidder != null) highestBidder.setText("Highest: " + bidder);

        // Thêm entry vào history
        Label entry = new Label(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + "  " + bidder
                        + "  →  " + formatVND(price));
        entry.getStyleClass().add("history-entry");
        historyBox.getChildren().add(0, entry);

        // Pulse animation trên giá
        pulseLabel(currentPrice);
    }
    /** Extend endTime khi có anti-snipe từ server */
    public void updateEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
        // countdown tự refresh ở tick tiếp theo
    }


    @FXML
    private void handleBid() {
        if (readOnly) return;
        String raw = bidInput.getText().trim();
        if (raw.isEmpty()) {
            showAlert(AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập số tiền đặt bid.");
            return;
        }
        try {
            double amount = Double.parseDouble(raw);
            if (amount <= 0) throw new NumberFormatException();

            // Kiểm tra client-side: phải cao hơn giá hiện tại
            if (amount <= latestPrice) {
                showAlert(AlertType.WARNING, "Bid quá thấp",
                        String.format("Bid phải cao hơn giá hiện tại: %s", formatVND(latestPrice)));
                return;
            }
            double minBid = latestPrice * 1.05;
            if (amount < minBid) {
                showAlert(AlertType.WARNING, "Bid quá thấp",
                        String.format("Bid tối thiểu: %s (5%% trên giá hiện tại)", formatVND(minBid)));
                return;
            }

            AuctionClient.getInstance().placeBid(auctionId, amount);
            bidInput.clear();
            showStatusBrief("Đã gửi bid " + formatVND(amount));

        } catch (NumberFormatException e) {
            showAlert(AlertType.WARNING, "Số không hợp lệ", "Vui lòng nhập số dương hợp lệ.");
        }
    }
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAutoBid() {
        if (readOnly) return;
        String rawMax = maxBidInput.getText().trim();
        String rawInc = incrementInput.getText().trim();

        if (rawMax.isEmpty() || rawInc.isEmpty()) {
            showAlert(AlertType.WARNING, "Thiếu thông tin",
                    "Vui lòng điền đầy đủ Max Price và Increment.");
            return;
        }
        try {
            double max       = Double.parseDouble(rawMax);
            double increment = Double.parseDouble(rawInc);

            if (max <= 0 || increment <= 0) {
                showAlert(AlertType.WARNING, "Giá trị không hợp lệ",
                        "Max price và increment phải là số dương.");
                return;
            }
            if (max <= latestPrice) {
                showAlert(AlertType.WARNING, "Max price quá thấp",
                        "Max price phải cao hơn giá hiện tại: " + formatVND(latestPrice));
                return;
            }

            AuctionClient.getInstance().registerAutoBid(auctionId, max, increment);
            maxBidInput.clear();
            incrementInput.clear();
            showStatusBrief("Auto-bid đã đăng ký (max: " + formatVND(max) + ")");

        } catch (NumberFormatException e) {
            showAlert(AlertType.WARNING, "Số không hợp lệ",
                    "Max price và increment phải là số hợp lệ.");
        }
    }

    @FXML
    private void handleShowHistory() {
        setHistoryVisible(true);
    }

    @FXML
    private void handleCloseHistory() {
        setHistoryVisible(false);
    }


    private void setHistoryVisible(boolean visible) {
        historyPopup.setVisible(visible);
        historyPopup.setManaged(visible);
    }




    @FXML
    private void handleBack(ActionEvent e) throws IOException {
        stopCountdown();
        AuctionClient.getInstance().clearActiveBidController();
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void startCountdown() {
        stopCountdown();
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> refreshCountdown()));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
        refreshCountdown();
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void refreshCountdown() {
        if (endTime == null) {
            timeLeft.setText(readOnly ? "Chưa bắt đầu" : "—");
            return;
        }

        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

        if (secondsLeft <= 0) {
            timeLeft.setText("Hết giờ");
            timeLeft.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            stopCountdown();
            return;
        }

        long h = secondsLeft / 3600;
        long m = (secondsLeft % 3600) / 60;
        long s = secondsLeft % 60;
        timeLeft.setText(String.format("%02d:%02d:%02d còn lại", h, m, s));

        if (secondsLeft < 30) {
            boolean flash = (secondsLeft % 2 == 0);
            timeLeft.setStyle(flash
                    ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                    : "-fx-text-fill: inherit;");
        } else if (secondsLeft < 300) {
            timeLeft.setStyle("-fx-text-fill: #e67e22;");  // cam khi < 5 phút
        } else {
            timeLeft.setStyle("");
        }
    }
    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount);
    }


    /** Hiển thị thông báo ngắn trên label timeLeft rồi khôi phục sau 3s */
    private void showStatusBrief(String message) {
        String prev = timeLeft.getText();
        timeLeft.setText(message);
        timeLeft.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            timeLeft.setStyle("");
            timeLeft.setText(prev);
        });
        pause.play();
    }

    /** Hiệu ứng nhấp nháy nhẹ khi giá được cập nhật */
    private void pulseLabel(Label label) {
        label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> label.setStyle(""));
        pause.play();
    }
}

