package org.example.uicontroller;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

import java.io.IOException;

public class MainScreenController {
    @FXML
    FlowPane itemContainer;
    @FXML
    public void initialize() throws IOException {
        for (int i = 0; i <= 6; i++){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/view/itemcard.fxml"));
            Node item = loader.load();
            ItemCardController controller = loader.getController();
            controller.setData("Item" +i,String.valueOf(i*100),"10AM");
            itemContainer.getChildren().add(item);
        }

    }
}