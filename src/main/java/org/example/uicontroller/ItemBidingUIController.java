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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.server.AuctionClient;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.server.AuctionClient;
import org.example.util.ThemeManager;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class ItemBidingUIController {

    @FXML
    private Label itemName;
    @FXML
    private Label currentPrice;
    @FXML
    private Label highestBidder;
    @FXML
    private Label timeLeft;
    @FXML
    private TextField bidInput;
    @FXML
    private TextField maxBidInput;
    @FXML
    private TextField incrementInput;
    @FXML
    private VBox historyBox;
    @FXML
    private VBox historyPopup;
    @FXML
    private Region historyBackdrop;
    @FXML
    private Label description;
    @FXML
    private VBox bidSection;
    @FXML
    private ImageView itemImage;
    @FXML private LineChart<String, Number> priceLineChart;
    @FXML private CategoryAxis lineXAxis;
    @FXML private NumberAxis lineYAxis;
    @FXML private BarChart<String, Number> bidBarChart;
    private Timeline countdownTimer;
    private LocalDateTime endTime;
    private LocalDateTime startTime;


    private int auctionId;
    private double latestPrice;
    private boolean readOnly = false;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    public void setAuctionData(int id, String name, double price, String startTimeStr, String endTimeStr, String desc, String imageUrl) {
        this.auctionId = id;
        this.latestPrice = price;

        itemName.setText(name);
        currentPrice.setText(formatVND(price));
        if (description != null) description.setText(desc != null ? desc : "");

        if (startTimeStr != null && !startTimeStr.isBlank()) {
            try {
                this.startTime = LocalDateTime.parse(startTimeStr);
            } catch (Exception ignored) {
            }
        }
        if (endTimeStr != null && !endTimeStr.isBlank()) {
            try {
                this.endTime = LocalDateTime.parse(endTimeStr);
            } catch (Exception ignored) {
            }
        }
        styleCharts();
        AuctionClient.getInstance().setActiveBidController(this);
        loadImage(imageUrl);
        startCountdown();
        loadBidHistory();
    }
    public void setData(int id, String name, double price, String imageUrl) {
        setAuctionData(id, name, price,null, null, "", imageUrl);
    }

    /**
     * Chế độ read-only (upcoming — chỉ xem, không bid).
     * Ẩn toàn bộ form bid và auto-bid.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (bidSection != null) {
            bidSection.setVisible(!readOnly);
            bidSection.setManaged(!readOnly);
        }
        if (readOnly && startTime != null) {
            // đếm ngược đến lúc bắt đầu thay vì kết thúc
            this.endTime = startTime; // countdown đến startTime
            startCountdown();
        }
    }
    public void updatePrice(double price, String bidder) {
        this.latestPrice = price;
        currentPrice.setText(formatVND(price));

        if (highestBidder != null) highestBidder.setText("Highest: " + bidder);

        // Reload lịch sử từ DB để tránh duplicate với realtime entry
        loadBidHistory();

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
        String raw = bidInput.getText();
        if (raw.isEmpty()) {
            showAlert("Empty field", "Please enter a bid amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(raw);
            if (amount <= 0) throw new NumberFormatException();
            AuctionClient.getInstance().placeBid(auctionId, amount);
            bidInput.clear();
        } catch (NumberFormatException e) {
            showAlert("Invalid bid", "Bid must be a positive number.");
        }
    }

    @FXML
    private void handleAutoBid() {
        String rawMax = maxBidInput.getText().trim();
        String rawInc = incrementInput.getText().trim();

        if (rawMax.isEmpty() || rawInc.isEmpty()) {
            showAlert("Empty fields", "Please fill in both Max Price and Increment.");
            return;
        }

        try {
            double max       = Double.parseDouble(rawMax);
            double increment = Double.parseDouble(rawInc);

            if (max <= 0 || increment <= 0) {
                showAlert("Invalid values", "Max price and increment must be positive.");
                return;
            }

            AuctionClient.getInstance().registerAutoBid(auctionId, max, increment);
            maxBidInput.clear();
            incrementInput.clear();

        } catch (NumberFormatException e) {
            showAlert("Invalid input", "Max price and increment must be valid numbers.");
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
        if (historyBackdrop != null) {
            historyBackdrop.setVisible(visible);
            historyBackdrop.setManaged(visible);
        }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleBack(ActionEvent e) throws IOException {
        stopCountdown();
        AuctionClient.getInstance().clearActiveBidController();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)e.getSource())
                .getScene()
                .getWindow();
        MainScreenController ctrl = loader.getController();
        String username = AuctionClient.getInstance().getCurrentUsername();
        String role     = AuctionClient.getInstance().getCurrentRole();
        if (username != null && role != null) {
            ctrl.setCurrentUser(username, role);
        }
        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
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
        if (endTime == null) return;

        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")), endTime);

        if (secondsLeft <= 0) {
            // ← SỬA: phân biệt readOnly hay không
            timeLeft.setText(readOnly ? "Đang diễn ra" : "Hết giờ");
            timeLeft.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            stopCountdown();
            return;
        }

        long h = secondsLeft / 3600;
        long m = (secondsLeft % 3600) / 60;
        long s = secondsLeft % 60;

        // ← SỬA: label khác nhau cho 2 chế độ
        String label = readOnly ? "Bắt đầu sau: %02d:%02d:%02d" : "Thời gian còn lại: %02d:%02d:%02d";
        timeLeft.setText(String.format(label, h, m, s));

        if (!readOnly && secondsLeft < 30) {
            boolean flash = (secondsLeft % 2 == 0);
            timeLeft.setStyle(flash
                    ? "-fx-text-fill: red; -fx-font-weight: bold;"
                    : "-fx-text-fill: inherit;");
        } else {
            timeLeft.setStyle("");
        }
    }
    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
    private void pulseLabel(Label label) {
        label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> label.setStyle(""));
        pause.play();
    }
    public void showAutoBidSuccess() {
        showStatusBrief("✓ AutoBid đã được đăng ký!");
        maxBidInput.clear();
        incrementInput.clear();
    }

    private void loadBidHistory() {
        AuctionClient.getInstance().requestBidHistory(auctionId, rows -> {
            historyBox.getChildren().clear();
            for (String[] row : rows) {
                String time = row[2] != null && row[2].length() >= 19
                        ? row[2].substring(11, 19)
                        : (row[2] != null ? row[2] : "");
                Label entry = new Label(time + "  " + row[0] + "  →  " + formatVND(Double.parseDouble(row[1])));
                entry.getStyleClass().add("history-entry");
                historyBox.getChildren().add(entry);
            }
            // Cập nhật chart theo dữ liệu mới nhất
            updateCharts(rows);
        });
    }

    private void loadImage(String imageUrl) {
        if (itemImage == null) return;
        if (imageUrl == null || imageUrl.isBlank()) {
            itemImage.setImage(null);
            return;
        }
        try {
            Image image = new Image(imageUrl, true); // true = load background
            itemImage.setImage(image);
        } catch (Exception e) {
            itemImage.setImage(null);
        }
    }
    private void styleCharts() {
        if (priceLineChart != null) {
            priceLineChart.getStyleClass().add("chart-section");
            if (lineYAxis != null) lineYAxis.setForceZeroInRange(false);
        }
        if (bidBarChart != null) {
            bidBarChart.getStyleClass().add("chart-section");
        }
    }

    private void updateCharts(List<String[]> rows) {
        // --- Line Chart: giá theo thời gian ---
        if (priceLineChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (int i = rows.size() - 1; i >= 0; i--) {  // rows DESC → duyệt ngược
                String[] row = rows.get(i);
                try {
                    String t = row[2] != null && row[2].length() >= 16
                            ? row[2].substring(11, 16) : (row[2] != null ? row[2] : "");
                    series.getData().add(new XYChart.Data<>(t, Double.parseDouble(row[1])));
                } catch (Exception ignored) {}
            }
            priceLineChart.getData().clear();
            priceLineChart.getData().add(series);
        }

        // --- Bar Chart: số bid theo giờ ---
        if (bidBarChart != null) {
            Map<String, Integer> hourCount = new TreeMap<>();
            for (int h = 0; h < 24; h++) hourCount.put(String.format("%02dh", h), 0);
            for (String[] row : rows) {
                try {
                    if (row[2] != null && row[2].length() >= 13) {
                        String key = String.format("%02dh", Integer.parseInt(row[2].substring(11, 13)));
                        hourCount.put(key, hourCount.getOrDefault(key, 0) + 1);
                    }
                } catch (Exception ignored) {}
            }
            // Tìm khoảng giờ có bid để trim trục X
            int first = -1, last = -1;
            for (Map.Entry<String, Integer> e : hourCount.entrySet()) {
                if (e.getValue() > 0) {
                    int h = Integer.parseInt(e.getKey().replace("h", ""));
                    if (first == -1) first = h;
                    last = h;
                }
            }
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            int from = first == -1 ? 0 : Math.max(0, first - 1);
            int to   = last  == -1 ? 23 : Math.min(23, last + 1);
            for (int h = from; h <= to; h++) {
                String key = String.format("%02dh", h);
                series.getData().add(new XYChart.Data<>(key, hourCount.getOrDefault(key, 0)));
            }
            bidBarChart.getData().clear();
            bidBarChart.getData().add(series);
        }
    }
    public void showError(String message) {
        // Hiện lỗi ngắn gọn trên label timeLeft rồi tự mất sau
        String prev = timeLeft.getText();
        String prevStyle = timeLeft.getStyle();
        timeLeft.setText(message);
        timeLeft.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            timeLeft.setText(prev);
            timeLeft.setStyle(prevStyle);
        });
        pause.play();
    }
}