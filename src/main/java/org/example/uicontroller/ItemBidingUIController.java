package org.example.uicontroller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
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


    private int auctionId;

    

    public void setData(int id, String name, double price) {
        this.auctionId = id;
        itemName.setText(name);
        currentPrice.setText("VND"+price);
        startCountdown();
    }

    
    
    public void updatePrice(double price, String bidder) {
        currentPrice.setText("VND"+price);
        Label entry = new Label(bidder + " — " + "VND"+price);
        historyBox.getChildren().add(0, entry);   
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
            AuctionClient.getInstance().placeBid(auctionId, (int) amount);
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
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)e.getSource())
                .getScene()
                .getWindow();

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
        if (endTime == null) return;

        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

        if (secondsLeft <= 0) {
            timeLeft.setText("Hết giờ");
            timeLeft.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
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
                    ? "-fx-text-fill: red; -fx-font-weight: bold;"
                    : "-fx-text-fill: inherit;");
        } else {
            timeLeft.setStyle("");
        }
    }
    public void updateEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
    }
}
