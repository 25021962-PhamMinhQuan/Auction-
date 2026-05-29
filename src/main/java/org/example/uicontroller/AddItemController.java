package org.example.uicontroller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.server.AuctionClient;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.util.SupabaseStorage;
import org.example.util.ThemeManager;

import java.io.File;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddItemController {

    // ─── FXML ───
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        nameField;
    @FXML private TextField         descField;
    @FXML private TextField        priceField;
    @FXML private DatePicker       startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;
    @FXML private DatePicker       endDatePicker;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;
    @FXML private Label            statusLabel;
    @FXML private Button           submitBtn;
    @FXML private javafx.scene.image.ImageView imagePreview;
    @FXML private Label            imagePathLabel;
    private String selectedImagePath = null;

    // Panel danh sách item của seller
    @FXML private VBox             myItemsBox;


    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(
                "ART", "ELECTRONICS", "ESTATE", "FASHIONS", "VEHICLES", "OTHERS"));
        typeCombo.getSelectionModel().selectFirst();
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) hours.add(String.format("%02d", h));

// Populate phút theo bước 5
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int m = 0; m < 60; m += 5) minutes.add(String.format("%02d", m));

        startHourCombo.setItems(hours);
        endHourCombo.setItems(FXCollections.observableArrayList(hours));
        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(FXCollections.observableArrayList(minutes));

        // Mặc định: hôm nay, bắt đầu 09:00 / kết thúc 21:00
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
        startHourCombo.setValue("09");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");
        loadMyItems();
    }

    // ─── Load danh sách item của seller ───

    private void loadMyItems() {
        myItemsBox.getChildren().clear();
        AuctionClient.getInstance().requestMyItems(items -> {
            Platform.runLater(() -> {
                myItemsBox.getChildren().clear();
                if (items.isEmpty()) {
                    myItemsBox.getChildren().add(new Label("Chưa có item nào."));
                    return;
                }
                for (String[] p : items) {
                    // p = ["MY_ITEM", id, name, startPrice, type, startTime, endTime]
                    String id    = p.length > 1 ? p[1] : "";
                    String name  = p.length > 2 ? p[2] : "";
                    double price = p.length > 3 ? Double.parseDouble(p[3]) : 0;
                    String type  = p.length > 4 ? p[4] : "";
                    String startStr = p.length > 5 ? p[5] : "";
                    LocalDateTime itemStartTime = null;
                    try {
                        if (!startStr.isEmpty() && !startStr.equals("null"))
                            itemStartTime = LocalDateTime.parse(startStr);
                    } catch (Exception ignored) {}

                    VBox card = new VBox(4);
                    card.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 6; "
                            + "-fx-background-color: #f8f9fa; -fx-background-radius: 6; "
                            + "-fx-padding: 10;");

                    Label lName  = new Label(name);
                    lName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    Label lInfo  = new Label(type + "  |  Giá khởi điểm: "
                            + String.format("%,.0f VND", price));
                    lInfo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                    HBox btnRow = new HBox(8);
                    if (itemStartTime != null && itemStartTime.isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
                        // Đã lên lịch, chưa đến giờ → không cho start thủ công
                        Label scheduledLbl = new Label("⏳ Đã lên lịch: " + itemStartTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        scheduledLbl.setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        btnRow.getChildren().add(scheduledLbl);
                    } else {
                        Button startBtn = new Button("▶ Bắt đầu đấu giá");
                        startBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; "
                                + "-fx-font-weight: bold; -fx-cursor: hand;");
                        startBtn.setOnAction(e -> handleStartAuction(id, name, startBtn));
                    }
                    Button deleteBtn = new Button("🗑 Xóa");
                    deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; "
                            + "-fx-font-weight: bold; -fx-cursor: hand;");
                    deleteBtn.setOnAction(e -> handleDeleteItem(id, name, card));


                    card.getChildren().addAll(lName, lInfo, btnRow);
                    myItemsBox.getChildren().add(card);
                }
            });
        });
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) submitBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        // Preview ngay lập tức
        imagePreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
        imagePathLabel.setText("Đang upload...");
        submitBtn.setDisable(true);

        // Upload lên Supabase trên background thread
        Thread uploadThread = new Thread(() -> {
            String url = SupabaseStorage.uploadImage(file);
            javafx.application.Platform.runLater(() -> {
                submitBtn.setDisable(false);
                if (url != null) {
                    selectedImagePath = url;   // ← lưu URL thay vì đường dẫn local
                    imagePathLabel.setText("✓ Upload thành công");
                } else {
                    selectedImagePath = null;
                    imagePathLabel.setText("✗ Upload thất bại");
                }
            });
        });
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    // ─── Submit item mới ───

    @FXML
    private void handleSubmit() {
        String type        = typeCombo.getValue();
        String name        = nameField.getText().trim();
        String description = descField.getText().trim();
        String priceStr    = priceField.getText().trim();

        // Validate text fields
        if (name.isEmpty() || priceStr.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ thông tin.", false);
            return;
        }

        // Validate date/time pickers
        if (startDatePicker.getValue() == null
                || startHourCombo.getValue() == null
                || startMinuteCombo.getValue() == null
                || endDatePicker.getValue() == null
                || endHourCombo.getValue() == null
                || endMinuteCombo.getValue() == null) {
            showStatus("Vui lòng chọn đầy đủ ngày và giờ.", false);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus("Giá khởi điểm phải là số dương.", false);
            return;
        }

        LocalDateTime start = LocalDateTime.of(
                startDatePicker.getValue(),
                java.time.LocalTime.of(
                        Integer.parseInt(startHourCombo.getValue()),
                        Integer.parseInt(startMinuteCombo.getValue())));

        LocalDateTime end = LocalDateTime.of(
                endDatePicker.getValue(),
                java.time.LocalTime.of(
                        Integer.parseInt(endHourCombo.getValue()),
                        Integer.parseInt(endMinuteCombo.getValue())));

        if (!end.isAfter(start)) {
            showStatus("Thời gian kết thúc phải sau thời gian bắt đầu.", false);
            return;
        }

        submitBtn.setDisable(true);
        showStatus("Đang gửi...", true);

        DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        AuctionClient.getInstance().addItem(
                type, name, description, price,
                start.format(ISO), end.format(ISO),
                selectedImagePath,
                (success, msg) -> {
                    submitBtn.setDisable(false);
                    if (success) {
                        showStatus("✓ Đã thêm item \"" + name + "\" thành công!", true);
                        clearForm();
                        loadMyItems();
                    } else {
                        showStatus("Lỗi: " + msg, false);
                    }
                }
        );
    }

    // ─── Start auction ───

     private void handleStartAuction(String itemId, String itemName, Button btn) {
        btn.setDisable(true);
        btn.setText("Đang xử lý...");

        AuctionClient.getInstance().startAuction(itemId, (success, msg) -> {
            if (success) {
                btn.setText("✓ Đang đấu giá");
                btn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
                showStatus("Phiên đấu giá cho \"" + itemName + "\" đã bắt đầu!", true);
            } else {
                btn.setDisable(false);
                btn.setText("▶ Bắt đầu đấu giá");
                showStatus("Lỗi: " + msg, false);
            }
        });
    }
    private void handleDeleteItem(String itemId, String itemName, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa item \"" + itemName + "\"?");
        confirm.setContentText("Hành động này không thể hoàn tác.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                AuctionClient.getInstance().deleteItem(itemId, (success, msg) -> {
                    if (success) {
                        Platform.runLater(() -> {
                            myItemsBox.getChildren().remove(card);
                            if (myItemsBox.getChildren().isEmpty()) {
                                myItemsBox.getChildren().add(new Label("Chưa có item nào."));
                            }
                            showStatus("✓ Đã xóa item \"" + itemName + "\".", true);
                        });
                    } else {
                        showStatus("Lỗi khi xóa: " + msg, false);
                    }
                });
            }
        });
    }

    // ─── Back ───

    @FXML
    private void handleBack(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"));
        Parent root = loader.load();
        MainScreenController controller = loader.getController();
        MainScreenController.setInstance(controller);
        String username = AuctionClient.getInstance().getCurrentUsername();
        String role = AuctionClient.getInstance().getCurrentRole();
        if (username != null && role != null) {
            controller.setCurrentUser(username, role);
        }
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    // ─── Helpers ───

    private void showStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle(ok
                ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
    }

    private void clearForm() {
        nameField.clear();
        descField.clear();
        priceField.clear();
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
        startHourCombo.setValue("09");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");
        typeCombo.getSelectionModel().selectFirst();
        selectedImagePath = null;
        imagePreview.setImage(null);
        imagePathLabel.setText("Chưa chọn ảnh");
    }

}
