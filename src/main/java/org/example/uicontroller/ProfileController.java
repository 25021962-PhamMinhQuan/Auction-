package org.example.uicontroller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

// Giả định các class Model/Service của bạn nằm ở các package này
import org.example.domain.user.User;
import org.example.service.UserService;
import org.example.factory.ServiceFactory;
import org.example.util.SupabaseStorage; // Hoặc package chứa tiện ích upload của bạn

public class ProfileController {

    // ── Các thuộc tính FXML đã đồng bộ với file FXML của bạn ──
    @FXML private ImageView avatarImageView; // Đã đổi từ avatarView sang avatarImageView để khớp FXML
    @FXML private Label avatarPlaceholder;
    @FXML private Button removeAvatarBtn;
    @FXML private Button chooseAvatarBtn;

    @FXML private Label navUsernameLabel;
    @FXML private Label displayUsernameLabel;
    @FXML private Label displayRoleLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private StackPane statusPane;
    @FXML private Label statusLabel;

    // ── Các thuộc tính nghiệp vụ ──
    private User currentUser;
    private final UserService userService = ServiceFactory.getInstance().getUserService();

    /**
     * Khởi tạo dữ liệu người dùng lên giao diện
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Cập nhật thông tin Navbar và Header Card
        navUsernameLabel.setText(user.getUsername());
        displayUsernameLabel.setText(user.getUsername());
        displayRoleLabel.setText(user.getRole());

        // Cập nhật thông tin các trường Form
        fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        emailField.setText(user.getEmail() != null ? user.getEmail() : "");
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");

        // Xử lý hiển thị Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            avatarImageView.setImage(new Image(user.getAvatarUrl(), true)); // true = background load
            avatarImageView.setVisible(true);
            avatarPlaceholder.setVisible(false);
            applyCircleClip(avatarImageView);
            removeAvatarBtn.setVisible(true);
            removeAvatarBtn.setManaged(true);
        } else {
            avatarImageView.setVisible(false);
            avatarPlaceholder.setVisible(true);
            removeAvatarBtn.setVisible(false);
            removeAvatarBtn.setManaged(false);
        }
    }

    /**
     * Hành động: Chọn ảnh đại diện mới và upload lên Supabase
     */
    @FXML
    private void handleChooseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(avatarImageView.getScene().getWindow());
        if (file == null) return;

        // Upload lên Supabase (chạy thread riêng tránh block UI)
        // Mở crop dialog trước khi upload
        Image rawImage = new Image(file.toURI().toString());
        Stage ownerStage = (Stage) avatarImageView.getScene().getWindow();

        AvatarCropDialog.show(ownerStage, rawImage, croppedImage -> {
            if (croppedImage == null) return; // user nhấn Huỷ

            // Preview ngay lên UI
            avatarImageView.setImage(croppedImage);
            avatarImageView.setVisible(true);
            avatarPlaceholder.setVisible(false);
            applyCircleClip(avatarImageView);
            removeAvatarBtn.setVisible(true);
            removeAvatarBtn.setManaged(true);

            // Export ra file tạm rồi upload
            new Thread(() -> {
                try {
                    File tmp = java.nio.file.Files.createTempFile("avatar_", ".png").toFile();
                    javax.imageio.ImageIO.write(
                            javafx.embed.swing.SwingFXUtils.fromFXImage(croppedImage, null),
                            "png", tmp
                    );
                    String url = SupabaseStorage.uploadAvatar(tmp, currentUser.getId());
                    tmp.delete();

                    Platform.runLater(() -> {
                        if (url != null) {
                            currentUser.setAvatarUrl(url);
                            showStatus("Ảnh đã tải lên. Nhấn Lưu thay đổi để xác nhận.", true);
                        } else {
                            showStatus("Tải ảnh thất bại.", false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showStatus("Lỗi: " + e.getMessage(), false));
                }
            }).start();
        });
    }

    /**
     * Hành động: Xóa ảnh đại diện tạm thời
     */
    @FXML
    private void handleRemoveAvatar() {
        avatarImageView.setVisible(false);
        avatarPlaceholder.setVisible(true);
        currentUser.setAvatarUrl(null);
        removeAvatarBtn.setVisible(false);
        removeAvatarBtn.setManaged(false);
        showStatus("Đã xóa ảnh đại diện tạm thời. Nhấn Lưu thay đổi để xác nhận.", false);
    }

    /**
     * Hành động: Lưu thông tin cá nhân
     */
    @FXML
    private void handleSaveProfile() {
        currentUser.setFullName(fullNameField.getText().trim());
        currentUser.setEmail(emailField.getText().trim());
        currentUser.setPhone(phoneField.getText().trim());

        String result = userService.updateProfile(currentUser);
        // Kiểm tra kết quả trả về từ service để gán trạng thái màu sắc chuẩn
        boolean isSuccess = result.toLowerCase().contains("thành công");
        showStatus(result, isSuccess);
        if (isSuccess && MainScreenController.getInstance() != null) {
            MainScreenController.getInstance().setCurrentUser(currentUser);
        }
    }

    /**
     * Hành động: Thay đổi mật khẩu
     */
    @FXML
    private void handleChangePassword() {
        String oldPw  = oldPasswordField.getText();
        String newPw  = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (newPw.isEmpty() || oldPw.isEmpty() || confirm.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ các trường mật khẩu", false);
            return;
        }

        if (!newPw.equals(confirm)) {
            showStatus("Mật khẩu xác nhận không khớp", false);
            return;
        }

        String result = userService.changePassword(currentUser, oldPw, newPw);
        boolean isSuccess = result.equals("Đổi mật khẩu thành công");
        showStatus(result, isSuccess);

        if (isSuccess) {
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        }
    }

    /**
     * Hành động: Quay lại trang trước đó
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/view/mainscreen.fxml"));
            Parent root = loader.load();

            MainScreenController controller = loader.getController();
            MainScreenController.setInstance(controller);
            controller.setCurrentUser(
                    currentUser.getUsername(),
                    currentUser.getRole()
            );

            Stage stage = (Stage) navUsernameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Phương thức bổ trợ: Hiển thị thanh thông báo trạng thái đẹp mắt
     */
    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusPane.setVisible(true);
        statusPane.setManaged(true);

        String color = isSuccess ? "rgba(34,197,94,0.12)" : "rgba(239,68,68,0.12)";
        String borderColor = isSuccess ? "rgba(34,197,94,0.3)" : "rgba(239,68,68,0.3)";
        String textColor = isSuccess ? "#4ade80" : "#f87171";

        statusPane.setStyle("-fx-background-color:" + color + "; -fx-background-radius:10;"
                + "-fx-border-color:" + borderColor + "; -fx-border-radius:10;"
                + "-fx-border-width:1; -fx-padding:12 16;");
        statusLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + textColor + ";");
    }

    private void applyCircleClip(ImageView iv) {
        double cx = iv.getFitWidth()  / 2.0;
        double cy = iv.getFitHeight() / 2.0;
        double r  = Math.min(cx, cy);
        iv.setClip(new javafx.scene.shape.Circle(cx, cy, r));
    }
}