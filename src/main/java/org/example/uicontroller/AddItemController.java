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
import org.example.util.LanguageManager;

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
    }

    // ─── Load danh sách item của seller ───

    private void loadMyItems() {
        if (myItemsBox == null) return;
        myItemsBox.getChildren().clear();
        AuctionClient.getInstance().requestMyItems(items -> {
            Platform.runLater(() -> {
                myItemsBox.getChildren().clear();
                if (items.isEmpty()) {
                    myItemsBox.getChildren().add(new Label(LanguageManager.get("additem.no_items")));
                    return;
                }
                for (String[] p : items) {
                    // p = ["MY_ITEM", auctionId, itemId, name, currentPrice, type, startTime, endTime, status, ...]
                    String id    = p.length > 2 ? p[2] : "";
                    String name  = p.length > 3 ? p[3] : "";
                    double price = p.length > 4 ? Double.parseDouble(p[4]) : 0;
                    String type  = p.length > 5 ? p[5] : "";
                    String startStr = p.length > 6 ? p[6] : "";
                    String status = p.length > 8 ? p[8] : "";
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
                    Label lInfo  = new Label(type + LanguageManager.get("additem.info.prefix")
                            + String.format("%,.0f VND", price));
                    lInfo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                    Label statusLbl = new Label("Status: " + status);
                    statusLbl.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                    HBox btnRow = new HBox(8);
                    if ("OPEN".equals(status) && itemStartTime != null && itemStartTime.isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
                        // Đã lên lịch, chưa đến giờ → không cho start thủ công
                        Label scheduledLbl = new Label(LanguageManager.get("additem.scheduled") + itemStartTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        scheduledLbl.setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        btnRow.getChildren().add(scheduledLbl);
                    } else if (!"RUNNING".equals(status) && !"FINISHED".equals(status) && !"PAID".equals(status) && !"CANCELED".equals(status)) {
                        Button startBtn = new Button(LanguageManager.get("additem.auction.start"));
                        startBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; "
                                + "-fx-font-weight: bold; -fx-cursor: hand;");
                        startBtn.setOnAction(e -> handleStartAuction(id, name, startBtn));
                    }
                    Button deleteBtn = new Button(LanguageManager.get("additem.error.delete").replace(":", ""));
                    deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; "
                            + "-fx-font-weight: bold; -fx-cursor: hand;");
                    deleteBtn.setOnAction(e -> handleDeleteItem(id, name, card));


                    card.getChildren().addAll(lName, lInfo, statusLbl, btnRow);
                    myItemsBox.getChildren().add(card);
                }
            });
        });
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.get("additem.image.choose"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) submitBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        // Preview ngay lập tức
        imagePreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
        imagePathLabel.setText(LanguageManager.get("additem.image.uploading"));
        submitBtn.setDisable(true);

        // Upload lên Supabase trên background thread
        Thread uploadThread = new Thread(() -> {
            String url = SupabaseStorage.uploadImage(file);
            javafx.application.Platform.runLater(() -> {
                submitBtn.setDisable(false);
                if (url != null) {
                    selectedImagePath = url;   // ← lưu URL thay vì đường dẫn local
                    imagePathLabel.setText(LanguageManager.get("additem.image.success"));
                } else {
                    selectedImagePath = null;
                    imagePathLabel.setText(LanguageManager.get("additem.image.failed"));
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
            showStatus(LanguageManager.get("additem.fill_all"), false);
            return;
        }

        // Validate date/time pickers
        if (startDatePicker.getValue() == null
                || startHourCombo.getValue() == null
                || startMinuteCombo.getValue() == null
                || endDatePicker.getValue() == null
                || endHourCombo.getValue() == null
                || endMinuteCombo.getValue() == null) {
            showStatus(LanguageManager.get("additem.fill_datetime"), false);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus(LanguageManager.get("additem.error.price"), false);
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
            showStatus(LanguageManager.get("additem.end_after_start"), false);
            return;
        }

        submitBtn.setDisable(true);
        showStatus(LanguageManager.get("additem.sending"), true);

        DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        AuctionClient.getInstance().addItem(
                type, name, description, price,
                start.format(ISO), end.format(ISO),
                selectedImagePath,
                (success, msg) -> {
                    submitBtn.setDisable(false);
                    if (success) {
                        showStatus(String.format(LanguageManager.get("additem.submitted"), name), true);
                        clearForm();
                    } else {
                        showStatus(LanguageManager.get("additem.error.prefix") + " " + msg, false);
                    }
                }
        );
    }

    // ─── Start auction ───

     private void handleStartAuction(String itemId, String itemName, Button btn) {
        btn.setDisable(true);
        btn.setText(LanguageManager.get("additem.processing"));

        AuctionClient.getInstance().startAuction(itemId, (success, msg) -> {
            if (success) {
                btn.setText(LanguageManager.get("additem.auction.active"));
                btn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
                showStatus(String.format(LanguageManager.get("additem.auction.started"), itemName), true);
            } else {
                btn.setDisable(false);
                btn.setText(LanguageManager.get("additem.auction.start"));
                showStatus(LanguageManager.get("additem.error.prefix") + " " + msg, false);
            }
        });
    }
    private void handleDeleteItem(String itemId, String itemName, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(LanguageManager.get("additem.confirm.delete.title"));
        confirm.setHeaderText(String.format(LanguageManager.get("additem.confirm.delete.header"), itemName));
        confirm.setContentText(LanguageManager.get("additem.confirm.delete.content"));

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                AuctionClient.getInstance().deleteItem(itemId, (success, msg) -> {
                    if (success) {
                        Platform.runLater(() -> {
                            myItemsBox.getChildren().remove(card);
                            if (myItemsBox.getChildren().isEmpty()) {
                                myItemsBox.getChildren().add(new Label(LanguageManager.get("additem.no_items")));
                            }
                            showStatus("✓ Đã xóa item \"" + itemName + "\".", true);
                        });
                    } else {
                        showStatus(LanguageManager.get("additem.error.delete") + " " + msg, false);
                    }
                });
            }
        });
    }

    // ─── Back ───

    @FXML
    private void handleBack(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"),LanguageManager.getBundle());
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
        imagePathLabel.setText(LanguageManager.get("additem.image.none"));
    }

}
