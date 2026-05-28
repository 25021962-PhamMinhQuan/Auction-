package org.example.uicontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import org.example.util.ThemeManager;

import java.io.IOException;

public class AuctionApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LoginController.class.getResource("/org/example/view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ThemeManager.applyTheme(scene);
        Image icon = new Image("/icon.png");
        stage.getIcons().add(icon);
        stage.setTitle("Auction App");


        stage.setWidth(1200);
        stage.setHeight(700);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
