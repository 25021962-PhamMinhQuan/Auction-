package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.server.AuctionClient;


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


    private int auctionId;

    

    public void setData(int id, String name, double price) {
        this.auctionId = id;
        itemName.setText(name);
        currentPrice.setText("VND"+price);
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
}
