package org.example.uicontroller;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    private static MainScreenController instance;

    public static MainScreenController getInstance() {
        return instance;
    }
 // chưa xong, chờ main screen
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
    }


    public void updateAuctionPrice(int auctionId, double newPrice, String bidder) {

    }

    public void onAuctionFinished(int auctionId, String winner, double finalPrice) {

    }


    public void setCurrentUser(String username, String role) {

    }
}