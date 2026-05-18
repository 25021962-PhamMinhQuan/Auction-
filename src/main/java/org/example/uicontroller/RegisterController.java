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
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import org.example.controller.AuthController;
import org.example.domain.user.Bidder;
import org.example.domain.user.Seller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    @FXML
    private PasswordField passwordfield, cfpasswordfield;

    @FXML
    private TextField passTextfield, cfpassTextfield, usernameTextField;

    @FXML
    private ToggleButton showPasswordbtn, showcfPasswordbtn;

    @FXML
    private Label warning;

    @FXML
    private StackPane spane;
    @FXML
    private ToggleGroup role;
    @FXML
    private RadioButton bidderRadio, sellerRadio;
    @FXML
    ImageView imageview1, imageview2;

    private Image eyeclosed;
    private Image eyeopen;
    AuthController auth = new AuthController();


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        eyeclosed = new Image(getClass().getResourceAsStream("/close.png"));
        eyeopen = new Image(getClass().getResourceAsStream("/open.png"));
        imageview1 = setupButton(showPasswordbtn);
        imageview2 = setupButton(showcfPasswordbtn);
    }

    //hàm xử lí ẩn hiện mật khẩu
    private void handleTogglemethod(PasswordField pf, TextField tf, ToggleButton tb, ImageView img) {
        if (tb.isSelected()) {
            tf.setText(pf.getText());
            tf.setVisible(true);
            pf.setVisible(false);
            tf.requestFocus();
            img.setImage(eyeclosed);
            tb.setGraphic(img);

        } else {
            pf.setText(tf.getText());
            pf.setVisible(true);
            tf.setVisible(false);
            pf.requestFocus();
            img.setImage(eyeopen);
            tb.setGraphic(img);
        }

    }

    //hàm xử lí hiệu ứng nút ẩn hiện mật khẩu
    private ImageView setupButton(ToggleButton button) {
        ImageView img = new ImageView(eyeopen);
        img.setFitWidth(15);
        img.setFitHeight(15);
        img.setPreserveRatio(true);

        button.setGraphic(img);

        ColorAdjust effect = new ColorAdjust();
        effect.setBrightness(0.6);
        img.setEffect(effect);
        img.setOpacity(0.8);

        button.setOnMouseEntered(e -> {
            effect.setBrightness(0.9);
            img.setOpacity(1);
            img.setScaleX(1.1);
            img.setScaleY(1.1);
        });

        button.setOnMouseExited(e -> {
            effect.setBrightness(0.6);
            img.setOpacity(0.8);
            img.setScaleX(1);
            img.setScaleY(1);
        });
        return img;
    }

    public String getRole() {
        if (bidderRadio.isSelected()) {
            return "BIDDER";
        } else {
            return "SELLER";
        }
    }

    @FXML
    public void handleTogglePass(ActionEvent e) {
        handleTogglemethod(passwordfield, passTextfield, showPasswordbtn, imageview1);
    }

    @FXML
    public void handleToggleConfirm(ActionEvent e) {
        handleTogglemethod(cfpasswordfield, cfpassTextfield, showcfPasswordbtn, imageview2);
    }

    //hàm chuyển sang scene đăng nhập
    @FXML
    public void gotoLogin(ActionEvent e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/org/example/view/login.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    //thông báo khi đăng nhập thành công
    private void notif() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Attenion!");
        alert.setHeaderText("Registered successfully!");
        alert.setContentText("You can now login!");
        alert.showAndWait();
    }

    @FXML
    public void handleRegister(ActionEvent e) throws IOException {
        String username = usernameTextField.getText();
        // lấy password từ field đang visible
        String password = passwordfield.isVisible() ? passwordfield.getText() : passTextfield.getText();
        String confirm = cfpasswordfield.isVisible() ? cfpasswordfield.getText() : cfpassTextfield.getText();

        //check input tên đăng nhập
        if (username.isEmpty()) {
            warning.setText("Username is required"); //hiển thị lỗi
            return;
        }
        //tương tự
        if (password.isEmpty()) {
            warning.setText("Password is required");
            return;
        }
        //check xác nhận mật khẩu
        if (!password.equals(cfpasswordfield.getText())) {
            warning.setText("Passwords do not match");
            return;
        } else {
            String roleType = getRole();
            String result;

            if ("BIDDER".equals(roleType)) {
                result = auth.register(new Bidder(username, password));
            } else {
                result = auth.register(new Seller(username, password));
            }
            if ("Register success".equals(result)) {
                notif();
                gotoLogin(e);
            } else {
                warning.setText(result);
            }
        }
        //chuyển sang đăng nhập
    }
}