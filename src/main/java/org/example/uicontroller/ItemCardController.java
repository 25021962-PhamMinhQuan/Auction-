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
    @FXML
    private Label itemname;
    @FXML
    private Label price;
    @FXML
    private Label timeopen;
    @FXML
    private ImageView itemImage;
    @FXML
    private Button detailsbutton;

    public void setData(Item item) {
        itemname.setText(item.getName());
        price.setText(String.valueOf(item.getCurrentPrice()));
        timeopen.setText("10.AM");
    }


    @FXML
    private void handleDetail() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/view/itemdetail.fxml")
        );

        Parent root = loader.load();

        ItemBidingUIController controller = loader.getController();
        controller.setData(123, "something", 10000);

        Stage stage = (Stage) detailsbutton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}


