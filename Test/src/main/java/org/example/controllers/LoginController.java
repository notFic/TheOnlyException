package org.example.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.example.core.GameApp;
import org.example.utils.DatabaseManager;
import org.example.utils.ResourceLoader;
import org.example.utils.UIAnimations;

import java.net.URL;
import java.sql.SQLException;

public class LoginController {
    private Runnable loginSuccessCallback;
    private Runnable switchToRegisterCallback;

    @FXML private Label titleLabel;
    @FXML private Label errorLabel;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private MediaView backgroundMediaView;

    private MediaPlayer mediaPlayer;

    @FXML
    private void initialize() {
        initializeBackgroundVideo();
        errorLabel.setText("");
        // Add listener to load stylesheet when scene is available
        backgroundMediaView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getStylesheets().add(ResourceLoader.getResource("/css/styles.css").toExternalForm());
                System.out.println("Stylesheet loaded successfully: /css/styles.css");
            }
        });
    }

    private void initializeBackgroundVideo() {
        try {
            System.out.println("Loading video from path: /assets/images/placeHolderVidBg.mp4");
            URL videoUrl = ResourceLoader.getResource("/assets/images/placeHolderVidBg.mp4");
            String videoPath = videoUrl.toExternalForm();
            System.out.println("Video path resolved to: " + videoPath);
            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setMute(true);
            mediaPlayer.play();

            mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("MediaPlayer status: " + newValue);
                if (newValue == MediaPlayer.Status.HALTED) {
                    System.out.println("MediaPlayer error: " + mediaPlayer.getError());
                }
            });

            UIAnimations.fadeIn(backgroundMediaView.getParent(), 800);
        } catch (Exception e) {
            handleMediaLoadError(e);
        }
    }

    private void handleMediaLoadError(Exception e) {
        e.printStackTrace();
        System.out.println("Failed to load background video: " + e.getMessage());
        if (backgroundMediaView.getParent() != null) {
            backgroundMediaView.getParent().setStyle("-fx-background-color: black;");
        }
    }

    public void setLoginSuccessCallback(Runnable callback) {
        this.loginSuccessCallback = callback;
    }

    public void setSwitchToRegisterCallback(Runnable callback) {
        this.switchToRegisterCallback = callback;
    }

    @FXML
    private void handleLogin() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Username or password cannot be empty!");
            return;
        }

        try {
            boolean loginSuccess = DatabaseManager.validateUser(user, pass);

            if (loginSuccess) {
                handleSuccessfulLogin(user);
            } else {
                showError("Incorrect username or password. Please try again.");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSuccessfulLogin(String username) {
        FXGL.getWorldProperties().setValue("playerName", username);
        GameApp.startGameWithName(username);

        GameApp gameApp = (GameApp) FXGL.getAppCast();
        gameApp.setLoggedIn(true);

        System.out.println("Login successful for user: " + username);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        gameApp.gotoNewMainMenu();

        if (loginSuccessCallback != null) {
            loginSuccessCallback.run();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        UIAnimations.shakeNode(errorLabel);
    }

    @FXML
    private void handleRegister() {
        if (switchToRegisterCallback != null) {
            switchToRegisterCallback.run();
        }
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}