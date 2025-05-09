package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.example.core.GameApp; // Add this import
import org.example.utils.DatabaseManager;
import org.example.utils.PasswordUtils;
import org.example.utils.ResourceLoader;
import org.example.utils.UIAnimations;

import java.net.URL;
import java.sql.SQLException;

public class RegisterController {
    private Runnable switchToLoginCallback;
    private Runnable switchToMainMenuCallback;

    @FXML private Label titleLabel;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Button switchButton;
    @FXML private MediaView backgroundMediaView;

    private MediaPlayer mediaPlayer;

    @FXML
    private void initialize() {
        initializeBackgroundVideo();
        errorLabel.setText("");
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

    public void setSwitchToLoginCallback(Runnable callback) {
        this.switchToLoginCallback = callback;
    }

    public void setSwitchToMainMenuCallback(Runnable callback) {
        this.switchToMainMenuCallback = callback;
    }

    @FXML
    private void handleRegister() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Username or password cannot be empty!");
            return;
        }

        if (!PasswordUtils.isStrongPassword(pass)) {
            showError("Password must be at least 8 characters and include numbers and letters!");
            return;
        }

        try {
            if (DatabaseManager.userExists(user)) {
                showError("Username already exists!");
                return;
            }

            boolean registrationSuccess = DatabaseManager.registerUser(user, pass);

            if (registrationSuccess) {
                // Store the username in GameApp
                GameApp.startGameWithName(user);
                handleSuccessfulRegistration();
            } else {
                showError("Registration failed. Try again.");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSuccessfulRegistration() {
        errorLabel.setText("Registration successful! Redirecting to Main Menu...");
        errorLabel.setStyle("-fx-text-fill: #4CAF50;");

        // Stop the media player before transitioning
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(event -> {
            if (switchToMainMenuCallback != null) {
                switchToMainMenuCallback.run();
            } else {
                System.out.println("switchToMainMenuCallback is null, cannot transition to Main Menu");
            }
        });
        pause.play();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        UIAnimations.shakeNode(errorLabel);
    }

    @FXML
    private void switchToLogin() {
        if (switchToLoginCallback != null) {
            switchToLoginCallback.run();
        }
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}