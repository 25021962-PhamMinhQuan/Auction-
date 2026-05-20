package org.example.uicontroller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.example.domain.user.User;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class MainScreenController {

    @FXML private HBox       upcomingHbox;
    @FXML private HBox       ongoingHbox;
    @FXML private FlowPane   gridPane;
    @FXML private ScrollPane gridScroll;
    @FXML private VBox       mainContent;
    @FXML private Button     backButton;
    @FXML private StackPane  categoryBox;
    @FXML private VBox       categoryMenu;
    @FXML private StackPane  auctionBox;
    @FXML private VBox       auctionMenu;
    @FXML private Label      usernameLabel;
    @FXML private Button     addItemBtn;

    private static final int    PREVIEW_COUNT  = 7;
    private static final String ITEM_CARD_FXML = "/org/example/view/itemcard.fxml";

    private static MainScreenController instance;
    private User   currentUser;
    private String currentGridType; // "OPEN" hoặc "RUNNING" — dùng khi updateCardsInContainer


    @FXML
    public void initialize() {
        // Đăng ký instance thực ngay khi JavaFX inject @FXML fields xong
        instance = this;
        wireHoverMenus();
        loadUpcoming();
        loadOngoing();
    }

    public static MainScreenController getInstance() { return instance; }
    public static void setInstance(MainScreenController ctrl) { instance = ctrl; }


    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (usernameLabel != null)
            usernameLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        showAddItemBtn("SELLER".equals(user.getRole()));
    }

    public void setCurrentUser(String username, String role) {
        if (usernameLabel != null)
            usernameLabel.setText(username + " (" + role + ")");
        showAddItemBtn("SELLER".equals(role));
    }

    private void showAddItemBtn(boolean show) {
        if (addItemBtn != null) {
            addItemBtn.setVisible(show);
            addItemBtn.setManaged(show);
        }
    }

    private void loadUpcoming() {
        // Gọi AuctionClient.requestAuctions() — khớp với ClientHandler "LIST_AUCTIONS|OPEN"
        AuctionClient.getInstance().requestAuctions("OPEN", items ->
                Platform.runLater(() -> {
                    upcomingHbox.getChildren().clear();
                    int count = Math.min(items.size(), PREVIEW_COUNT);
                    for (int i = 0; i < count; i++) {
                        upcomingHbox.getChildren().add(
                                buildCard(items.get(i), ItemCardController.CardMode.DETAIL));
                    }
                })
        );
    }

    private void loadOngoing() {
        // Gọi AuctionClient.requestAuctions() — khớp với ClientHandler "LIST_AUCTIONS|RUNNING"
        AuctionClient.getInstance().requestAuctions("RUNNING", items ->
                Platform.runLater(() -> {
                    ongoingHbox.getChildren().clear();
                    int count = Math.min(items.size(), PREVIEW_COUNT);
                    for (int i = 0; i < count; i++) {
                        ongoingHbox.getChildren().add(
                                buildCard(items.get(i), ItemCardController.CardMode.BID));
                    }
                })
        );
    }

    private Node buildCard(String[] parts, ItemCardController.CardMode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ITEM_CARD_FXML));
            Node node = loader.load();
            ItemCardController ctrl = loader.getController();

            int    id          = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            String name        = parts.length > 2 ? parts[2] : "—";
            double price       = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
            String endTime     = parts.length > 4 ? parts[4] : "";
            // parts[5] = status (bỏ qua, dùng mode thay thế)
            String startTime   = parts.length > 6 ? parts[6] : "";
            String description = parts.length > 7 ? parts[7] : "";

            // setAuctionData() khớp với signature trong ItemCardController
            ctrl.setAuctionData(id, name, price, startTime, endTime, description, mode);

            // Gắn controller vào node properties để updateCardsInContainer() tìm lại được
            node.getProperties().put("controller", ctrl);
            return node;

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load item card", e);
        }
    }

    @FXML
    private void handleViewAllUpcoming() {
        currentGridType = "OPEN";
        gridPane.getChildren().clear();
        setGridVisible(true);
        AuctionClient.getInstance().requestAuctions("OPEN", items ->
                Platform.runLater(() -> {
                    gridPane.getChildren().clear();
                    for (String[] item : items) {
                        gridPane.getChildren().add(
                                buildCard(item, ItemCardController.CardMode.DETAIL));
                    }
                })
        );
    }

    @FXML
    private void handleViewAllOngoing() {
        currentGridType = "RUNNING";
        gridPane.getChildren().clear();
        setGridVisible(true);
        AuctionClient.getInstance().requestAuctions("RUNNING", items ->
                Platform.runLater(() -> {
                    gridPane.getChildren().clear();
                    for (String[] item : items) {
                        gridPane.getChildren().add(
                                buildCard(item, ItemCardController.CardMode.BID));
                    }
                })
        );
    }

    @FXML private void handleBack() { setGridVisible(false); }

    @FXML
    private void handleAddItem(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/additem.fxml"));
        javafx.scene.Parent root = loader.load();
        javafx.stage.Stage stage =
                (javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
    }

    public void updateAuctionPrice(int auctionId, double newPrice, String bidder) {
        updateCardsInContainer(ongoingHbox, auctionId, newPrice, bidder);
        if ("RUNNING".equals(currentGridType)) {
            updateCardsInContainer(gridPane, auctionId, newPrice, bidder);
        }
    }

    private void updateCardsInContainer(Pane container, int auctionId,
                                        double newPrice, String bidder) {
        for (Node node : container.getChildren()) {
            Object ctrl = node.getProperties().get("controller");
            if (ctrl instanceof ItemCardController) {
                ItemCardController card = (ItemCardController) ctrl;
                // getAuctionId() khớp với getter trong ItemCardController
                if (card.getAuctionId() == auctionId) {
                    card.liveUpdatePrice(newPrice, bidder);
                }
            }
        }
    }
    public void onAuctionFinished(int auctionId, String winner, double finalPrice) {
        System.out.printf("Auction #%d finished — Winner: %s, Final: %,.0f VND%n",
                auctionId, winner, finalPrice);
        loadOngoing();
        loadUpcoming();
    }
    public void onNewAuction() {
        Platform.runLater(() -> {
            loadUpcoming();
            loadOngoing();
        });
    }

    private void setGridVisible(boolean show) {
        mainContent.setVisible(!show); mainContent.setManaged(!show);
        gridScroll.setVisible(show);   gridScroll.setManaged(show);
        backButton.setVisible(show);   backButton.setManaged(show);
    }

    private void wireHoverMenus() {
        categoryBox.setOnMouseEntered(e  -> setMenuVisible(categoryMenu, true));
        categoryBox.setOnMouseExited(e   -> setMenuVisible(categoryMenu, false));
        categoryMenu.setOnMouseEntered(e -> setMenuVisible(categoryMenu, true));
        categoryMenu.setOnMouseExited(e  -> setMenuVisible(categoryMenu, false));

        auctionBox.setOnMouseEntered(e   -> setMenuVisible(auctionMenu, true));
        auctionBox.setOnMouseExited(e    -> setMenuVisible(auctionMenu, false));
        auctionMenu.setOnMouseEntered(e  -> setMenuVisible(auctionMenu, true));
        auctionMenu.setOnMouseExited(e   -> setMenuVisible(auctionMenu, false));
    }

    private void setMenuVisible(VBox menu, boolean visible) {
        menu.setVisible(visible);
        menu.setManaged(visible);
    }
}