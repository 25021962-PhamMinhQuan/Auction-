package org.example.uicontroller;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import org.example.controller.AuthController;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private PasswordField passwordfield;

    @FXML
    private TextField passTextfield, usernameTextField;
    @FXML
    private Label warning;
    @FXML
    private ToggleButton showPasswordbtn;

    @FXML
    private StackPane spane;
    @FXML
    ImageView imageview;

    private Image eyeclosed, eyeopen;
    AuthController auth = new AuthController();


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        eyeclosed = new Image(getClass().getResourceAsStream("/close.png"));
        eyeopen = new Image(getClass().getResourceAsStream("/open.png"));
        imageview = new ImageView();
        imageview.setImage(eyeopen);
        imageview.setFitWidth(15);
        imageview.setFitHeight(15);
        imageview.setPreserveRatio(true);
        showPasswordbtn.setGraphic(imageview);
        ColorAdjust effect = new ColorAdjust();
        effect.setBrightness(0.6);
        imageview.setEffect(effect);
        imageview.setOpacity(0.8);
        showPasswordbtn.setOnMouseEntered(e -> {
            effect.setBrightness(0.9);
            imageview.setOpacity(1);
            imageview.setScaleX(1.1);
            imageview.setScaleY(1.1);
        });
        showPasswordbtn.setOnMouseExited(e -> {
            effect.setBrightness(0.6);
            imageview.setOpacity(0.8);
            imageview.setScaleX(1);
            imageview.setScaleY(1);
        });
    }


    @FXML
    public void handleToggle(ActionEvent e) {
        if (showPasswordbtn.isSelected()) {
            passTextfield.setText(passwordfield.getText());
            passTextfield.setVisible(true);
            passwordfield.setVisible(false);
            passTextfield.requestFocus();
            imageview.setImage(eyeclosed);
            showPasswordbtn.setGraphic(imageview);

        } else {
            passwordfield.setText(passTextfield.getText());
            passwordfield.setVisible(true);
            passTextfield.setVisible(false);
            passwordfield.requestFocus();
            imageview.setImage(eyeopen);
            showPasswordbtn.setGraphic(imageview);
        }

    }

    @FXML
    public void gotoRegister(ActionEvent e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/org/example/view/register.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void handleLogin(ActionEvent e) {
        if (usernameTextField.getText().isEmpty()) {
            warning.setText("Username is required");
        } else if (passwordfield.getText().isEmpty()) {
            warning.setText("Password is required");
        } else {
            try {
                // Kết nối server lần đầu khi login
                Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                AuctionClient.getInstance().connect(stage);

                // Gửi lệnh LOGIN lên server
                AuctionClient.getInstance().login(
                        usernameTextField.getText(),
                        passwordfield.getText()
                );
            } catch (IOException ex) {
                warning.setText(ex.getMessage());
            }
        }

    }
}
