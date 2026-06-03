package org.example.uicontroller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.domain.user.User;
import org.example.factory.ServiceFactory;
import org.example.server.AuctionClient;
import org.example.util.LanguageManager;
import org.example.util.Theme;
import org.example.util.ThemeManager;

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
    @FXML private Button      categoryButton;
    @FXML private Button      auctionButton;
    @FXML private Label      usernameLabel; // có trong FXML navbar
    @FXML private AnchorPane overlayDashboard;
    @FXML private Button     addItemBtn;
    @FXML private TextField searchField;
    @FXML private VBox suggestionBox;
    @FXML private Button settingsBtn;
    @FXML private VBox   settingsSubPanel;
    @FXML private Button langEnBtn;
    @FXML private Button langViBtn;
    @FXML private Button themeLightBtn;
    @FXML private Button themeDarkBtn;
    @FXML private StackPane searchContainer;
    @FXML private Button catArtBtn;
    @FXML private Button catElectronicsBtn;
    @FXML private Button catEstateBtn;
    @FXML private Button catFashionsBtn;
    @FXML private Button catVehiclesBtn;
    @FXML private Button catOthersBtn;
    @FXML private Button auctionUpcomingBtn;
    @FXML private Button auctionOngoingBtn;
    private boolean settingsExpanded = false;
    private String  currentLang      = "EN";
    private Theme currentTheme     = ThemeManager.getCurrentTheme();
    private VBox openMenu = null;
    @FXML private ImageView navAvatarView;
    @FXML private StackPane navAvatarPane;
    @FXML private ImageView navDefaultIcon;   // thêm mới
    @FXML private Label     panelUsernameLabel; // thêm mới
    @FXML private Label     panelRoleLabel;     // thêm mới
    @FXML private ImageView panelAvatarView;    // thêm mới
    @FXML private Label     panelAvatarIcon;
    @FXML private Button      introButton;
    @FXML private Button      contactButton;
    @FXML private Label       upcomingTitleLabel;
    @FXML private Label       ongoingTitleLabel;
    @FXML private Button      viewAllUpcoming;
    @FXML private Button      viewAllOngoing;
    @FXML private Label       myAccountLabel;
    @FXML private Button      editProfileBtn;
    @FXML private Label       accountSectionLabel;
    @FXML private Label       languageSettingLabel;
    @FXML private Label       themeSettingLabel;
    @FXML private Button      logOutBtn;
    @FXML private Label      balanceLabel;
    @FXML private AnchorPane depositOverlay;
    @FXML private TextField  depositAmountField;
    @FXML private TextField  depositNoteField;
    @FXML private Label depositTitleLabel;
    @FXML private Label depositAmountLabel;
    @FXML private Label depositNoteLabel;
    @FXML private Label depositInfoLabel;
    @FXML private Button depositCancelBtn;
    @FXML private Button depositSubmitBtn;
    @FXML private Label balanceTitleLabel;
    @FXML private Button depositBtn;
    @FXML private Button sellerMyItemsBtn;
    @FXML private Button bidderWonAuctionsBtn;





    private String currentGridType; // "OPEN" hoặc "RUNNING" – để biết grid đang xem loại nào
    private static final String ITEM_CARD_FXML = "/org/example/view/itemcard.fxml";
    private static final int PREVIEW_COUNT = 7;
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingSuggest;
    private final org.example.service.DepositService depositService = org.example.factory.ServiceFactory.getInstance().getDepositService();

    // FIX: không tự new() — instance được set từ ngoài vào bằng setInstance()
    // Nếu tự new() thì @FXML fields sẽ null hết vì JavaFX không inject vào
    private static MainScreenController instance;

    private User currentUser;

    @FXML
    public void initialize() {
        instance = this;
        wireAutocomplete();
        loadUpcoming();
        updateLangButtons();
        updateThemeButtons();
        wireDropdownClicks();
        wireGlobalClickToCloseDropdowns();
        loadOngoing();
        applyLanguage();
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
    private void handleOpenProfile(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/view/profile.fxml"),LanguageManager.getBundle());
        Parent root = loader.load();
        ProfileController ctrl = loader.getController();
        ctrl.setCurrentUser(AuctionClient.getInstance().getCurrentUser()); // truyền user hiện tại

        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ITEM_CARD_FXML),LanguageManager.getBundle());
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
                getClass().getResource("/org/example/view/additem.fxml"),LanguageManager.getBundle());
        javafx.scene.Parent root = loader.load();
        javafx.stage.Stage stage =
                (javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }
    private void wireDropdownClicks() {
        categoryButton.setOnAction(e -> toggleMenu(categoryMenu));
        auctionButton .setOnAction(e -> toggleMenu(auctionMenu));
    }

    private void toggleMenu(VBox menu) {
        if (openMenu == menu) {
            // Đang mở → đóng
            closeAllMenus();
        } else {
            // Đóng menu cũ, mở menu mới
            closeAllMenus();
            menu.setVisible(true);
            menu.setManaged(true);
            openMenu = menu;
            // Highlight nút trigger tương ứng
            setTriggerActive(menu, true);
        }
    }

    private void closeAllMenus() {
        if (openMenu != null) {
            openMenu.setVisible(false);
            openMenu.setManaged(false);
            setTriggerActive(openMenu, false);
            openMenu = null;
        }
    }

    /** Highlight/bỏ highlight nút trigger khi menu mở/đóng */
    private void setTriggerActive(VBox menu, boolean active) {
        Button trigger = (menu == categoryMenu) ? categoryButton : auctionButton;
        if (active) {
            if (!trigger.getStyleClass().contains("nav-link-active"))
                trigger.getStyleClass().add("nav-link-active");
        } else {
            trigger.getStyleClass().remove("nav-link-active");
        }
    }

    /**
     * Click bất kỳ đâu trên Scene (filter = capture phase)
     * → đóng dropdown nếu click KHÔNG nằm trong menu và KHÔNG phải trigger.
     * Dùng addEventFilter ở root scene để bắt mọi click trước khi node con xử lý.
     */
    private void wireGlobalClickToCloseDropdowns() {
        // Scene chưa có khi initialize() chạy → đợi scene gắn vào
        categoryBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (openMenu == null) return;
                    Node target = (Node) e.getTarget();
                    // Nếu click vào bên trong menu hoặc vào trigger button → không đóng
                    if (isInsideNode(target, openMenu)
                            || isInsideNode(target, categoryBox)
                            || isInsideNode(target, auctionBox)) {
                        return;
                    }
                    closeAllMenus();
                });
            }
        });
    }

    /** Kiểm tra xem node có nằm trong (hoặc chính là) ancestor không */
    private boolean isInsideNode(Node node, Node ancestor) {
        Node current = node;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent();
        }
        return false;
    }

    private void setGridVisible(boolean show) {
        mainContent.setVisible(!show);
        mainContent.setManaged(!show);
        gridScroll.setVisible(show);
        gridScroll.setManaged(show);
        backButton.setVisible(show);
        backButton.setManaged(show);
    }

    @FXML
    private void handleToggleSettings() {
        settingsExpanded = !settingsExpanded;
        settingsSubPanel.setVisible(settingsExpanded);
        settingsSubPanel.setManaged(settingsExpanded);

        if (settingsExpanded) {
            settingsBtn.getStyleClass().removeAll("panel-nav-btn");
            if (!settingsBtn.getStyleClass().contains("panel-nav-btn-expanded"))
                settingsBtn.getStyleClass().add("panel-nav-btn-expanded");
            settingsBtn.setText(LanguageManager.get("main.panel.settings.open"));
        } else {
            settingsBtn.getStyleClass().removeAll("panel-nav-btn-expanded");
            if (!settingsBtn.getStyleClass().contains("panel-nav-btn"))
                settingsBtn.getStyleClass().add("panel-nav-btn");
            settingsBtn.setText(LanguageManager.get("main.panel.settings.closed"));
        }
    }
    @FXML
    private void handleLangEn() {
        LanguageManager.setEnglish();
        updateLangButtons();
        applyLanguage();
        refreshVisibleCardsLanguage();
    }

    @FXML
    private void handleLangVi() {
        LanguageManager.setVietnamese();
        updateLangButtons();
        applyLanguage();
        refreshVisibleCardsLanguage();
    }

    private void updateLangButtons() {
        if (langEnBtn == null || langViBtn == null) return;
        setToggleActive(langEnBtn, "toggle-btn-left", !LanguageManager.isVietnamese());
        setToggleActive(langViBtn, "toggle-btn-right", LanguageManager.isVietnamese());
    }
    private void applyLanguage() {
        if (categoryButton != null) categoryButton.setText(LanguageManager.get("main.nav.categories"));
        if (auctionButton != null) auctionButton.setText(LanguageManager.get("main.nav.auctions"));
        if (introButton != null) introButton.setText(LanguageManager.get("main.nav.introduction"));
        if (contactButton != null) contactButton.setText(LanguageManager.get("main.nav.contact"));

        if (addItemBtn != null) addItemBtn.setText(LanguageManager.get("main.btn.add_item"));
        if (searchField != null) searchField.setPromptText(LanguageManager.get("main.search.placeholder"));

        if (upcomingTitleLabel != null) upcomingTitleLabel.setText(LanguageManager.get("main.section.upcoming"));
        if (ongoingTitleLabel != null) ongoingTitleLabel.setText(LanguageManager.get("main.section.ongoing"));
        if (viewAllUpcoming != null) viewAllUpcoming.setText(LanguageManager.get("main.btn.view_all"));
        if (viewAllOngoing != null) viewAllOngoing.setText(LanguageManager.get("main.btn.view_all"));

        if (myAccountLabel != null) myAccountLabel.setText(LanguageManager.get("main.panel.my_account"));
        if (editProfileBtn != null) editProfileBtn.setText(LanguageManager.get("main.panel.edit_profile"));
        if (accountSectionLabel != null) accountSectionLabel.setText(LanguageManager.get("main.panel.account"));
        if (settingsBtn != null) {
            settingsBtn.setText(settingsExpanded
                    ? LanguageManager.get("main.panel.settings.open")
                    : LanguageManager.get("main.panel.settings.closed"));
        }
        if (languageSettingLabel != null) languageSettingLabel.setText(LanguageManager.get("main.panel.language"));
        if (themeSettingLabel != null) themeSettingLabel.setText(LanguageManager.get("main.panel.theme"));
        if (logOutBtn != null) logOutBtn.setText(LanguageManager.get("main.panel.logout"));
        if (backButton != null) backButton.setText(LanguageManager.get("main.btn.back"));

        if (catArtBtn != null) catArtBtn.setText(LanguageManager.get("main.category.art"));
        if (catElectronicsBtn != null) catElectronicsBtn.setText(LanguageManager.get("main.category.electronics"));
        if (catEstateBtn != null) catEstateBtn.setText(LanguageManager.get("main.category.estate"));
        if (catFashionsBtn != null) catFashionsBtn.setText(LanguageManager.get("main.category.fashions"));
        if (catVehiclesBtn != null) catVehiclesBtn.setText(LanguageManager.get("main.category.vehicles"));
        if (catOthersBtn != null) catOthersBtn.setText(LanguageManager.get("main.category.others"));

        if (auctionUpcomingBtn != null) auctionUpcomingBtn.setText(LanguageManager.get("main.auction.upcoming"));
        if (auctionOngoingBtn != null) auctionOngoingBtn.setText(LanguageManager.get("main.auction.ongoing"));
        if (depositTitleLabel != null) depositTitleLabel.setText(LanguageManager.get("deposit.title"));
        if (depositAmountLabel != null) depositAmountLabel.setText(LanguageManager.get("deposit.amount"));
        if (depositAmountField != null) depositAmountField.setPromptText(LanguageManager.get("deposit.amount.placeholder"));
        if (depositNoteLabel != null) depositNoteLabel.setText(LanguageManager.get("deposit.note"));
        if (depositNoteField != null) depositNoteField.setPromptText(LanguageManager.get("deposit.note.placeholder"));
        if (depositInfoLabel != null) depositInfoLabel.setText(LanguageManager.get("deposit.info"));
        if (depositCancelBtn != null) depositCancelBtn.setText(LanguageManager.get("common.cancel"));
        if (depositSubmitBtn != null) depositSubmitBtn.setText(LanguageManager.get("deposit.submit"));
        if (balanceTitleLabel != null) balanceTitleLabel.setText(LanguageManager.get("main.panel.balance"));
        if (depositBtn != null) depositBtn.setText(LanguageManager.get("deposit.open"));
        if (sellerMyItemsBtn != null) sellerMyItemsBtn.setText(LanguageManager.get("main.panel.my_items"));
        if (bidderWonAuctionsBtn != null) bidderWonAuctionsBtn.setText(LanguageManager.get("main.panel.won_auctions"));
    }
    private void refreshVisibleCardsLanguage() {
        refreshCardsInContainer(upcomingHbox);
        refreshCardsInContainer(ongoingHbox);
        refreshCardsInContainer(gridPane);
    }

    private void refreshCardsInContainer(Pane container) {
        if (container == null) return;

        for (Node node : container.getChildren()) {
            Object ctrl = node.getProperties().get("controller");
            if (ctrl instanceof ItemCardController card) {
                card.refreshLanguage();
            }
        }
    }

    // ─── Theme toggles ──────────────────────────────────────────────────────
    @FXML
    private void handleThemeLight() {
        currentTheme = Theme.LIGHT;

        ThemeManager.setTheme(currentTheme);

        Scene scene = themeLightBtn.getScene();

        ThemeManager.applyTheme(scene);

        updateThemeButtons();
    }

    @FXML
    private void handleThemeDark() {
        currentTheme = Theme.DARK;
        ThemeManager.setTheme(currentTheme);
        Scene scene = themeDarkBtn.getScene();
        ThemeManager.applyTheme(scene);
        updateThemeButtons();
    }

    private void updateThemeButtons() {
        if (themeLightBtn == null || themeDarkBtn == null) return;
        setToggleActive(themeLightBtn, "toggle-btn-left",  currentTheme.equals(Theme.LIGHT));
        setToggleActive(themeDarkBtn,  "toggle-btn-right", currentTheme.equals(Theme.DARK));
    }

    /** Swap active/inactive style on a toggle button while keeping its base side-class */
    private void setToggleActive(Button btn, String baseClass, boolean active) {
        btn.getStyleClass().removeAll("toggle-btn-active");
        if (!btn.getStyleClass().contains(baseClass))
            btn.getStyleClass().add(baseClass);
        if (active)
            btn.getStyleClass().add("toggle-btn-active");
    }

    private void setMenuVisible(VBox menu, boolean visible) {
        menu.setVisible(visible);
        menu.setManaged(visible);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (usernameLabel != null) {
            // Hiện full name nếu có, không thì dùng username
            String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                    ? user.getFullName()
                    : user.getUsername();
            usernameLabel.setText(displayName + " (" + user.getRole() + ")");
            // Cập nhật dashboard panel
            if (panelUsernameLabel != null)
                panelUsernameLabel.setText(user.getFullName() != null && !user.getFullName().isBlank()
                        ? user.getFullName() : user.getUsername());
            if (panelRoleLabel != null)
                panelRoleLabel.setText(user.getRole() != null ? user.getRole() : "BIDDER");
            updateBalanceLabel(user.getBalance());
            updateRoleActions(user.getRole());

// Hiện avatar ở cả navbar lẫn panel
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Image avatar = new Image(user.getAvatarUrl(), true);
                avatar.progressProperty().addListener((obs, o, newV) -> {
                    if (newV.doubleValue() >= 1.0 && !avatar.isError()) {
                        Platform.runLater(() -> applyAvatarToNavbar(avatar));
                        Platform.runLater(() -> applyAvatarToPanel(avatar));
                    }
                });
            }
        }
        // Hiện avatar nếu có
        if (navAvatarView != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Image avatar = new Image(user.getAvatarUrl(), true);
            navAvatarView.setImage(avatar);
            // Clip tròn
            double r = 13;
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(r, r, r);
            navAvatarView.setClip(clip);
            // Bo tròn StackPane
            navAvatarPane.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-background-radius: 999;" +
                            "-fx-border-radius: 999;"
            );
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
        updateRoleActions(role);
        // Load đầy đủ User object để lấy avatar + full name
        User user = ServiceFactory.getInstance().getUserService().findUser(username);
        if (user != null) {
            setCurrentUser(user); // gọi lại overload bên trên
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
        if (settingsExpanded) handleToggleSettings();
    }
    public void onNewAuction() {
        loadUpcoming();
        loadOngoing();
    }

    public void updateAuctionPrice(int auctionId, double newPrice, String bidder, String newEndTime) {
        updateCardsInContainer(ongoingHbox, auctionId, newPrice, bidder, newEndTime);
        if ("RUNNING".equals(currentGridType)) {
            updateCardsInContainer(gridPane, auctionId, newPrice, bidder, newEndTime);
        }
    }

    public void onAuctionFinished(int auctionId, String winner, double finalPrice) {
        System.out.printf("Auction #%d finished — Winner: %s, Final: %,.0f VND%n",
                auctionId, winner, finalPrice);
        // Reload cả hai hàng để đồng bộ trạng thái
        loadUpcoming();
        loadOngoing();
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
        if (searchContainer != null && searchContainer.getScene() != null) {
            javafx.geometry.Bounds bounds = searchContainer.localToScene(searchContainer.getBoundsInLocal());
            AnchorPane.setTopAnchor(suggestionBox, bounds.getMaxY() + 2);
            AnchorPane.setLeftAnchor(suggestionBox, bounds.getMinX());
            AnchorPane.setRightAnchor(suggestionBox, null);  // xóa rightAnchor cố định
        }

        suggestionBox.setVisible(true);
        suggestionBox.setManaged(true);
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
                    if (items.isEmpty()) gridPane.getChildren().add(new Label(LanguageManager.get("main.no_results") + " " + keyword));
                    for (String[] item : items)
                        gridPane.getChildren().add(buildCard(item, ItemCardController.CardMode.BID));
                })
        );
    }

    @FXML
    private void handleCategory(ActionEvent e) {
        String type = ((Button) e.getSource()).getUserData().toString();
        setMenuVisible(categoryMenu, false);
        currentGridType = "CATEGORY";
        setGridVisible(true);
        gridPane.getChildren().clear();

        AuctionClient.getInstance().requestAuctionsByCategory(type, items ->
                Platform.runLater(() -> {
                    gridPane.getChildren().clear();
                    if (items.isEmpty()) {
                        gridPane.getChildren().add(new Label(LanguageManager.get("main.no_category") + " " + type));
                        return;
                    }
                    for (String[] item : items) {
                        String status = item.length > 5 ? item[5] : "";
                        ItemCardController.CardMode mode = "OPEN".equals(status)
                                ? ItemCardController.CardMode.DETAIL
                                : ItemCardController.CardMode.BID;
                        gridPane.getChildren().add(buildCard(item, mode));
                    }
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
                                        double newPrice, String bidder,String newEndTime) {
        for (Node node : container.getChildren()) {
            Object ctrl = node.getProperties().get("controller");
            if (ctrl instanceof ItemCardController) {
                ItemCardController card = (ItemCardController) ctrl;
                if (card.getAuctionId() == auctionId) {
                    card.liveUpdatePrice(newPrice, bidder);
                    if (newEndTime!=null){
                        card.liveUpdateEndTime(newEndTime);
                    }
                }
            }
        }
    }
    @FXML
    private void handleLogout(ActionEvent e) {
        AuctionClient.getInstance().disconnect();

        try {
            FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/org/example/view/login.fxml"),LanguageManager.getBundle());
            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) e.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    private void applyAvatarToNavbar(Image avatar) {
        if (navAvatarView == null) return;
        navAvatarView.setImage(avatar);
        navAvatarView.setVisible(true);
        if (navDefaultIcon != null) navDefaultIcon.setVisible(false);
        double r = navAvatarView.getFitWidth() / 2.0;
        navAvatarView.setClip(new javafx.scene.shape.Circle(r, r, r));
    }

    private void applyAvatarToPanel(Image avatar) {
        if (panelAvatarView == null) return;
        panelAvatarView.setImage(avatar);
        panelAvatarView.setVisible(true);
        if (panelAvatarIcon != null) panelAvatarIcon.setVisible(false);
        double r = panelAvatarView.getFitWidth() / 2.0;
        panelAvatarView.setClip(new javafx.scene.shape.Circle(r, r, r));
    }

    public void updateBalanceLabel(double balance) {
        if (balanceLabel != null) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi","VN"));
            balanceLabel.setText(fmt.format((long) balance) + " ₫");
        }
    }

    /** Hiển thị thông báo khi yêu cầu nạp tiền bị Admin từ chối */
    public void showDepositRejectedNotification() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Nạp tiền bị từ chối");
        alert.setHeaderText(null);
        alert.setContentText("Yêu cầu nạp tiền của bạn đã bị Admin từ chối.");
        alert.showAndWait();
    }

    private void updateRoleActions(String role) {
        boolean isSeller = "SELLER".equals(role);
        boolean isBidder = "BIDDER".equals(role);
        if (addItemBtn != null) {
            addItemBtn.setVisible(isSeller);
            addItemBtn.setManaged(isSeller);
        }
        if (sellerMyItemsBtn != null) {
            sellerMyItemsBtn.setVisible(isSeller);
            sellerMyItemsBtn.setManaged(isSeller);
        }
        if (bidderWonAuctionsBtn != null) {
            bidderWonAuctionsBtn.setVisible(isBidder);
            bidderWonAuctionsBtn.setManaged(isBidder);
        }
    }

    @FXML
    private void handleMyItems() {
        handleCloseDashboard();
        currentGridType = "MY_ITEMS";
        setGridVisible(true);
        gridPane.getChildren().clear();
        AuctionClient.getInstance().requestMyItems(items -> Platform.runLater(() -> {
            gridPane.getChildren().clear();
            if (items.isEmpty()) {
                gridPane.getChildren().add(new Label(LanguageManager.get("main.my_items.empty")));
                return;
            }
            for (String[] item : items) {
                gridPane.getChildren().add(buildSellerAuctionCard(item));
            }
        }));
    }

    @FXML
    private void handleWonAuctions() {
        handleCloseDashboard();
        currentGridType = "WON_AUCTIONS";
        setGridVisible(true);
        gridPane.getChildren().clear();
        AuctionClient.getInstance().requestWonAuctions(items -> Platform.runLater(() -> {
            gridPane.getChildren().clear();
            if (items.isEmpty()) {
                gridPane.getChildren().add(new Label(LanguageManager.get("main.won_auctions.empty")));
                return;
            }
            for (String[] item : items) {
                gridPane.getChildren().add(buildWonAuctionCard(item));
            }
        }));
    }

    private Node buildSellerAuctionCard(String[] p) {
        AuctionRow row = parseAuctionRow(p);

        VBox card = baseManagementCard(row);
        Label descriptionLabel = new Label(row.description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("item-time");
        if (!row.description.isBlank()) {
            card.getChildren().add(descriptionLabel);
        }

        HBox actions = new HBox(8);
        if (!row.supportsActions) {
            Label note = new Label(LanguageManager.get("main.my_items.server_old"));
            note.setWrapText(true);
            note.getStyleClass().add("item-time");
            card.getChildren().add(note);
        } else if ("OPEN".equals(row.status)) {
            Button edit = new Button(LanguageManager.get("main.item.edit"));
            edit.getStyleClass().add("button-login");
            edit.setOnAction(e -> editScheduledItem(p));
            Button cancel = new Button(LanguageManager.get("main.auction.cancel"));
            cancel.getStyleClass().add("btn-cancel");
            cancel.setOnAction(e -> cancelAuction(row.auctionId));
            actions.getChildren().addAll(edit, cancel);
        } else if ("FINISHED".equals(row.status)) {
            Button close = new Button(LanguageManager.get("main.auction.close_winner"));
            close.getStyleClass().add("button-login");
            close.setOnAction(e -> closeAuction(row.auctionId));
            actions.getChildren().add(close);
        }
        card.getChildren().add(actions);
        return card;
    }

    private Node buildWonAuctionCard(String[] p) {
        AuctionRow row = parseAuctionRow(p);
        VBox card = baseManagementCard(row);
        Label note = new Label("PAID".equals(row.status)
                ? LanguageManager.get("main.won_auctions.paid")
                : LanguageManager.get("main.won_auctions.waiting"));
        note.getStyleClass().add("item-time");
        card.getChildren().add(note);
        return card;
    }

    private VBox baseManagementCard(AuctionRow row) {
        VBox card = new VBox(8);
        card.setPrefWidth(300);
        card.getStyleClass().add("item-card");
        card.setPadding(new javafx.geometry.Insets(14));
        Label title = new Label(row.name);
        title.getStyleClass().add("item-name");
        title.setWrapText(true);

        Label typeLabel = new Label(LanguageManager.get("main.item.type") + ": " + row.type);
        typeLabel.getStyleClass().add("item-time");
        Label priceLabel = new Label(LanguageManager.get("common.label.current_price") + " " + formatVND(row.price));
        priceLabel.getStyleClass().add("item-price");
        Label startLabel = new Label(LanguageManager.get("itemcard.opens") + " " + formatDateTime(row.startTime));
        startLabel.getStyleClass().add("item-time");
        Label endLabel = new Label(LanguageManager.get("itemcard.ends") + " " + formatDateTime(row.endTime));
        endLabel.getStyleClass().add("item-time");
        Label statusLabel = new Label(LanguageManager.get("main.auction.status") + ": " + statusLabel(row.status));
        statusLabel.getStyleClass().add("panel-role-badge");
        card.getChildren().addAll(title, typeLabel, priceLabel, startLabel, endLabel, statusLabel);
        if (!row.winner.isBlank()) {
            Label winnerLabel = new Label(LanguageManager.get("main.auction.winner") + ": " + row.winner);
            winnerLabel.getStyleClass().add("item-time");
            card.getChildren().add(winnerLabel);
        }
        return card;
    }

    private void editScheduledItem(String[] p) {
        int auctionId = parseIntPart(p, 1);
        String name = prompt(LanguageManager.get("main.item.edit_name"), part(p, 3));
        if (name == null) return;
        String desc = prompt(LanguageManager.get("main.item.edit_desc"), part(p, 9));
        if (desc == null) return;
        String priceText = prompt(LanguageManager.get("main.item.edit_price"), part(p, 4));
        if (priceText == null) return;
        String start = prompt(LanguageManager.get("main.item.edit_start"), part(p, 6));
        if (start == null) return;
        String end = prompt(LanguageManager.get("main.item.edit_end"), part(p, 7));
        if (end == null) return;
        try {
            double price = Double.parseDouble(priceText.trim());
            java.time.LocalDateTime.parse(start.trim());
            java.time.LocalDateTime.parse(end.trim());
            AuctionClient.getInstance().updateScheduledItem(auctionId, name.trim(), desc.trim(), price,
                    start.trim(), end.trim(), (success, msg) -> {
                        showInfo(success ? LanguageManager.get("main.item.updated") : msg);
                        handleMyItems();
                    });
        } catch (Exception ex) {
            showInfo(LanguageManager.get("main.item.invalid_update"));
        }
    }

    private void cancelAuction(int auctionId) {
        if (!confirm(LanguageManager.get("main.auction.cancel"), LanguageManager.get("main.auction.cancel_confirm"))) return;
        AuctionClient.getInstance().cancelAuction(auctionId, (success, msg) -> {
            showInfo(success ? LanguageManager.get("main.auction.cancelled") : msg);
            handleMyItems();
        });
    }

    private void closeAuction(int auctionId) {
        if (!confirm(LanguageManager.get("main.auction.close_winner"), LanguageManager.get("main.auction.close_confirm"))) return;
        AuctionClient.getInstance().closeAuction(auctionId, (success, msg) -> {
            showInfo(success ? LanguageManager.get("main.auction.closed") : msg);
            handleMyItems();
        });
    }

    private String prompt(String title, String initial) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(title);
        return dialog.showAndWait().orElse(null);
    }

    private boolean confirm(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK;
    }

    private void showInfo(String message) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private String part(String[] p, int index) {
        return p.length > index && p[index] != null ? p[index] : "";
    }

    private int parseIntPart(String[] p, int index) {
        try { return Integer.parseInt(part(p, index)); } catch (Exception ignored) { return 0; }
    }

    private double parseDoublePart(String[] p, int index) {
        try { return Double.parseDouble(part(p, index)); } catch (Exception ignored) { return 0; }
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private String formatDateTime(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return java.time.LocalDateTime.parse(iso).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception ignored) {
            return iso;
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OPEN" -> LanguageManager.get("main.auction.status.open");
            case "RUNNING" -> LanguageManager.get("main.auction.status.running");
            case "FINISHED" -> LanguageManager.get("main.auction.status.finished");
            case "PAID" -> LanguageManager.get("main.auction.status.paid");
            case "CANCELED" -> LanguageManager.get("main.auction.status.cancelled");
            default -> status;
        };
    }

    private AuctionRow parseAuctionRow(String[] p) {
        boolean newFormat = p.length > 8 && isKnownAuctionStatus(part(p, 8));
        if (newFormat) {
            return new AuctionRow(
                    parseIntPart(p, 1),
                    part(p, 2),
                    part(p, 3),
                    parseDoublePart(p, 4),
                    part(p, 5),
                    part(p, 6),
                    part(p, 7),
                    part(p, 8),
                    part(p, 9),
                    part(p, 11),
                    true
            );
        }
        return new AuctionRow(
                0,
                part(p, 1),
                part(p, 2),
                parseDoublePart(p, 3),
                part(p, 4),
                part(p, 5),
                part(p, 6),
                "OPEN",
                "",
                "",
                false
        );
    }

    private boolean isKnownAuctionStatus(String status) {
        return "OPEN".equals(status) || "RUNNING".equals(status) || "FINISHED".equals(status)
                || "PAID".equals(status) || "CANCELED".equals(status);
    }

    private record AuctionRow(int auctionId, String itemId, String name, double price, String type,
                              String startTime, String endTime, String status, String description,
                              String winner, boolean supportsActions) {}

    @FXML private void handleOpenDeposit() {
        if (depositOverlay != null) {
            depositAmountField.clear(); depositNoteField.clear();
            depositOverlay.setVisible(true); depositOverlay.setManaged(true);
        }
    }

    @FXML private void handleCloseDeposit() {
        if (depositOverlay != null) {
            depositOverlay.setVisible(false); depositOverlay.setManaged(false);
        }
    }

    @FXML private void handleSubmitDeposit() {
        String amountText = depositAmountField.getText().trim().replace(",","").replace(".","");
        String note = depositNoteField.getText().trim();
        double amount;
        try { amount = Double.parseDouble(amountText); }
        catch (NumberFormatException e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    LanguageManager.get("deposit.invalid_amount")).showAndWait(); return;
        }
        if (currentUser == null) return;
        String result = depositService.requestDeposit(currentUser.getId(), currentUser.getUsername(), amount, note);
        handleCloseDeposit();
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, result).showAndWait();
    }
}