package org.example.uicontroller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.domain.auction.Auction;
import org.example.domain.item.Item;
import org.example.domain.user.User;
import org.example.factory.ServiceFactory;
import org.example.server.AuctionClient;
import org.example.service.AuctionService;
import org.example.service.ItemService;
import org.example.service.UserService;
import org.example.util.LanguageManager;
import javafx.beans.property.SimpleStringProperty;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.domain.user.DepositRequest;
import org.example.service.DepositService;
import org.example.util.Theme;
import org.example.util.ThemeManager;

public class AdminScreenController {
    private final UserService userService = ServiceFactory.getInstance().getUserService();
    private final DepositService depositService = ServiceFactory.getInstance().getDepositService();
    private final ItemService itemService = ServiceFactory.getInstance().getItemService();
    private final AuctionService auctionService = ServiceFactory.getInstance().getAuctionService();
    // ── Topbar ──
    private User currentAdmin;
    @FXML
    private Label adminUsernameLabel;

    // ── Side panel ──
    @FXML
    private AnchorPane overlayAdminPanel;
    @FXML
    private Label panelAdminName;

    // ── Sidebar nav buttons ──
    @FXML
    private Button navDashboard;
    @FXML
    private Button navUsers;
    @FXML
    private Button navItems;
    @FXML
    private Button navAuctions;

    // ── Content sections ──
    @FXML
    private VBox sectionDashboard;
    @FXML
    private VBox sectionUsers;
    @FXML
    private VBox sectionItems;
    @FXML
    private VBox sectionAuctions;

    // ── Dashboard stat labels ──
    @FXML
    private Label statTotalUsers;
    @FXML
    private Label statTotalItems;
    @FXML
    private Label statActiveAuctions;
    @FXML
    private Label statTotalRevenue;
    @FXML
    private Label statBanned;
    @FXML
    private Label statUpcoming;
    @FXML
    private Label statPendingItems;
    @FXML
    private Label statTodayBids;
    @FXML
    private VBox recentActivityList;

    // ── User Management ──
    @FXML
    private TextField userSearchField;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colUserId;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colUserStatus;
    @FXML
    private TableColumn<User, String> colUserDate;
    private final Map<String, String> itemSellerNameCache = new HashMap<>();

    // ── Item Management ──
    @FXML
    private TextField itemSearchField;
    @FXML
    private TableView<Item> itemTable;
    @FXML
    private TableColumn<Item, String> colItemId;
    @FXML
    private TableColumn<Item, String> colItemName;
    @FXML
    private TableColumn<Item, String> colItemSeller;
    @FXML
    private TableColumn<Item, String> colItemCategory;
    @FXML
    private TableColumn<Item, String> colItemPrice;
    @FXML
    private TableColumn<Item, String> colItemStatus;

    // ── Auction Management ──
    @FXML
    private TextField auctionSearchField;
    @FXML
    private TableView<Auction> auctionTable;
    @FXML
    private TableColumn<Auction, String> colAuctionId;
    @FXML
    private TableColumn<Auction, String> colAuctionItem;
    @FXML
    private TableColumn<Auction, String> colAuctionStart;
    @FXML
    private TableColumn<Auction, String> colAuctionEnd;
    @FXML
    private TableColumn<Auction, String> colAuctionPrice;
    @FXML
    private TableColumn<Auction, String> colAuctionStatus;

    // ── Confirm Dialog ──
    @FXML
    private StackPane confirmOverlay;
    @FXML
    private Label confirmTitle;
    @FXML
    private Label confirmMessage;
    @FXML
    private Button confirmOkBtn;

    @FXML private Button navDeposits;
    @FXML private VBox   sectionDeposits;
    @FXML private TableView<DepositRequest>          depositTable;
    @FXML private TableColumn<DepositRequest, String> colDepositId, colDepositUser,
            colDepositAmount, colDepositNote,
            colDepositStatus, colDepositDate;
    @FXML private Label depositActionInfo;
    @FXML private Button adminSettingsBtn;
    @FXML private VBox   adminSettingsSubPanel;
    @FXML private Button adminLangEnBtn;
    @FXML private Button adminLangViBtn;
    @FXML private Button adminThemeLightBtn;
    @FXML private Button adminThemeDarkBtn;
    @FXML private Label adminBadgeLabel;
    @FXML private Label sidebarOverviewLabel;
    @FXML private Label sidebarManagementLabel;
    @FXML private Button sidebarLogoutBtn;

    @FXML private Label dashboardHeadingLabel;
    @FXML private Label dashboardSubtitleLabel;
    @FXML private Label statTotalUsersLabel;
    @FXML private Label statTotalItemsLabel;
    @FXML private Label statActiveAuctionsLabel;
    @FXML private Label statTotalRevenueLabel;
    @FXML private Label recentActivityTitleLabel;
    @FXML private Button viewAllActivityBtn;
    @FXML private Label quickStatsTitleLabel;
    @FXML private Label statBannedLabel;
    @FXML private Label statUpcomingLabel;
    @FXML private Label statPendingItemsLabel;
    @FXML private Label statTodayBidsLabel;

