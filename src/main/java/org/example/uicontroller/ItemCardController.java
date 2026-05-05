package org.example.uicontroller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.model.item.Item;

public class ItemCardController {
    @FXML
    private Label itemname;
    @FXML
    private Label price;
    @FXML
    private Label timeopen;

    public void setData(Item item) {
        itemname.setText(item.getName());
        price.setText(String.valueOf(item.getCurrentPrice()));
        timeopen.setText("10.AM");
    }

    public void setData(String name, String gia, String opentime) {
        itemname.setText(name);
        price.setText(gia);
        timeopen.setText(opentime);
    }

}