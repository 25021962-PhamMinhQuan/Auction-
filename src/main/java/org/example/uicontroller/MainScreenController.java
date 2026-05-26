package org.example.uicontroller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.example.domain.user.User;
import org.example.server.AuctionClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.io.UncheckedIOException;

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
    @FXML private Label      usernameLabel; // có trong FXML navbar
    @FXML private AnchorPane overlayDashboard;
    @FXML private Button     addItemBtn;
    @FXML private TextField searchField;
    @FXML private VBox suggestionBox;




    private String currentGridType; // "OPEN" hoặc "RUNNING" – để biết grid đang xem loại nào
    private static final String ITEM_CARD_FXML = "/org/example/view/itemcard.fxml";
    private static final int PREVIEW_COUNT = 7;
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingSuggest;

    // FIX: không tự new() — instance được set từ ngoài vào bằng setInstance()
    // Nếu tự new() thì @FXML fields sẽ null hết vì JavaFX không inject vào
    private static MainScreenController instance;

    private User currentUser;

    @FXML
    public void initialize() {
        instance = this;
        wireHoverMenus();
        wireAutocomplete();
        loadUpcoming();
    }


    // FIX: chỉ trả về instance hiện tại, không tự tạo mới
    public static MainScreenController getInstance() {
        return instance;
    }

    // FIX: thêm setter để AuctionClient có thể đăng ký instance thực sau khi load FXML
    public static void setInstance(MainScreenController ctrl) {
        instance = ctrl;
    }
    /** Upcoming = status OPEN → card chỉ xem detail */
    private void loadUpcoming() {
        AuctionClient.getInstance().requestAuctions("OPEN", items ->
                Platform.runLater(() -> {
                    upcomingHbox.getChildren().clear();
                    int count = Math.min(items.size(), PREVIEW_COUNT);
                    for (int i = 0; i < count; i++) {
                        upcomingHbox.getChildren().add(
                                buildCard(items.get(i), ItemCardController.CardMode.DETAIL));
                    }
                    loadOngoing();
                })
        );
    }

    /** Ongoing = status RUNNING → card mở màn hình bid */
    private void loadOngoing() {
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

    @FXML
    private void handleBack() { setGridVisible(false); }


    /**
     * Tạo ItemCard node từ dữ liệu server.
     * parts = ["AUCTION_ITEM", id, name, price, endTime, status, startTime, description]
     */
    private Node buildCard(String[] parts, ItemCardController.CardMode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ITEM_CARD_FXML));
            Node node = loader.load();
            ItemCardController ctrl = loader.getController();

            int    id          = parts.length > 1 ? Integer.parseInt(parts[1])   : 0;
            String name        = parts.length > 2 ? parts[2]                     : "—";
            double price       = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
            String endTime     = parts.length > 4 ? parts[4]                     : "";
            String startTime   = parts.length > 6 ? parts[6]                     : "";
            String description = parts.length > 7 ? parts[7]                     : "";
            String imageUrl    = parts.length > 8 ? parts[8]                     : "";

            ctrl.setAuctionData(id, name, price, startTime, endTime, description, mode, imageUrl);
            // Gắn controller vào properties để updateAuctionPrice() có thể tìm lại
            node.getProperties().put("controller", ctrl);
            return node;

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load item card", e);
        }
    }
    @FXML
    private void handleAddItem(ActionEvent e) throws java.io.IOException {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/org/example/view/additem.fxml"));
        javafx.scene.Parent root = loader.load();
        javafx.stage.Stage stage =
                (javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.show();
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

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (usernameLabel != null) {
            usernameLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        }
    }

    public void setCurrentUser(String username, String role) {
        if (usernameLabel != null) {
            usernameLabel.setText(username + " (" + role + ")");
        }
        if (addItemBtn != null && "SELLER".equals(role)) {
            addItemBtn.setVisible(true);
            addItemBtn.setManaged(true);
        }
    }
    @FXML
    public void handleDashboard(){
        overlayDashboard.setVisible(true);
        overlayDashboard.setManaged(true);
    }
    @FXML
    public void handleCloseDashboard(){
        overlayDashboard.setVisible(false);
        overlayDashboard.setManaged(false);
    }
    public void onNewAuction() {
        loadUpcoming();
    }

    public void updateAuctionPrice(int auctionId, double newPrice, String bidder) {
        updateCardsInContainer(ongoingHbox, auctionId, newPrice, bidder);
        if ("RUNNING".equals(currentGridType)) {
            updateCardsInContainer(gridPane, auctionId, newPrice, bidder);
        }
    }

    public void onAuctionFinished(int auctionId, String winner, double finalPrice) {
        System.out.printf("Auction #%d finished — Winner: %s, Final: %,.0f VND%n",
                auctionId, winner, finalPrice);
        // Reload cả hai hàng để đồng bộ trạng thái
        loadUpcoming();
    }


    private void wireAutocomplete() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String trimmed = newVal.trim();
            if (trimmed.isEmpty()) { hideSuggestions(); return; }
            if (pendingSuggest != null && !pendingSuggest.isDone()) pendingSuggest.cancel(false);
            pendingSuggest = debounceExecutor.schedule(() ->
                    AuctionClient.getInstance().requestSuggestAuctions(trimmed, names ->
                            Platform.runLater(() -> showSuggestions(names))
                    ), 250, TimeUnit.MILLISECONDS
            );
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) hideSuggestions();
        });
        searchField.focusedProperty().addListener((obs, was, isFocused) -> {
            if (!isFocused)
                debounceExecutor.schedule(() -> Platform.runLater(this::hideSuggestions), 150, TimeUnit.MILLISECONDS);
        });
    }

    private void showSuggestions(java.util.List<String> names) {
        suggestionBox.getChildren().clear();
        if (names.isEmpty()) { hideSuggestions(); return; }
        for (String name : names) {
            Button item = new Button("🔍  " + name);
            item.getStyleClass().add("suggest-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnAction(e -> { searchField.setText(name); hideSuggestions(); triggerSearch(name); });
            suggestionBox.getChildren().add(item);
        }
        suggestionBox.setVisible(true);
        suggestionBox.setManaged(true);
        suggestionBox.toFront();
    }

    private void hideSuggestions() {
        suggestionBox.setVisible(false);
        suggestionBox.setManaged(false);
        suggestionBox.getChildren().clear();
    }

    private void triggerSearch(String keyword) {
        currentGridType = "SEARCH";
        setGridVisible(true);
        gridPane.getChildren().clear();
        AuctionClient.getInstance().requestSearchAuctions(keyword, items ->
                Platform.runLater(() -> {
                    gridPane.getChildren().clear();
                    if (items.isEmpty()) gridPane.getChildren().add(new Label("No results for: " + keyword));
                    for (String[] item : items)
                        gridPane.getChildren().add(buildCard(item, ItemCardController.CardMode.BID));
                })
        );
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;
        hideSuggestions();
        triggerSearch(keyword);
    }

    private void updateCardsInContainer(Pane container, int auctionId,
                                        double newPrice, String bidder) {
        for (Node node : container.getChildren()) {
            Object ctrl = node.getProperties().get("controller");
            if (ctrl instanceof ItemCardController) {
                ItemCardController card = (ItemCardController) ctrl;
                if (card.getAuctionId() == auctionId) {
                    card.liveUpdatePrice(newPrice, bidder);
                }
            }
        }
    }
}