    @FXML private Label usersTitleLabel;
    @FXML private Label usersSubtitleLabel;
    @FXML private Button refreshUsersBtn;
    @FXML private Button filterLockedUsersBtn;
    @FXML private Button filterAllUsersBtn;

    @FXML private Label itemsTitleLabel;
    @FXML private Label itemsSubtitleLabel;
    @FXML private Button refreshItemsBtn;
    @FXML private Button filterPendingItemsBtn;
    @FXML private Button filterActiveItemsBtn;
    @FXML private Button filterAllItemsBtn;

    @FXML private Label auctionsTitleLabel;
    @FXML private Label auctionsSubtitleLabel;
    @FXML private Button refreshAuctionsBtn;
    @FXML private Button filterRunningAuctionsBtn;
    @FXML private Button filterUpcomingAuctionsBtn;
    @FXML private Button filterAllAuctionsBtn;

    @FXML private Label depositsTitleLabel;
    @FXML private Label depositsSubtitleLabel;
    @FXML private Button refreshDepositsBtn;

    @FXML private Label adminRoleLabel;
    @FXML private Label adminLangLabel;
    @FXML private Label adminThemeLabel;
    @FXML private Button panelLogoutBtn;
    @FXML private Button confirmCancelBtn;
    @FXML private Button depositFilterAll;
    @FXML private Button depositFilterPending;
    @FXML private Button depositFilterApproved;
    @FXML private Button depositFilterRejected;
    @FXML private Button btnBanUser;
    @FXML private Button btnUnbanUser;
    @FXML private Button btnViewUserDetail;
    @FXML private Button btnDeleteUser;
    @FXML private Button btnApproveItem;
    @FXML private Button btnViewItemDetail;
    @FXML private Button btnDeleteItem;
    @FXML private Button btnStopAuction;
    @FXML private Button btnViewAuction;
    @FXML private Button btnDeleteAuction;
    @FXML private Button btnApproveDeposit;
    @FXML private Button btnRejectDeposit;


    private boolean adminSettingsExpanded = false;
    private Theme currentTheme = ThemeManager.getCurrentTheme();

    private Runnable pendingConfirmAction;
    private List<DepositRequest> currentDepositList;


    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ═══════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════

    @FXML
    public void initialize() {
        setupTables();
        setupSearchFields();
        showDashboard();
        loadDashboardStats();
        updateAdminThemeButtons();
        updateAdminLangButtons();
    }

