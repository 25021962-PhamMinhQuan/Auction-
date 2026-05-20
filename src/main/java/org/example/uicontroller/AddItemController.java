package org.example.uicontroller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddItemController {

    // ─── FXML ───
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField        nameField;
    @FXML private TextField         descField;
    @FXML private TextField        priceField;
    @FXML private TextField        startTimeField;   // "dd/MM/yyyy HH:mm"
    @FXML private TextField        endTimeField;
    @FXML private Label            statusLabel;
    @FXML private Button           submitBtn;

    // Panel danh sách item của seller
    @FXML private VBox             myItemsBox;

    private static final DateTimeFormatter INPUT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(
                "ART", "ELECTRONIC", "ESTATE", "FASHIONS", "VEHICLES", "OTHERS"));
        typeCombo.getSelectionModel().selectFirst();
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

                    VBox card = new VBox(4);
                    card.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 6; "
                            + "-fx-background-color: #f8f9fa; -fx-background-radius: 6; "
                            + "-fx-padding: 10;");

                    Label lName  = new Label(name);
                    lName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    Label lInfo  = new Label(type + "  |  Giá khởi điểm: "
                            + String.format("%,.0f VND", price));
                    lInfo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                    Button startBtn = new Button("▶ Bắt đầu đấu giá");
                    startBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; "
                            + "-fx-font-weight: bold; -fx-cursor: hand;");
                    startBtn.setOnAction(e -> handleStartAuction(id, name, startBtn));

                    card.getChildren().addAll(lName, lInfo, startBtn);
                    myItemsBox.getChildren().add(card);
                }
            });
        });
    }

    // ─── Submit item mới ───

    @FXML
    private void handleSubmit() {
        String type        = typeCombo.getValue();
        String name        = nameField.getText().trim();
        String description = descField.getText().trim();
        String priceStr    = priceField.getText().trim();
        String startStr    = startTimeField.getText().trim();
        String endStr      = endTimeField.getText().trim();

        // Validate
        if (name.isEmpty() || priceStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ thông tin.", false);
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

        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(startStr, INPUT_FMT);
            end   = LocalDateTime.parse(endStr,   INPUT_FMT);
        } catch (DateTimeParseException e) {
            showStatus("Định dạng thời gian: dd/MM/yyyy HH:mm", false);
            return;
        }

        if (!end.isAfter(start)) {
            showStatus("Thời gian kết thúc phải sau thời gian bắt đầu.", false);
            return;
        }

        submitBtn.setDisable(true);
        showStatus("Đang gửi...", true);

        // ISO format để gửi lên server
        DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        AuctionClient.getInstance().addItem(
                type, name, description, price,
                start.format(ISO), end.format(ISO),
                (success, msg) -> {
                    submitBtn.setDisable(false);
                    if (success) {
                        showStatus("✓ Đã thêm item \"" + name + "\" thành công!", true);
                        clearForm();
                        loadMyItems();   // refresh danh sách
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

    // ─── Back ───

    @FXML
    private void handleBack(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/mainscreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
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
        startTimeField.clear();
        endTimeField.clear();
        typeCombo.getSelectionModel().selectFirst();
    }
}
