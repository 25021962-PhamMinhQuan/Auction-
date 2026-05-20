package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.domain.item.Item;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ItemCardController {
    @FXML private Label     itemname;
    @FXML private Label     price;
    @FXML private Label     timeopen;
    @FXML private ImageView itemImage;
    @FXML private Button    detailsbutton;

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

    // Dùng khi có Item thực từ server
    public void setAuctionData(int id, String name, double price,
                               String startTime, String endTime,
                               String description, CardMode mode) {
        this.auctionId   = id;
        this.auctionName = name;
        this.currentPrice = price;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.description = description;
        this.mode        = mode;

        itemname.setText(name);
        this.price.setText(formatVND(price));

        // Hiển thị thời gian phù hợp theo mode
        if (mode == CardMode.BID) {
            // Ongoing: hiển thị thời gian kết thúc
            timeopen.setText("Ends: " + formatTime(endTime));
            detailsbutton.setText("Place Bid");
            detailsbutton.getStyleClass().add("button-bid");
        } else {
            // Upcoming: hiển thị thời gian mở
            timeopen.setText("Opens: " + formatTime(startTime));
            detailsbutton.setText("View Detail");
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
    public int getAuctionId() { return auctionId; }

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
                getClass().getResource("/org/example/view/itemdetail.fxml"));
        Parent root = loader.load();
        ItemBidingUIController ctrl = loader.getController();
        ctrl.setAuctionData(auctionId, auctionName, currentPrice, endTime, description);

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
    }

    /** Upcoming → mở màn detail chỉ đọc (TODO: tạo riêng màn detail nếu cần) */
    private void openDetailScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml"));
        Parent root = loader.load();
        ItemBidingUIController ctrl = loader.getController();
        ctrl.setAuctionData(auctionId, auctionName, currentPrice, endTime, description);
        ctrl.setReadOnly(true);   // ẩn form bid, chỉ hiện thông tin

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
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
}