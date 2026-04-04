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

public class RegisterController implements Initializable {
    @FXML
    private PasswordField passwordfield, cfpasswordfield;

    @FXML
    private TextField passTextfield, cfpassTextfield ;

    @FXML
    private ToggleButton showPasswordbtn, showcfPasswordbtn;

    @FXML
    private MediaView mvVideo;

    @FXML
    private StackPane spane;
    @FXML
    ImageView imageview1, imageview2;

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
            imageview1 = new ImageView();
            imageview2 = new ImageView();
            imageview1.setFitWidth(15);
            imageview1.setFitHeight(15);
            imageview2.setFitHeight(15);
            imageview2.setFitWidth(15);
            imageview1.setImage(eyeopen);
            showPasswordbtn.setGraphic(imageview1);
            imageview2.setImage(eyeopen);
            showcfPasswordbtn.setGraphic(imageview2);

    }
    private void handleTogglemethod(PasswordField pf, TextField tf, ToggleButton tb, ImageView img){
        if (tb.isSelected()){
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
    @FXML
    public void handleTogglePass(ActionEvent e){
        handleTogglemethod(passwordfield, passTextfield, showPasswordbtn, imageview1);
    }
    @FXML
    public void handleToggleConfirm(ActionEvent e){
        handleTogglemethod(cfpasswordfield, cfpassTextfield, showcfPasswordbtn, imageview2);
    }
    @FXML
    public void gotoLogin(ActionEvent e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/org/example/view/login.fxml"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}