    private void setupTables() {
        // Loại bỏ cột trắng thừa cuối bảng
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        auctionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        depositTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colUserId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId()));
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(nullToDash(data.getValue().getEmail())));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole()));
        colUserStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
        colUserDate.setCellValueFactory(data ->
                new SimpleStringProperty("—"));

        colItemId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId()));
        colItemName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        colItemSeller.setCellValueFactory(data ->
                new SimpleStringProperty(resolveSellerName(data.getValue())));
        colItemCategory.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getType()));
        colItemPrice.setCellValueFactory(data ->
                new SimpleStringProperty(formatMoney(data.getValue().getCurrentPrice())));
        colItemStatus.setCellValueFactory(data ->
                new SimpleStringProperty(resolveItemStatus(data.getValue())));

        colAuctionId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colAuctionItem.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getItem().getName()));
        colAuctionStart.setCellValueFactory(data ->
                new SimpleStringProperty(formatDateTime(data.getValue().getItem().getStartTime())));
        colAuctionEnd.setCellValueFactory(data ->
                new SimpleStringProperty(formatDateTime(data.getValue().getItem().getEndTime())));
        colAuctionPrice.setCellValueFactory(data ->
                new SimpleStringProperty(formatMoney(data.getValue().getCurrentPrice())));
        colAuctionStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().name()));

        colDepositId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colDepositUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colDepositAmount.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().getAmount())));
        colDepositNote.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote() != null ? d.getValue().getNote() : "—"));
        colDepositStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colDepositDate.setCellValueFactory(d -> {
            java.time.LocalDateTime dt = d.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DATE_TIME_FMT) : "—");
        });
    }

    private void setupSearchFields() {
        userSearchField.textProperty().addListener((obs, oldValue, newValue) -> loadUsers());
        itemSearchField.textProperty().addListener((obs, oldValue, newValue) -> loadItems());
        auctionSearchField.textProperty().addListener((obs, oldValue, newValue) -> loadAuctions());
    }

    /**
     * Gán user admin hiện tại vào màn hình
     */
    public void setAdminUser(org.example.domain.user.User user) {
        this.currentAdmin = user;
        if (user != null) {
            adminUsernameLabel.setText(user.getUsername());
            panelAdminName.setText(user.getUsername());
        }
    }

    // ═══════════════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ═══════════════════════════════════════════

    @FXML
    public void showDashboard() {
        switchSection(sectionDashboard);
        setActiveNav(navDashboard);
    }

    @FXML
    public void showUserManagement() {
        switchSection(sectionUsers);
        setActiveNav(navUsers);
        loadUsers();
    }

    @FXML
    public void showItemManagement() {
        switchSection(sectionItems);
        setActiveNav(navItems);
        loadItems();
    }

    @FXML
    public void showAuctionManagement() {
        switchSection(sectionAuctions);
        setActiveNav(navAuctions);
        loadAuctions();
    }

    /**
     * Ẩn tất cả section, hiện section được chọn
     */
    private void switchSection(VBox target) {
        for (VBox s : new VBox[]{sectionDashboard, sectionUsers, sectionItems, sectionAuctions, sectionDeposits}) {
            s.setVisible(false);
            s.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);
    }

    /**
     * Đặt active style cho nút sidebar được chọn
     */
    private void setActiveNav(Button active) {
        for (Button b : new Button[]{navDashboard, navUsers, navItems, navAuctions, navDeposits}) {
            b.getStyleClass().removeAll("sidebar-btn-active");
            if (!b.getStyleClass().contains("sidebar-btn"))
                b.getStyleClass().add("sidebar-btn");
        }
        active.getStyleClass().add("sidebar-btn-active");
    }

    // ═══════════════════════════════════════════
    //  DASHBOARD
    // ═══════════════════════════════════════════

    private void loadDashboardStats() {
        long totalUsers = userService.countAllUsers();
        long totalItems = itemService.countAllItems();
        long activeAuctions = auctionService.countRunningAuctions();
        long upcomingAuctions = auctionService.countOpenAuctions();

        statTotalUsers.setText(String.valueOf(totalUsers));
        statTotalItems.setText(String.valueOf(totalItems));
        statActiveAuctions.setText(String.valueOf(activeAuctions));
        statUpcoming.setText(String.valueOf(upcomingAuctions));

        statTotalRevenue.setText("0 ₫");
        statBanned.setText(String.valueOf(userService.countLockedUsers()));
        statPendingItems.setText(String.valueOf(itemService.countPendingItems()));
        statTodayBids.setText("0");

        recentActivityList.getChildren().clear();
        recentActivityList.getChildren().add(new Label(LanguageManager.get("admin.dashboard.updated")));
    }

    // ═══════════════════════════════════════════
    //  USER MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadUsers() {
        String keyword = userSearchField.getText() == null
                ? ""
                : userSearchField.getText().trim().toLowerCase();

        List<User> users = userService.findAllUsers();

        if (!keyword.isEmpty()) {
            users = users.stream()
                    .filter(user ->
                            containsIgnoreCase(user.getId(), keyword)
                                    || containsIgnoreCase(user.getUsername(), keyword)
                                    || containsIgnoreCase(user.getEmail(), keyword)
                                    || containsIgnoreCase(user.getRole(), keyword))
                    .toList();
        }

        userTable.setItems(FXCollections.observableArrayList(users));
    }


    @FXML
    public void refreshUsers() {
        loadUsers();
    }

    @FXML
    public void filterAllUsers() {
        loadUsers();
    }

    @FXML
    public void filterLockedUsers() {
        List<User> users = userService.findAllUsers().stream()
                .filter(userService::isLocked)
                .toList();
        userTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    public void handleBanUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo(LanguageManager.get("admin.account.none_selected"), LanguageManager.get("admin.account.none_selected.msg"));
            return;
        }
        if (User.UserRole.ADMIN.name().equals(selectedUser.getRole())) {
            showInfo(LanguageManager.get("admin.notice"), "Admin account cannot be locked.");
            return;
        }

        showConfirm(LanguageManager.get("admin.account.lock.title"),
                String.format(LanguageManager.get("admin.account.lock.msg"), selectedUser.getUsername()),
                () -> {
                    userService.lockUser(selectedUser.getId());
                    loadUsers();
                    loadDashboardStats();
                });
    }

    @FXML
    public void handleUnbanUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo(LanguageManager.get("admin.account.none_selected"), LanguageManager.get("admin.account.none_selected.msg"));
            return;
        }

        showConfirm(LanguageManager.get("admin.account.unlock.title"),
                String.format(LanguageManager.get("admin.account.unlock.msg"), selectedUser.getUsername()),
                () -> {
                    userService.unlockUser(selectedUser.getId());
                    loadUsers();
                    loadDashboardStats();
                });
    }

    @FXML
    public void handleViewUserDetail() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo(LanguageManager.get("admin.account.none_selected"), LanguageManager.get("admin.account.none_selected.msg"));
            return;
        }
        showInfo(LanguageManager.get("admin.account.detail"),
                "ID: " + selectedUser.getId()
                        + "\nUsername: " + selectedUser.getUsername()
                        + "\nRole: " + selectedUser.getRole()
                        + "\nStatus: " + selectedUser.getStatus()
                        + "\nEmail: " + nullToDash(selectedUser.getEmail())
                        + "\nPhone: " + nullToDash(selectedUser.getPhone()));
    }

    @FXML
    public void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo(LanguageManager.get("admin.account.none_selected"), LanguageManager.get("admin.account.none_selected.msg"));
            return;
        }
        showConfirm(LanguageManager.get("admin.account.delete.title"),
                String.format(LanguageManager.get("admin.account.delete.msg"), selectedUser.getUsername()),
                () -> {
                    userService.deleteUser(selectedUser.getId());
                    loadUsers();
                    loadDashboardStats();
                });
    }

    // ═══════════════════════════════════════════
    //  ITEM MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadItems() {
        String keyword = itemSearchField.getText() == null
                ? ""
                : itemSearchField.getText().trim().toLowerCase();

        List<Item> items = itemService.findAllItems();

        if (!keyword.isEmpty()) {
            items = items.stream()
                    .filter(item ->
                            containsIgnoreCase(item.getId(), keyword)
                                    || containsIgnoreCase(item.getName(), keyword)
                                    || containsIgnoreCase(item.getType(), keyword)
                                    || containsIgnoreCase(item.getDescription(), keyword))
                    .toList();
        }

        itemTable.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    public void refreshItems() {
        loadItems();
    }

    @FXML
    public void filterAllItems() {
        loadItems();
    }

    @FXML
    public void filterPendingItems() {
        List<Item> items = itemService.findAllItems().stream()
                .filter(item -> "PENDING".equalsIgnoreCase(item.getStatus()))
                .toList();
        itemTable.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    public void filterActiveItems() {
        List<Item> items = itemService.findAllItems().stream()
                .filter(item -> item.getStartTime() != null
                        && item.getEndTime() != null
                        && !item.getStartTime().isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                        && item.getEndTime().isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
                .toList();
        itemTable.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    public void handleApproveItem() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showInfo(LanguageManager.get("admin.item.none_selected"), LanguageManager.get("admin.item.none_selected.msg"));
            return;
        }

        if ("APPROVED".equalsIgnoreCase(selectedItem.getStatus())) {
            showInfo(LanguageManager.get("admin.notice"), LanguageManager.get("admin.item.already_approved"));
            return;
        }

        showConfirm(LanguageManager.get("admin.item.approve.title"),
                String.format(LanguageManager.get("admin.item.approve.msg"), selectedItem.getName()),
                () -> AuctionClient.getInstance().approveItem(selectedItem.getId(), (ok, msg) ->
                        javafx.application.Platform.runLater(() -> {
                            if (ok) {
                                loadItems();
                                loadDashboardStats();
                            } else {
                                showInfo(LanguageManager.get("common.error"), msg);
                            }
                        })));
    }

    @FXML
    public void handleViewItemDetail() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showInfo(LanguageManager.get("admin.item.none_selected"), LanguageManager.get("admin.item.none_selected.msg"));
            return;
        }
        showInfo(LanguageManager.get("admin.item.detail"),
                "ID: " + selectedItem.getId()
                        + "\nTên: " + selectedItem.getName()
                        + "\nLoại: " + selectedItem.getType()
                        + "\nTrạng thái: " + selectedItem.getStatus()
                        + "\nGiá hiện tại: " + formatMoney(selectedItem.getCurrentPrice())
                        + "\nBắt đầu: " + formatDateTime(selectedItem.getStartTime())
                        + "\nKết thúc: " + formatDateTime(selectedItem.getEndTime())
                        + "\nMô tả: " + nullToDash(selectedItem.getDescription()));
    }

    @FXML
    public void handleDeleteItem() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showInfo(LanguageManager.get("admin.item.none_selected"), LanguageManager.get("admin.item.none_selected.msg"));
            return;
        }

        showConfirm(LanguageManager.get("admin.item.delete.title"),
                String.format(LanguageManager.get("admin.item.delete.msg"), selectedItem.getName()),
                () -> {
                    itemService.deleteItem(selectedItem.getId(), currentAdmin);
                    loadItems();
                    loadDashboardStats();
                });
    }

    // ═══════════════════════════════════════════
    //  AUCTION MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadAuctions() {
        String keyword = auctionSearchField.getText() == null
                ? ""
                : auctionSearchField.getText().trim().toLowerCase();

        List<Auction> auctions = auctionService.findAllAuctions();

        if (!keyword.isEmpty()) {
            auctions = auctions.stream()
                    .filter(auction ->
                            containsIgnoreCase(String.valueOf(auction.getId()), keyword)
                                    || containsIgnoreCase(auction.getItem().getName(), keyword)
                                    || containsIgnoreCase(auction.getStatus().name(), keyword))
                    .toList();
        }

        auctionTable.setItems(FXCollections.observableArrayList(auctions));
    }

    @FXML
    public void refreshAuctions() {
        loadAuctions();
    }

    @FXML
    public void handleStopAuction() {
        Auction selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            showInfo(LanguageManager.get("admin.auction.none_selected"), LanguageManager.get("admin.auction.none_selected.msg"));
            return;
        }

        showConfirm(LanguageManager.get("admin.auction.stop.title"),
                String.format(LanguageManager.get("admin.auction.stop.msg"), selectedAuction.getId()),
                () -> {
                    auctionService.stopAuction(selectedAuction.getId(), currentAdmin);
                    loadAuctions();
                    loadDashboardStats();
                });
    }

    @FXML
    public void handleViewAuction() {
        Auction selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            showInfo(LanguageManager.get("admin.auction.none_selected"), LanguageManager.get("admin.auction.none_selected.msg"));
            return;
        }

        showInfo(LanguageManager.get("admin.auction.detail"),
                "ID: " + selectedAuction.getId()
                        + "\nSản phẩm: " + selectedAuction.getItem().getName()
                        + "\nTrạng thái: " + selectedAuction.getStatus()
                        + "\nGiá hiện tại: " + formatMoney(selectedAuction.getCurrentPrice())
                        + "\nBắt đầu: " + formatDateTime(selectedAuction.getItem().getStartTime())
                        + "\nKết thúc: " + formatDateTime(selectedAuction.getItem().getEndTime()));
    }

    @FXML
    public void handleDeleteAuction() {
        Auction selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            showInfo(LanguageManager.get("admin.auction.none_selected"), LanguageManager.get("admin.auction.none_selected.msg"));
            return;
        }

        showConfirm(LanguageManager.get("admin.auction.delete.title"),
                String.format(LanguageManager.get("admin.auction.delete.msg"), selectedAuction.getId()),
                () -> {
                    auctionService.deleteAuction(selectedAuction.getId());
                    loadAuctions();
                    loadDashboardStats();
                });
    }

    // ═══════════════════════════════════════════
    //  SIDE PANEL
    // ═══════════════════════════════════════════

    @FXML
    public void handleOpenAdminPanel() {
        overlayAdminPanel.setVisible(true);
        overlayAdminPanel.setManaged(true);
    }

    @FXML
    public void handleCloseAdminPanel() {
        overlayAdminPanel.setVisible(false);
        overlayAdminPanel.setManaged(false);
    }

    // ═══════════════════════════════════════════
    //  CONFIRM DIALOG
    // ═══════════════════════════════════════════

    private void showConfirm(String title, String message, Runnable onOk) {
        confirmTitle.setText(title);
        confirmMessage.setText(message);
        pendingConfirmAction = onOk;
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
    }

    @FXML
    public void handleConfirmOk() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
        if (pendingConfirmAction != null) {
            try {
                pendingConfirmAction.run();
            } catch (Exception e) {
                showInfo(LanguageManager.get("common.error"), e.getMessage());
            }
            pendingConfirmAction = null;
        }
    }

    @FXML
    public void handleConfirmCancel() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
        pendingConfirmAction = null;
    }

    // ═══════════════════════════════════════════
    //  LOGOUT
    // ═══════════════════════════════════════════

    @FXML
    public void handleLogout() {
        showConfirm(LanguageManager.get("admin.logout.confirm").replace("?",""),
                LanguageManager.get("admin.logout.confirm"),
                () -> {
                    try {
                        AuctionClient.getInstance().disconnect();

                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/org/example/view/login.fxml"),LanguageManager.getBundle());
                        Parent root = loader.load();

                        Stage stage = (Stage) adminUsernameLabel.getScene().getWindow();
                        Scene scene = new Scene(root);
                        ThemeManager.applyTheme(scene);
                        stage.setScene(scene);
                        stage.centerOnScreen();
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showInfo(LanguageManager.get("common.error"), LanguageManager.get("admin.logout.error"));
                    }
                });
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String formatMoney(double amount) {
        return VND_FORMAT.format(amount) + " ₫";
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(DATE_TIME_FMT);
    }

    private String resolveItemStatus(Item item) {
        if (item.getStatus() != null && !"APPROVED".equalsIgnoreCase(item.getStatus())) {
            return item.getStatus();
        }
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return "UNKNOWN";
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if (now.isBefore(item.getStartTime())) {
            return "UPCOMING";
        }
        if (now.isAfter(item.getEndTime())) {
            return "ENDED";
        }
        return "ACTIVE";
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private String resolveSellerName(Item item) {
        if (item == null || item.getId() == null) {
            return "—";
        }

        return itemSellerNameCache.computeIfAbsent(item.getId(), itemId -> {
            String sellerId = itemService.getSellerIdByItemId(itemId);
            if (sellerId == null || sellerId.isBlank()) {
                return "—";
            }

            User seller = userService.findUserById(sellerId);
            return seller == null ? "—" : seller.getUsername();
        });
    }

    @FXML
    public void showDepositManagement() {
        switchSection(sectionDeposits);
        setActiveNav(navDeposits);
        loadDeposits();
    }

    private void loadDeposits() {
        currentDepositList = depositService.getAllRequests();
        depositTable.setItems(FXCollections.observableArrayList(currentDepositList));
    }

    @FXML
    public void refreshDeposits() {
        loadDeposits();
    }

    @FXML
    public void filterAllDeposits() {
        currentDepositList = depositService.getAllRequests();
        depositTable.setItems(FXCollections.observableArrayList(currentDepositList));
    }

    @FXML
    public void filterPendingDeposits() {
        currentDepositList = depositService.getPendingRequests();
        depositTable.setItems(FXCollections.observableArrayList(currentDepositList));
    }

    @FXML
    public void filterApprovedDeposits() {
        currentDepositList = depositService.getAllRequests().stream()
                .filter(r -> r.getStatus() == org.example.domain.user.DepositRequest.Status.APPROVED)
                .toList();
        depositTable.setItems(FXCollections.observableArrayList(currentDepositList));
    }

    @FXML
    public void filterRejectedDeposits() {
        currentDepositList = depositService.getAllRequests().stream()
                .filter(r -> r.getStatus() == org.example.domain.user.DepositRequest.Status.REJECTED)
                .toList();
        depositTable.setItems(FXCollections.observableArrayList(currentDepositList));
    }

    @FXML
    public void handleApproveDeposit() {
        DepositRequest selected = depositTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo(LanguageManager.get("admin.deposit.none_selected"),
                    LanguageManager.get("admin.deposit.none_selected.msg"));
            return;
        }
        if (selected.getStatus() != DepositRequest.Status.PENDING) {
            showInfo(LanguageManager.get("admin.deposit.invalid"),
                    LanguageManager.get("admin.deposit.only_pending_approve"));
            return;
        }
        java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi","VN"));
        showConfirm(
                LanguageManager.get("admin.deposit.approve.title"),
                String.format(LanguageManager.get("admin.deposit.approve.msg"),
                        fmt.format((long) selected.getAmount()), selected.getUsername()),
                () -> AuctionClient.getInstance().approveDeposit(selected.getId(), (ok, msg) ->
                        javafx.application.Platform.runLater(() -> {
                            loadDeposits();
                            if (depositActionInfo != null)
                                depositActionInfo.setText(
                                        String.format(LanguageManager.get("admin.deposit.approved_info"), selected.getId()));
                        }))
        );
    }

    @FXML
    public void handleRejectDeposit() {
        DepositRequest selected = depositTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo(LanguageManager.get("admin.deposit.none_selected"),
                    LanguageManager.get("admin.deposit.none_selected.msg"));
            return;
        }
        if (selected.getStatus() != DepositRequest.Status.PENDING) {
            showInfo(LanguageManager.get("admin.deposit.invalid"),
                    LanguageManager.get("admin.deposit.only_pending_reject"));
            return;
        }
        showConfirm(
                LanguageManager.get("admin.deposit.reject.title"),
                String.format(LanguageManager.get("admin.deposit.reject.msg"), selected.getUsername()),
                () -> AuctionClient.getInstance().rejectDeposit(selected.getId(), (ok, msg) ->
                        javafx.application.Platform.runLater(() -> {
                            loadDeposits();
                            if (depositActionInfo != null)
                                depositActionInfo.setText(
                                        String.format(LanguageManager.get("admin.deposit.rejected_info"), selected.getId()));
                        }))
        );
    }

    @FXML
    public void handleToggleAdminSettings() {
        adminSettingsExpanded = !adminSettingsExpanded;
        adminSettingsSubPanel.setVisible(adminSettingsExpanded);
        adminSettingsSubPanel.setManaged(adminSettingsExpanded);

        if (adminSettingsExpanded) {
            adminSettingsBtn.getStyleClass().removeAll("panel-nav-btn");
            if (!adminSettingsBtn.getStyleClass().contains("panel-nav-btn-expanded"))
                adminSettingsBtn.getStyleClass().add("panel-nav-btn-expanded");
            adminSettingsBtn.setText(LanguageManager.get("main.panel.settings.open"));
        } else {
            adminSettingsBtn.getStyleClass().removeAll("panel-nav-btn-expanded");
            if (!adminSettingsBtn.getStyleClass().contains("panel-nav-btn"))
                adminSettingsBtn.getStyleClass().add("panel-nav-btn");
            adminSettingsBtn.setText(LanguageManager.get("main.panel.settings.closed"));
        }
    }

    @FXML
    public void handleAdminLangEn() {
        LanguageManager.setEnglish();
        updateAdminLangButtons();
        applyAdminLanguage();
    }

    @FXML
    public void handleAdminLangVi() {
        LanguageManager.setVietnamese();
        updateAdminLangButtons();
        applyAdminLanguage();
    }

    @FXML
    public void handleAdminThemeLight() {
        currentTheme = Theme.LIGHT;
        ThemeManager.setTheme(currentTheme);
        ThemeManager.applyTheme(adminThemeLightBtn.getScene());
        updateAdminThemeButtons();
    }

    @FXML
    public void handleAdminThemeDark() {
        currentTheme = Theme.DARK;
        ThemeManager.setTheme(currentTheme);
        ThemeManager.applyTheme(adminThemeDarkBtn.getScene());
        updateAdminThemeButtons();
    }

    private void updateAdminThemeButtons() {
        if (adminThemeLightBtn == null || adminThemeDarkBtn == null) return;
        setAdminToggleActive(adminThemeLightBtn, "toggle-btn-left",  currentTheme == Theme.LIGHT);
        setAdminToggleActive(adminThemeDarkBtn,  "toggle-btn-right", currentTheme == Theme.DARK);
    }

    private void updateAdminLangButtons() {
        if (adminLangEnBtn == null || adminLangViBtn == null) return;
        setAdminToggleActive(adminLangEnBtn, "toggle-btn-left",  !LanguageManager.isVietnamese());
        setAdminToggleActive(adminLangViBtn, "toggle-btn-right",  LanguageManager.isVietnamese());
    }

    private void setAdminToggleActive(Button btn, String baseClass, boolean active) {
        btn.getStyleClass().removeAll("toggle-btn-active");
        if (!btn.getStyleClass().contains(baseClass))
            btn.getStyleClass().add(baseClass);
        if (active)
            btn.getStyleClass().add("toggle-btn-active");
    }

    /** Cập nhật lại text cho các label/button trong admin screen khi đổi ngôn ngữ */
    /** Cập nhật lại text cho admin screen khi đổi ngôn ngữ */
    private void applyAdminLanguage() {
        // Top bar
        setText(adminBadgeLabel, "admin.badge");

        // Sidebar
        setText(sidebarOverviewLabel, "admin.sidebar.overview");
        setText(sidebarManagementLabel, "admin.sidebar.management");
        setText(navDashboard, "admin.nav.dashboard");
        setText(navUsers, "admin.nav.accounts");
        setText(navItems, "admin.nav.items");
        setText(navAuctions, "admin.nav.auctions");
        setText(navDeposits, "admin.nav.deposits");
        setText(sidebarLogoutBtn, "admin.nav.logout");

        // Dashboard
        setText(dashboardHeadingLabel, "admin.dashboard.heading");
        setText(dashboardSubtitleLabel, "admin.dashboard.title");
        setText(statTotalUsersLabel, "admin.stat.total_users");
        setText(statTotalItemsLabel, "admin.stat.total_items");
        setText(statActiveAuctionsLabel, "admin.stat.active_auctions");
        setText(statTotalRevenueLabel, "admin.stat.total_revenue");
        setText(recentActivityTitleLabel, "admin.recent_activity");
        setText(viewAllActivityBtn, "admin.btn.view_all");
        setText(quickStatsTitleLabel, "admin.quick_stats");
        setText(statBannedLabel, "admin.stat.banned");
        setText(statUpcomingLabel, "admin.stat.upcoming");
        setText(statPendingItemsLabel, "admin.stat.pending_items");
        setText(statTodayBidsLabel, "admin.stat.today_bids");

        // Users section
        setText(usersTitleLabel, "admin.users.title");
        setText(usersSubtitleLabel, "admin.users.subtitle");
        setText(refreshUsersBtn, "common.refresh");
        setText(filterLockedUsersBtn, "admin.filter.locked");
        setText(filterAllUsersBtn, "admin.filter.all");
        if (userSearchField != null) userSearchField.setPromptText(LanguageManager.get("admin.users.search"));

        setText(btnBanUser, "admin.user.lock");
        setText(btnUnbanUser, "admin.user.unlock");
        setText(btnViewUserDetail, "admin.btn.view_detail");
        setText(btnDeleteUser, "admin.user.delete");

        // Items section
        setText(itemsTitleLabel, "admin.items.title");
        setText(itemsSubtitleLabel, "admin.items.subtitle");
        setText(refreshItemsBtn, "common.refresh");
        setText(filterPendingItemsBtn, "admin.filter.pending");
        setText(filterActiveItemsBtn, "admin.filter.active");
        setText(filterAllItemsBtn, "admin.filter.all");
        if (itemSearchField != null) itemSearchField.setPromptText(LanguageManager.get("admin.items.search"));

        setText(btnApproveItem, "admin.item.approve");
        setText(btnViewItemDetail, "admin.btn.view_detail");
        setText(btnDeleteItem, "admin.item.delete");

        // Auctions section
        setText(auctionsTitleLabel, "admin.auction.title");
        setText(auctionsSubtitleLabel, "admin.auction.subtitle");
        setText(refreshAuctionsBtn, "common.refresh");
        setText(filterRunningAuctionsBtn, "admin.filter.running");
        setText(filterUpcomingAuctionsBtn, "admin.filter.upcoming");
        setText(filterAllAuctionsBtn, "admin.filter.all");
        if (auctionSearchField != null) auctionSearchField.setPromptText(LanguageManager.get("admin.auction.search"));

        setText(btnStopAuction, "admin.auction.stop");
        setText(btnViewAuction, "admin.btn.view_detail");
        setText(btnDeleteAuction, "admin.auction.delete");

        // Deposits section
        setText(depositsTitleLabel, "admin.deposits.title");
        setText(depositsSubtitleLabel, "admin.deposits.subtitle");
        setText(refreshDepositsBtn, "common.refresh");
        setText(depositFilterAll, "admin.filter.all");
        setText(depositFilterPending, "admin.filter.pending");
        setText(depositFilterApproved, "admin.filter.approved");
        setText(depositFilterRejected, "admin.filter.rejected");

        setText(btnApproveDeposit, "admin.deposit.approve");
        setText(btnRejectDeposit, "admin.deposit.reject");

        // Table columns
        setText(colUserId, "admin.col.id");
        setText(colUsername, "admin.col.username");
        setText(colEmail, "admin.col.email");
        setText(colRole, "admin.col.role");
        setText(colUserStatus, "admin.col.status");
        setText(colUserDate, "admin.col.created_at");

        setText(colItemId, "admin.col.id");
        setText(colItemName, "admin.col.item_name");
        setText(colItemSeller, "admin.col.seller");
        setText(colItemCategory, "admin.col.category");
        setText(colItemPrice, "admin.col.start_price");
        setText(colItemStatus, "admin.col.status");

        setText(colAuctionId, "admin.col.id");
        setText(colAuctionItem, "admin.col.item");
        setText(colAuctionStart, "admin.col.start");
        setText(colAuctionEnd, "admin.col.end");
        setText(colAuctionPrice, "admin.col.current_price");
        setText(colAuctionStatus, "admin.col.status");

        setText(colDepositId, "admin.col.id");
        setText(colDepositUser, "admin.col.user");
        setText(colDepositAmount, "admin.col.amount");
        setText(colDepositNote, "admin.col.note");
        setText(colDepositStatus, "admin.col.status");
        setText(colDepositDate, "admin.col.time");

        // Admin panel
        setText(adminRoleLabel, "admin.role");
        setText(adminLangLabel, "main.panel.language");
        setText(adminThemeLabel, "main.panel.theme");
        setText(panelLogoutBtn, "main.panel.logout");

        if (adminSettingsBtn != null) {
            adminSettingsBtn.setText(adminSettingsExpanded
                    ? LanguageManager.get("main.panel.settings.open")
                    : LanguageManager.get("main.panel.settings.closed"));
        }

        // Confirm dialog
        setText(confirmTitle, "common.confirm");
        setText(confirmMessage, "admin.confirm.default_message");
        setText(confirmCancelBtn, "common.cancel");
        setText(confirmOkBtn, "common.confirm");

        // Dynamic dashboard text
        if (recentActivityList != null && !recentActivityList.getChildren().isEmpty()) {
            recentActivityList.getChildren().clear();
            recentActivityList.getChildren().add(new Label(LanguageManager.get("admin.dashboard.updated")));
        }

        updateAdminLangButtons();
    }
    private void setText(Labeled control, String key) {
        if (control != null) {
            control.setText(LanguageManager.get(key));
        }
    }

    private void setText(TableColumn<?, ?> column, String key) {
        if (column != null) {
            column.setText(LanguageManager.get(key));
        }
    }
    @FXML
    public void filterAllAuctions() {
        loadAuctions();
    }

    @FXML
    public void filterRunningAuctions() {
        List<Auction> auctions = auctionService.findAllAuctions().stream()
                .filter(auction -> auction.getStatus() != null
                        && auction.getStatus().name().equalsIgnoreCase("RUNNING"))
                .toList();

        auctionTable.setItems(FXCollections.observableArrayList(auctions));
    }

    @FXML
    public void filterUpcomingAuctions() {
        List<Auction> auctions = auctionService.findAllAuctions().stream()
                .filter(auction -> auction.getItem() != null
                        && auction.getItem().getStartTime() != null
                        && java.time.LocalDateTime.now().isBefore(auction.getItem().getStartTime()))
                .toList();

        auctionTable.setItems(FXCollections.observableArrayList(auctions));
    }
}