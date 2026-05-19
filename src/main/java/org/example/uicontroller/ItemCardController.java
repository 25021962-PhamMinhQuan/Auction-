package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.domain.item.Item;

import java.io.IOException;

public class ItemCardController {
    @FXML private Label     itemname;
    @FXML private Label     price;
    @FXML private Label     timeopen;
    @FXML private ImageView itemImage;
    @FXML private Button    detailsbutton;

    private Item currentItem;

    // Dùng khi có Item thực từ server
    public void setData(Item item) {
        this.currentItem = item;
        itemname.setText(item.getName());
        price.setText(String.valueOf(item.getCurrentPrice()));
        timeopen.setText(item.getStartTime() != null
                ? item.getStartTime().toLocalTime().toString()
                : "N/A");
    }

    // Dùng tạm khi chưa có Item thực (placeholder từ MainScreenController)
    public void setData(String name, String priceStr, String time) {
        itemname.setText(name);
        price.setText(priceStr);
        timeopen.setText(time);
    }

    @FXML
    private void handleDetail() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml")
        );
        Parent root = loader.load();
        ItemBidingUIController controller = loader.getController();

        if (currentItem != null) {
            // FIX: getId() trả String, ItemBidingUIController.setData() nhận int
            // parse sang int, nếu lỗi dùng 0
            int id;
            try { id = Integer.parseInt(currentItem.getId()); }
            catch (NumberFormatException e) { id = 0; }

            controller.setData(id, currentItem.getName(), currentItem.getCurrentPrice());
        } else {
            controller.setData(0, itemname.getText(), 0);
        }

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}