package org.example.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.core.GameApp;
import org.example.scenes.LeaderboardUI;
import org.example.scenes.LoginScene;
import org.example.scenes.MainMenuScene;

public class MainMenuController {
    private boolean isLeaderboardOpen = false;

    @FXML private StackPane root;
    @FXML private MediaView backgroundMediaView;
    @FXML private Rectangle overlay;
    @FXML private VBox menuBox;
    @FXML private Text title;
    @FXML private Button startButton;
    @FXML private Button settingsButton;
    @FXML private Button leaderboardButton;
    @FXML private Button exitButton;
    @FXML private Button logoutButton;

    private MediaPlayer mediaPlayer;
    private AudioClip menuMusic;

    @FXML
    private void initialize() {
        // Add hover effects to buttons
        addHoverEffect(startButton);
        addHoverEffect(settingsButton);
        addHoverEffect(leaderboardButton);
        addHoverEffect(exitButton);
        addHoverEffect(logoutButton);

        // Start the media
        startMedia();
    }

    private void startMedia() {
        // Stop any existing media to prevent overlap
        stopMedia();

        // Set up the video background
        try {
            System.out.println("Attempting to load video for MainMenuScene...");
            java.net.URL videoUrl = getClass().getResource("/assets/images/mainmenubg_placeholder.mp4");
            if (videoUrl == null) {
                throw new IllegalStateException("Video file not found at /assets/images/mainmenubg_placeholder.mp4.");
            }

            String videoPath = videoUrl.toExternalForm();
            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            backgroundMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();

            mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("MainMenuScene MediaPlayer status: " + newValue);
                if (newValue == MediaPlayer.Status.HALTED) {
                    System.out.println("MainMenuScene MediaPlayer error: " + mediaPlayer.getError());
                }
            });

            System.out.println("MainMenuScene background video loaded and playing successfully");
        } catch (Exception e) {
            System.err.println("Error loading MainMenuScene video: " + e.getMessage());
            e.printStackTrace();
            root.setStyle("-fx-background-color: black;");
        }

        // Load background music
        try {
            java.net.URL musicUrl = getClass().getResource("/assets/music/music2.mp3");
            if (musicUrl == null) {
                throw new IllegalStateException("Menu music file not found at /assets/music/music2.mp3.");
            }
            menuMusic = new AudioClip(musicUrl.toExternalForm());
            menuMusic.setCycleCount(AudioClip.INDEFINITE);
            menuMusic.setVolume(0.5);
            menuMusic.play();
            System.out.println("Menu music loaded and playing successfully");
        } catch (Exception e) {
            System.err.println("Error loading menu music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onSceneShown() {
        System.out.println("MainMenuScene shown - restarting media");
        startMedia();
    }

    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 24; -fx-background-color: #18b7e7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 24; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;"));
    }

    @FXML
    private void startGame() {
        System.out.println("Starting new game...");
        stopMedia();
        FXGL.getGameController().startNewGame();
    }

    @FXML
    private void showSettings() {
        System.out.println("Settings button clicked - placeholder action");
        FXGL.getDialogService().showMessageBox("Settings menu not yet implemented.");
    }

    @FXML
    private void showLeaderboard() {
        if (isLeaderboardOpen) {
            System.out.println("Leaderboard dialog already open - ignoring request");
            return;
        }
        System.out.println("Opening leaderboard dialog...");
        String currentUsername = GameApp.getStoredPlayerName();
        System.out.println("Passing currentUsername to LeaderboardUI: " + currentUsername);
        LeaderboardUI leaderboardUI = new LeaderboardUI(currentUsername);
        isLeaderboardOpen = true;
        FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
        leaderboardUI.getCloseButton().setOnAction(e -> {
            isLeaderboardOpen = false;
            System.out.println("Leaderboard dialog closed");
            // Workaround: Refresh the scene to clear the overlay
            FXGL.getSceneService().popSubScene();
            FXGL.getSceneService().pushSubScene(new MainMenuScene());
        });
    }

    @FXML
    private void exitGame() {
        System.out.println("Exiting game...");
        stopMedia();
        FXGL.getGameController().exit();
    }

    @FXML
    private void logout() {
        System.out.println("Logging out...");
        stopMedia();
        FXGL.getSceneService().popSubScene();
        FXGL.getSceneService().pushSubScene(new LoginScene());
        GameApp gameApp = (GameApp) FXGL.getAppCast();
        gameApp.setLoggedIn(false);
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("MainMenuScene background video stopped");
        }
        if (menuMusic != null) {
            menuMusic.stop();
            System.out.println("Menu music stopped");
        }
    }
}