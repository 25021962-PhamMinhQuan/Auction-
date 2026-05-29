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
import javafx.beans.property.SimpleStringProperty;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class AdminScreenController {
    private final UserService userService = ServiceFactory.getInstance().getUserService();
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
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private Runnable pendingConfirmAction;

    // ═══════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════

    @FXML
    public void initialize() {
        setupTables();
        setupSearchFields();
        showDashboard();
        loadDashboardStats();
    }

    private void setupTables() {
        colUserId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getId()));
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(nullToDash(data.getValue().getEmail())));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole()));
        colUserStatus.setCellValueFactory(data ->
                new SimpleStringProperty("ACTIVE"));
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
        for (VBox s : new VBox[]{sectionDashboard, sectionUsers, sectionItems, sectionAuctions}) {
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
        for (Button b : new Button[]{navDashboard, navUsers, navItems, navAuctions}) {
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
        statBanned.setText("0");
        statPendingItems.setText("0");
        statTodayBids.setText("0");

        recentActivityList.getChildren().clear();
        recentActivityList.getChildren().add(new Label("Dashboard đã được cập nhật."));
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
        userTable.setItems(FXCollections.observableArrayList());
        showInfo("Thông báo", "Chức năng lọc tài khoản bị khóa cần cột status/is_locked trong bảng account.");
    }

    @FXML
    public void handleBanUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo("Chưa chọn tài khoản", "Vui lòng chọn một tài khoản trước.");
            return;
        }

        showInfo("Thông báo", "Chức năng khóa tài khoản cần bổ sung cột status/is_locked trong bảng account.");
    }

    @FXML
    public void handleUnbanUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo("Chưa chọn tài khoản", "Vui lòng chọn một tài khoản trước.");
            return;
        }

        showInfo("Thông báo", "Chức năng mở khóa tài khoản cần bổ sung cột status/is_locked trong bảng account.");
    }

    @FXML
    public void handleViewUserDetail() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo("Chưa chọn tài khoản", "Vui lòng chọn một tài khoản trước.");
            return;
        }
        showInfo("Chi tiết tài khoản",
                "ID: " + selectedUser.getId()
                        + "\nUsername: " + selectedUser.getUsername()
                        + "\nRole: " + selectedUser.getRole()
                        + "\nEmail: " + nullToDash(selectedUser.getEmail())
                        + "\nPhone: " + nullToDash(selectedUser.getPhone()));
    }

    @FXML
    public void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showInfo("Chưa chọn tài khoản", "Vui lòng chọn một tài khoản trước.");
            return;
        }
        showConfirm("Xóa tài khoản",
                "Hành động này không thể hoàn tác.\nTài khoản \"" + selectedUser.getUsername() + "\" sẽ bị xóa.",
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
                .filter(item -> item.getStartTime() != null
                        && item.getStartTime().isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
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
            showInfo("Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trước.");
            return;
        }

        showInfo("Thông báo", "Sản phẩm hiện đang được tạo trực tiếp bởi Seller, chưa có trạng thái PENDING để duyệt.");
    }

    @FXML
    public void handleViewItemDetail() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showInfo("Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trước.");
            return;
        }
        showInfo("Chi tiết sản phẩm",
                "ID: " + selectedItem.getId()
                        + "\nTên: " + selectedItem.getName()
                        + "\nLoại: " + selectedItem.getType()
                        + "\nGiá hiện tại: " + formatMoney(selectedItem.getCurrentPrice())
                        + "\nBắt đầu: " + formatDateTime(selectedItem.getStartTime())
                        + "\nKết thúc: " + formatDateTime(selectedItem.getEndTime())
                        + "\nMô tả: " + nullToDash(selectedItem.getDescription()));
    }

    @FXML
    public void handleDeleteItem() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showInfo("Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trước.");
            return;
        }

        showConfirm("Xóa sản phẩm",
                "Sản phẩm \"" + selectedItem.getName() + "\" sẽ bị xóa khỏi hệ thống. Tiếp tục?",
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
            showInfo("Chưa chọn phiên đấu giá", "Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        showConfirm("Dừng phiên đấu giá",
                "Phiên đấu giá #" + selectedAuction.getId() + " sẽ bị dừng ngay lập tức. Tiếp tục?",
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
            showInfo("Chưa chọn phiên đấu giá", "Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        showInfo("Chi tiết phiên đấu giá",
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
            showInfo("Chưa chọn phiên đấu giá", "Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        showConfirm("Xóa phiên đấu giá",
                "Phiên đấu giá #" + selectedAuction.getId() + " sẽ bị xóa. Tiếp tục?",
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
            pendingConfirmAction.run();
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
        showConfirm("Đăng xuất",
                "Bạn có chắc muốn đăng xuất khỏi trang quản trị?",
                () -> {
                    try {
                        AuctionClient.getInstance().disconnect();

                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/org/example/view/login.fxml"));
                        Parent root = loader.load();

                        Stage stage = (Stage) adminUsernameLabel.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.centerOnScreen();
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showInfo("Lỗi", "Không thể đăng xuất. Vui lòng thử lại.");
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


}

