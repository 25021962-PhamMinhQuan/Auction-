package org.example.controller;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.Media;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private PasswordField passwordfield;

    @FXML
    private TextField passTextfield;

    @FXML
    private ToggleButton showPasswordbtn;

    @FXML
    private MediaView mvVideo;

    @FXML
    private StackPane spane;
    @FXML
    ImageView imageview;

    private File file;
    private Media media;
    private MediaPlayer mediaPlayer;
    private Image eyeclosed;
    private Image eyeopen;


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        URL vidURL = getClass().getResource("/background.mp4");
        media = new Media(vidURL.toExternalForm());
        mediaPlayer = new MediaPlayer(media);
        mvVideo.setMediaPlayer(mediaPlayer);
        mvVideo.fitWidthProperty().bind(spane.widthProperty());
        mvVideo.fitHeightProperty().bind(spane.heightProperty());
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        eyeclosed = new Image(getClass().getResourceAsStream("/close.png"));
        eyeopen = new Image(getClass().getResourceAsStream("/open.png"));
        imageview = new ImageView();
        imageview.setImage(eyeopen);
        imageview.setFitWidth(15);
        imageview.setFitHeight(15);
        imageview.setPreserveRatio(true);
        showPasswordbtn.setGraphic(imageview);
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
}