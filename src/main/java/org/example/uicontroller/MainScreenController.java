package org.example.uicontroller;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


import java.io.IOException;

public class MainScreenController {
    @FXML private HBox upcomingHbox;
    @FXML private HBox ongoingHbox;
    @FXML private FlowPane gridPane;
    @FXML private ScrollPane gridScroll;
    @FXML private VBox mainContent;
    @FXML private Button backButton;
    @FXML private VBox categoryMenu;
    @FXML private StackPane categoryBox;
    @FXML private StackPane auctionBox;
    @FXML private VBox auctionMenu;
    @FXML
    public void initialize() throws IOException {
        for (int i = 0; i <= 6; i++){
            upcomingHbox.getChildren().add(loadItem("Upcoming " + i, String.valueOf(i * 100), "10AM"));
        }
        for (int i = 0; i <= 6; i++){
            ongoingHbox.getChildren().add(loadItem("Ongoing " + i, String.valueOf(i * 100), "10AM"));
        // hover menu item
            categoryBox.setOnMouseEntered(e -> {
                categoryMenu.setVisible(true);
                categoryMenu.setManaged(true);
            });

            categoryBox.setOnMouseExited(e -> {
                categoryMenu.setVisible(false);
                categoryMenu.setManaged(false);
            });
            auctionBox.setOnMouseEntered(e -> {
                auctionMenu.setVisible(true);
                auctionMenu.setManaged(true);
            });

            auctionMenu.setOnMouseExited(e -> {
                auctionMenu.setVisible(false);
                auctionMenu.setManaged(false);
            });
        }
    }
    private Node loadItem(String name, String price, String time) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/view/itemcard.fxml"));
        Node item = loader.load();
        ItemCardController controller = loader.getController();
        controller.setData(name, price, time);

        return item;
    }
    @FXML
    private void handleViewAllUpcoming() throws IOException {
        gridPane.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            gridPane.getChildren().add(loadItem("Upcoming " + i, String.valueOf(i * 100), "10AM"));
        }
        mainContent.setVisible(false);
        mainContent.setManaged(false);
        gridScroll.setVisible(true);
        gridScroll.setManaged(true);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }
    @FXML
    private void handleViewAllOngoing() throws IOException {
        gridPane.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            gridPane.getChildren().add(loadItem("Ongoing " + i, String.valueOf(i * 100), "10AM"));
        }
        mainContent.setVisible(false);
        mainContent.setManaged(false);
        gridScroll.setVisible(true);
        gridScroll.setManaged(true);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }
    @FXML
    private void handleBack() {
        gridScroll.setVisible(false);
        gridScroll.setManaged(false);

        mainContent.setVisible(true);
        mainContent.setManaged(true);

        backButton.setVisible(false);
        backButton.setManaged(false);
    }
}