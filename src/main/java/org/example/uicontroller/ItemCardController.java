package org.example.uicontroller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.domain.item.Item;
import org.example.util.LanguageManager;
import org.example.util.ThemeManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ItemCardController {
    @FXML private Label     itemname;
    @FXML private Label     price;
    @FXML private Label     timeopen;
    @FXML private ImageView itemImage;
    @FXML private Button    detailsbutton;
    @FXML private Label     openTimeLabel;

    private Item currentItem;
    private CardMode mode = CardMode.DETAIL;
    public enum CardMode { DETAIL, BID } /** DETAIL = upcoming (chỉ xem), BID = ongoing (mở màn bid) */
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private int     auctionId;
    private String  auctionName;
    private double  currentPrice;
    private String  startTime;
    private String  endTime;
    private String  description;
    private Timeline cardCountdown;
    private String imageUrl;

    // Dùng khi có Item thực từ server
    public void setAuctionData(int id, String name, double price,
                               String startTime, String endTime,
                               String description, CardMode mode, String imageUrl) {
        this.auctionId   = id;
        this.auctionName = name;
        this.currentPrice = price;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.description = description;
        this.mode        = mode;
        this.imageUrl = imageUrl;

        itemname.setText(name);
        this.price.setText(formatVND(price));

        // Hiển thị thời gian phù hợp theo mode
        if (mode == CardMode.BID) {
            // Ongoing: hiển thị thời gian kết thúc
            timeopen.setText(LanguageManager.get("itemcard.ends") + " " + formatTime(endTime));
            detailsbutton.setText(LanguageManager.get("itemcard.btn.place_bid"));
            detailsbutton.getStyleClass().add("button-bid");
            startCardCountdown(endTime);
        } else {
            // Upcoming: hiển thị thời gian mở
            timeopen.setText(LanguageManager.get("itemcard.opens") + " " + formatTime(startTime));
            detailsbutton.setText(LanguageManager.get("itemcard.btn.view_detail"));
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                itemImage.setImage(new javafx.scene.image.Image(imageUrl, true)); // true = background loading
            } catch (Exception e) {
                System.err.println("Không load được ảnh: " + imageUrl);
            }
        }
    }
    /** Cập nhật giá live từ MainScreenController (nhận UPDATE từ server) */
    public void liveUpdatePrice(double newPrice, String bidder) {
        this.currentPrice = newPrice;
        price.setText(formatVND(newPrice));
        // Flash effect nhẹ
        price.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        // Reset về style gốc sau 2s
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> price.setStyle(""));
        pause.play();
    }
    public void liveUpdateEndTime(String newEndTimeStr) {
        this.endTime = newEndTimeStr;
        if (cardCountdown != null) cardCountdown.stop();
        startCardCountdown(newEndTimeStr);
    }
    private void startCardCountdown(String endTimeStr) {
        try {
            LocalDateTime end = LocalDateTime.parse(endTimeStr);
            cardCountdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                long sec = ChronoUnit.SECONDS.between(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")), end);
                if (sec <= 0) {
                    timeopen.setText(LanguageManager.get("itemcard.time_up"));
                    cardCountdown.stop();
                    detailsbutton.setDisable(true);  // ← không cho bid khi hết giờ
                } else {
                    detailsbutton.setDisable(false); // ← bật lại nếu anti-snipe extend
                    long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
                    timeopen.setText(String.format(LanguageManager.get("itemcard.time_left"), h, m, s));
                }
            }));
            cardCountdown.setCycleCount(Animation.INDEFINITE);
            cardCountdown.play();
        } catch (Exception ignored) {}
    } int getAuctionId() { return auctionId; }

    @FXML
    private void handleDetail() throws IOException {
        if (mode == CardMode.BID) {
            openBidScreen();
        } else {
            openDetailScreen();
        }
    }
    /** Ongoing → mở màn hình đấu giá */
    private void openBidScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml"),LanguageManager.getBundle());
        Parent root = loader.load();
        ItemBidingUIController ctrl = loader.getController();
        ctrl.setAuctionData(auctionId, auctionName, currentPrice, startTime, endTime, description, imageUrl);

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    /** Upcoming → mở màn detail chỉ đọc */
    private void openDetailScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml"),LanguageManager.getBundle());
        Parent root = loader.load();
        ItemBidingUIController ctrl = loader.getController();
        ctrl.setAuctionData(auctionId, auctionName, currentPrice, startTime, endTime, description, imageUrl);
        ctrl.setReadOnly(true);   // ẩn form bid, chỉ hiện thông tin

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }


    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private String formatTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) return "—";
        try {
            return LocalDateTime.parse(isoDateTime).format(DISPLAY_FMT);
        } catch (Exception e) {
            return isoDateTime;
        }
    }
    public void refreshLanguage() {
        if (openTimeLabel != null) {
            openTimeLabel.setText(LanguageManager.get("itemcard.open_time"));
        }
        if (detailsbutton == null || timeopen == null) return;

        if (mode == CardMode.BID) {
            detailsbutton.setText(LanguageManager.get("itemcard.btn.place_bid"));

            if (cardCountdown == null) {
                timeopen.setText(LanguageManager.get("itemcard.ends") + " " + formatTime(endTime));
            }
        } else {
            detailsbutton.setText(LanguageManager.get("itemcard.btn.view_detail"));
            timeopen.setText(LanguageManager.get("itemcard.opens") + " " + formatTime(startTime));
        }
    }
}