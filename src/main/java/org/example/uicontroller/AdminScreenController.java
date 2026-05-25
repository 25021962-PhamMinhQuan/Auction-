package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AdminScreenController {

    // ── Topbar ──
    @FXML private Label adminUsernameLabel;

    // ── Side panel ──
    @FXML private AnchorPane overlayAdminPanel;
    @FXML private Label      panelAdminName;

    // ── Sidebar nav buttons ──
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navItems;
    @FXML private Button navAuctions;

    // ── Content sections ──
    @FXML private VBox sectionDashboard;
    @FXML private VBox sectionUsers;
    @FXML private VBox sectionItems;
    @FXML private VBox sectionAuctions;

    // ── Dashboard stat labels ──
    @FXML private Label statTotalUsers;
    @FXML private Label statTotalItems;
    @FXML private Label statActiveAuctions;
    @FXML private Label statTotalRevenue;
    @FXML private Label statBanned;
    @FXML private Label statUpcoming;
    @FXML private Label statPendingItems;
    @FXML private Label statTodayBids;
    @FXML private VBox  recentActivityList;

    // ── User Management ──
    @FXML private TextField userSearchField;
    @FXML private TableView<?> userTable;
    @FXML private TableColumn<?, ?> colUserId;
    @FXML private TableColumn<?, ?> colUsername;
    @FXML private TableColumn<?, ?> colEmail;
    @FXML private TableColumn<?, ?> colRole;
    @FXML private TableColumn<?, ?> colUserStatus;
    @FXML private TableColumn<?, ?> colUserDate;

    // ── Item Management ──
    @FXML private TextField itemSearchField;
    @FXML private TableView<?> itemTable;
    @FXML private TableColumn<?, ?> colItemId;
    @FXML private TableColumn<?, ?> colItemName;
    @FXML private TableColumn<?, ?> colItemSeller;
    @FXML private TableColumn<?, ?> colItemCategory;
    @FXML private TableColumn<?, ?> colItemPrice;
    @FXML private TableColumn<?, ?> colItemStatus;

    // ── Auction Management ──
    @FXML private TextField auctionSearchField;
    @FXML private TableView<?> auctionTable;
    @FXML private TableColumn<?, ?> colAuctionId;
    @FXML private TableColumn<?, ?> colAuctionItem;
    @FXML private TableColumn<?, ?> colAuctionStart;
    @FXML private TableColumn<?, ?> colAuctionEnd;
    @FXML private TableColumn<?, ?> colAuctionPrice;
    @FXML private TableColumn<?, ?> colAuctionStatus;

    // ── Confirm Dialog ──
    @FXML private StackPane confirmOverlay;
    @FXML private Label     confirmTitle;
    @FXML private Label     confirmMessage;
    @FXML private Button    confirmOkBtn;

    private Runnable pendingConfirmAction;

    // ═══════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════

    @FXML
    public void initialize() {
        showDashboard();
        loadDashboardStats();
    }

    /** Gán user admin hiện tại vào màn hình */
    public void setAdminUser(org.example.domain.user.User user) {
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

    /** Ẩn tất cả section, hiện section được chọn */
    private void switchSection(VBox target) {
        for (VBox s : new VBox[]{sectionDashboard, sectionUsers, sectionItems, sectionAuctions}) {
            s.setVisible(false);
            s.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);
    }

    /** Đặt active style cho nút sidebar được chọn */
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
        // TODO: gọi service để lấy thống kê thực
        // Ví dụ:
        // statTotalUsers.setText(String.valueOf(userService.countAll()));
        // statTotalItems.setText(String.valueOf(itemService.countAll()));
        // statActiveAuctions.setText(String.valueOf(auctionService.countRunning()));
        // statTotalRevenue.setText(NumberFormat...format(auctionService.totalRevenue()) + " ₫");
    }

    // ═══════════════════════════════════════════
    //  USER MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadUsers() {
        // TODO: userTable.setItems(FXCollections.observableList(userService.findAll()));
    }

    @FXML public void refreshUsers()   { loadUsers(); }
    @FXML public void filterAllUsers() { loadUsers(); }

    @FXML
    public void filterLockedUsers() {
        // TODO: userTable.setItems(...userService.findBanned()...);
    }

    @FXML
    public void handleBanUser() {
        showConfirm("Khóa tài khoản",
                "Tài khoản này sẽ bị khóa và không thể đăng nhập.\nBạn có chắc không?",
                () -> {
                    // TODO: userService.ban(selectedUser.getId());
                    loadUsers();
                });
    }

    @FXML
    public void handleUnbanUser() {
        // TODO: userService.unban(selectedUser.getId()); loadUsers();
    }

    @FXML
    public void handleViewUserDetail() {
        // TODO: mở cửa sổ profile detail hoặc show panel bên phải
    }

    @FXML
    public void handleDeleteUser() {
        showConfirm("Xóa tài khoản",
                "Hành động này không thể hoàn tác.\nToàn bộ dữ liệu của tài khoản sẽ bị xóa.",
                () -> {
                    // TODO: userService.delete(selectedUser.getId());
                    loadUsers();
                });
    }

    // ═══════════════════════════════════════════
    //  ITEM MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadItems() {
        // TODO: itemTable.setItems(FXCollections.observableList(itemService.findAll()));
    }

    @FXML public void refreshItems()      { loadItems(); }
    @FXML public void filterAllItems()    { loadItems(); }
    @FXML public void filterPendingItems(){ /* TODO: itemService.findPending() */ }
    @FXML public void filterActiveItems() { /* TODO: itemService.findActive() */ }

    @FXML
    public void handleApproveItem() {
        // TODO: itemService.approve(selectedItem.getId()); loadItems();
    }

    @FXML
    public void handleViewItemDetail() {
        // TODO: mở ItemDetailController
    }

    @FXML
    public void handleDeleteItem() {
        showConfirm("Xóa sản phẩm",
                "Sản phẩm sẽ bị xóa khỏi hệ thống. Tiếp tục?",
                () -> {
                    // TODO: itemService.delete(selectedItem.getId());
                    loadItems();
                });
    }

    // ═══════════════════════════════════════════
    //  AUCTION MANAGEMENT
    // ═══════════════════════════════════════════

    private void loadAuctions() {
        // TODO: auctionTable.setItems(FXCollections.observableList(auctionService.findAll()));
    }

    @FXML public void refreshAuctions() { loadAuctions(); }

    @FXML
    public void handleStopAuction() {
        showConfirm("Dừng phiên đấu giá",
                "Phiên đấu giá sẽ bị dừng ngay lập tức. Tiếp tục?",
                () -> {
                    // TODO: auctionService.stop(selectedAuction.getId());
                    loadAuctions();
                });
    }

    @FXML public void handleViewAuction() { /* TODO */ }

    @FXML
    public void handleDeleteAuction() {
        showConfirm("Xóa phiên đấu giá",
                "Phiên đấu giá và toàn bộ lịch sử đặt giá sẽ bị xóa. Tiếp tục?",
                () -> {
                    // TODO: auctionService.delete(selectedAuction.getId());
                    loadAuctions();
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
                    // TODO: chuyển về màn hình login
                    // AuctionApplication.showLogin();
                });
    }
}
