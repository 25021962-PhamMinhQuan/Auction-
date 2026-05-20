package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ItemCardController {

    // DETAIL = chỉ xem thông tin, BID = mở màn hình đặt giá
    public enum CardMode { DETAIL, BID }

    @FXML private Label     itemname;
    @FXML private Label     price;
    @FXML private Label     timeopen;
    @FXML private ImageView itemImage;
    @FXML private Button    detailsbutton;

    private int      auctionId;
    private String   name;
    private double   currentPrice;
    private String   startTime;
    private String   endTime;
    private String   description;
    private CardMode mode;

    private static final DateTimeFormatter ISO     = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    public void setAuctionData(int auctionId, String name, double price,
                               String startTime, String endTime,
                               String description, CardMode mode) {
        this.auctionId    = auctionId;
        this.name         = name;
        this.currentPrice = price;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.description  = description;
        this.mode         = mode;

        itemname.setText(name);
        this.price.setText(String.format("%,.0f VND", price));

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, ISO);
            timeopen.setText(start.format(DISPLAY));
        } catch (Exception ex) {
            timeopen.setText(startTime != null ? startTime : "");
        }

        detailsbutton.setText(mode == CardMode.DETAIL ? "Xem chi tiết" : "Đặt giá");
    }
    public void liveUpdatePrice(double newPrice, String bidder) {
        this.currentPrice = newPrice;
        price.setText(String.format("%,.0f VND", newPrice));
    }

    public int getAuctionId() {
        return auctionId;
    }

    @FXML
    private void handleDetail() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml"));
        Parent root = loader.load();

        ItemBidingUIController controller = loader.getController();
        controller.setData(auctionId, name, currentPrice);

        // Pass endTime để countdown chạy đúng
        if (endTime != null && !endTime.isEmpty()) {
            try {
                controller.updateEndTime(LocalDateTime.parse(endTime, ISO));
            } catch (Exception ignored) {}
        }

        // Đăng ký để nhận UPDATE real-time khi đang ở màn hình bid
        AuctionClient.getInstance().setActiveBidController(controller);

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}