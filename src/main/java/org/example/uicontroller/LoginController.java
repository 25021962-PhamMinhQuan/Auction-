package org.example.uicontroller;

import javafx.application.Platform;
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
import org.example.server.AuctionClient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private PasswordField passwordfield;
    @FXML private TextField     passTextfield, usernameTextField;
    @FXML private Label         warning;
    @FXML private ToggleButton  showPasswordbtn;
    @FXML private StackPane     spane;
    @FXML        ImageView      imageview;

    private Image eyeclosed, eyeopen;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        eyeclosed = new Image(getClass().getResourceAsStream("/close.png"));
        eyeopen   = new Image(getClass().getResourceAsStream("/open.png"));

        imageview = new ImageView(eyeopen);
        imageview.setFitWidth(15);
        imageview.setFitHeight(15);
        imageview.setPreserveRatio(true);
        showPasswordbtn.setGraphic(imageview);

        ColorAdjust effect = new ColorAdjust();
        effect.setBrightness(0.6);
        imageview.setEffect(effect);
        imageview.setOpacity(0.8);

        showPasswordbtn.setOnMouseEntered(e -> { effect.setBrightness(0.9); imageview.setOpacity(1);   imageview.setScaleX(1.1); imageview.setScaleY(1.1); });
        showPasswordbtn.setOnMouseExited (e -> { effect.setBrightness(0.6); imageview.setOpacity(0.8); imageview.setScaleX(1);   imageview.setScaleY(1);   });
    }

    @FXML
    public void handleToggle(ActionEvent e) {
        if (showPasswordbtn.isSelected()) {
            passTextfield.setText(passwordfield.getText());
            passTextfield.setVisible(true);
            passwordfield.setVisible(false);
            passTextfield.requestFocus();
            imageview.setImage(eyeclosed);
        } else {
            passwordfield.setText(passTextfield.getText());
            passwordfield.setVisible(true);
            passTextfield.setVisible(false);
            passwordfield.requestFocus();
            imageview.setImage(eyeopen);
        }
        showPasswordbtn.setGraphic(imageview);
    }

    @FXML
    public void gotoRegister(ActionEvent e) throws IOException {
        Parent root  = FXMLLoader.load(getClass().getResource("/org/example/view/register.fxml"));
        Stage  stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void handleLogin(ActionEvent e) {
        String username = usernameTextField.getText().trim();
        String password = passwordfield.isVisible() ? passwordfield.getText() : passTextfield.getText();

        if (username.isEmpty()) { warning.setText("Username is required"); return; }
        if (password.isEmpty()) { warning.setText("Password is required"); return; }

        Button loginBtn = (Button) e.getSource();
        // FIX: lưu đúng stage gốc (primaryStage từ AuctionApplication)
        Stage  stage    = (Stage) loginBtn.getScene().getWindow();

        loginBtn.setDisable(true);
        warning.setText("");

        try {
            AuctionClient client = AuctionClient.getInstance();
            // Truyền stage vào để AuctionClient biết dùng stage nào
            client.connect(stage);

            client.setLoginCallback((success, message) ->
                    Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        if (success) {
                            // FIX: chuyển scene ngay tại đây trên đúng stage gốc
                            // thay vì để AuctionClient.openMainScreen() tự mở stage mới
                            openMainScreen(stage, client.getCurrentUsername(), client.getCurrentRole());
                        } else {
                            warning.setText(message);
                        }
                    })
            );

            client.login(username, password);

        } catch (IOException ex) {
            loginBtn.setDisable(false);
            warning.setText("Không thể kết nối server. Vui lòng thử lại.");
        }
    }

    private void openMainScreen(Stage stage, String username, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/view/mainscreen.fxml"));
            Parent root = loader.load();

            MainScreenController controller = loader.getController();
            MainScreenController.setInstance(controller);
            controller.setCurrentUser(username, role);

            // setScene trên stage gốc → không mở cửa sổ mới
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            warning.setText("Không thể mở màn hình chính.");
        }
    }
}