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
import org.example.domain.user.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class MainScreenController {

    @FXML private HBox      upcomingHbox;
    @FXML private HBox      ongoingHbox;
    @FXML private FlowPane  gridPane;
    @FXML private ScrollPane gridScroll;
    @FXML private VBox      mainContent;
    @FXML private Button    backButton;
    @FXML private StackPane categoryBox;
    @FXML private VBox      categoryMenu;
    @FXML private StackPane auctionBox;
    @FXML private VBox      auctionMenu;


    private static final String ITEM_CARD_FXML = "/org/example/view/itemcard.fxml";
    private static final int    PREVIEW_COUNT  = 7;
    private static final int    GRID_COUNT     = 10;
    private static MainScreenController instance;
    private User currentUser;



    @FXML
    public void initialize() {
        populateRow(upcomingHbox, "Upcoming", PREVIEW_COUNT);
        populateRow(ongoingHbox,  "Ongoing",  PREVIEW_COUNT);
        wireHoverMenus();
    }

    public static synchronized MainScreenController getInstance() {
        if(instance == null){
            instance = new MainScreenController();
        }
        return instance;
    }

    @FXML
    private void handleViewAllUpcoming() {
        showGrid("Upcoming", GRID_COUNT);
    }

    @FXML
    private void handleViewAllOngoing() {
        showGrid("Ongoing", GRID_COUNT);
    }

    @FXML
    private void handleBack() {
        setGridVisible(false);
    }


    private void populateRow(HBox row, String label, int count) {
        for (int i = 0; i < count; i++) {
            try {
                row.getChildren().add(loadItemCard(label + " " + i, String.valueOf(i * 100), "10AM"));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load item card for " + label + " " + i, e);
            }
        }
    }

   
    private void showGrid(String label, int count) {
        gridPane.getChildren().clear();
        for (int i = 0; i < count; i++) {
            try {
                gridPane.getChildren().add(loadItemCard(label + " " + i, String.valueOf(i * 100), "10AM"));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load grid item card " + i, e);
            }
        }
        setGridVisible(true);
    }


    private Node loadItemCard(String name, String price, String time) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ITEM_CARD_FXML));
        Node node = loader.load();
        ItemCardController ctrl = loader.getController();
        return node;
    }

   
    private void setGridVisible(boolean show) {
        mainContent.setVisible(!show);
        mainContent.setManaged(!show);
        gridScroll.setVisible(show);
        gridScroll.setManaged(show);
        backButton.setVisible(show);
        backButton.setManaged(show);
    }

    
    private void wireHoverMenus() {
        // Category dropdown
        categoryBox.setOnMouseEntered(e -> setMenuVisible(categoryMenu, true));
        categoryBox.setOnMouseExited(e  -> setMenuVisible(categoryMenu, false));
        categoryMenu.setOnMouseEntered(e -> setMenuVisible(categoryMenu, true));
        categoryMenu.setOnMouseExited(e  -> setMenuVisible(categoryMenu, false));

        // Auction dropdown
        auctionBox.setOnMouseEntered(e  -> setMenuVisible(auctionMenu, true));
        auctionBox.setOnMouseExited(e   -> setMenuVisible(auctionMenu, false));
        auctionMenu.setOnMouseEntered(e -> setMenuVisible(auctionMenu, true));
        auctionMenu.setOnMouseExited(e  -> setMenuVisible(auctionMenu, false));
    }

    private void setMenuVisible(VBox menu, boolean visible) {
        menu.setVisible(visible);
        menu.setManaged(visible);
    }

    public void updateAuctionPrice(int auctionId, double newPrice, String bidder) {
    }
    public void setCurrentUser(User user){
        this.currentUser = user;
    }
    public void setCurrentUser(String username, String role){
    }
    public void onAuctionFinished(int auctionId,String winner,double finalprice){}

}
