package org.example.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LoginController.class.getResource("/org/example/view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Image icon = new Image("/icon.png");
        stage.getIcons().add(icon);
        stage.setTitle("Auction App");
        stage.setScene(scene);
        stage.show();
    }
}
