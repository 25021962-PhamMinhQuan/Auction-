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
import javafx.scene.control.Alert.AlertType;
import org.example.server.AuctionClient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private PasswordField passwordfield, cfpasswordfield;
    @FXML private TextField     passTextfield, cfpassTextfield, usernameTextField;
    @FXML private ToggleButton  showPasswordbtn, showcfPasswordbtn;
    @FXML private Label         warning;
    @FXML private StackPane     spane;
    @FXML private ToggleGroup   role;
    @FXML private RadioButton   bidderRadio, sellerRadio;
    @FXML private ImageView     imageview1, imageview2;
    @FXML private Button        registerBtn;

    private Image eyeclosed;
    private Image eyeopen;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        eyeclosed  = new Image(getClass().getResourceAsStream("/close.png"));
        eyeopen    = new Image(getClass().getResourceAsStream("/open.png"));
        imageview1 = setupButton(showPasswordbtn);
        imageview2 = setupButton(showcfPasswordbtn);
    }

    // ──────── toggle ẩn/hiện mật khẩu ────────

    private void handleTogglemethod(PasswordField pf, TextField tf, ToggleButton tb, ImageView img) {
        if (tb.isSelected()) {
            tf.setText(pf.getText()); tf.setVisible(true); tf.setManaged(true); pf.setVisible(false); pf.setManaged(false);
            tf.requestFocus(); img.setImage(eyeclosed);
        } else {
            pf.setText(tf.getText()); pf.setVisible(true); pf.setManaged(true); tf.setVisible(false); tf.setManaged(false);
            pf.requestFocus(); img.setImage(eyeopen);
        }
        tb.setGraphic(img);
    }

    private ImageView setupButton(ToggleButton button) {
        ImageView img = new ImageView(eyeopen);
        img.setFitWidth(15); img.setFitHeight(15); img.setPreserveRatio(true);
        button.setGraphic(img);
        ColorAdjust effect = new ColorAdjust();
        effect.setBrightness(0.6); img.setEffect(effect); img.setOpacity(0.8);
        button.setOnMouseEntered(e -> { effect.setBrightness(0.9); img.setOpacity(1);   img.setScaleX(1.1); img.setScaleY(1.1); });
        button.setOnMouseExited (e -> { effect.setBrightness(0.6); img.setOpacity(0.8); img.setScaleX(1);   img.setScaleY(1);   });
        return img;
    }

    @FXML public void handleTogglePass   (ActionEvent e) { handleTogglemethod(passwordfield,  passTextfield,   showPasswordbtn,   imageview1); }
    @FXML public void handleToggleConfirm(ActionEvent e) { handleTogglemethod(cfpasswordfield, cfpassTextfield, showcfPasswordbtn, imageview2); }

    // ──────── chuyển màn hình ────────

    @FXML
    public void gotoLogin() throws IOException {
        Parent root  = FXMLLoader.load(getClass().getResource("/org/example/view/login.fxml"));
        Stage  stage = (Stage) spane.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ──────── xử lý đăng ký ────────

    @FXML
    public void handleRegister(ActionEvent e) {
        String username = usernameTextField.getText().trim();
        String password = passwordfield.isVisible() ? passwordfield.getText() : passTextfield.getText();
        String confirm  = cfpasswordfield.isVisible() ? cfpasswordfield.getText() : cfpassTextfield.getText();
        String roleStr  = (sellerRadio != null && sellerRadio.isSelected()) ? "SELLER" : "BIDDER";

        // FIX 1: UI-level check — KHÔNG disable button ở đây, chỉ hiện lỗi và return
        // Button chỉ bị disable khi thực sự gửi lên server
        if (username.isEmpty()) { warning.setText("Username is required"); return; }
        if (password.isEmpty()) { warning.setText("Password is required"); return; }
        if (!password.equals(confirm)) { warning.setText("Mật khẩu xác nhận không khớp"); return; }

        // Từ đây mới disable button vì sắp gửi request
        warning.setText("");
        registerBtn.setDisable(true);

        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

        // FIX 2: connect tới server trước khi gửi REGISTER
        // (LoginController gọi connect() nhưng nếu user vào thẳng Register thì chưa connect)
        try {
            AuctionClient.getInstance().connect(stage);
        } catch (IOException ex) {
            warning.setText("Không thể kết nối server. Vui lòng thử lại.");
            registerBtn.setDisable(false); // bật lại nếu connect thất bại
            return;
        }

        // Đăng ký callback nhận phản hồi REGISTER_OK / REGISTER_ERROR từ server
        AuctionClient.getInstance().setRegisterCallback((success, message) ->
                Platform.runLater(() -> {
                    registerBtn.setDisable(false); // luôn bật lại sau khi nhận phản hồi
                    if (success) {
                        showSuccessAlert();
                        try { gotoLogin(); } catch (IOException ex) { ex.printStackTrace(); }
                    } else {
                        warning.setText(message); // lỗi từ UserService: username tồn tại, pass yếu...
                    }
                })
        );

        AuctionClient.getInstance().register(username, password, roleStr);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText("Đăng ký thành công!");
        alert.setContentText("Bạn có thể đăng nhập ngay bây giờ.");
        alert.showAndWait();
    }
}