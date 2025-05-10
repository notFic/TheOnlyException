package org.example.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.core.GameApp;
import org.example.scenes.LeaderboardUI;
import org.example.scenes.LoginScene;
import org.example.scenes.MainMenuScene;
import org.example.scenes.SettingsScene;

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

    private MediaPlayer videoPlayer;
    private MediaPlayer musicPlayer;
    private ChangeListener<Number> volumeListener;

    @FXML
    private void initialize() {
        // Add hover effects to buttons
        addHoverEffect(startButton);
        addHoverEffect(settingsButton);
        addHoverEffect(leaderboardButton);
        addHoverEffect(exitButton);
        addHoverEffect(logoutButton);

        // Create volume listener to be used when music player is created
        volumeListener = (obs, oldVal, newVal) -> {
            if (musicPlayer != null && musicPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
                try {
                    double volume = newVal.doubleValue();
                    musicPlayer.setVolume(volume);
                } catch (Exception e) {
                    // Silently ignore any errors when setting volume
                    // This prevents NullPointerException when player is being disposed
                }
            }
        };

        // Start the media
        startMedia();
    }

    private void startMedia() {
        // Stop any existing media to prevent overlap
        stopMedia();

        // Set up the video background
        try {
            java.net.URL videoUrl = getClass().getResource("/assets/images/mainmenubg_placeholder.mp4");
            if (videoUrl == null) {
                throw new IllegalStateException("Video file not found at /assets/images/mainmenubg_placeholder.mp4.");
            }

            String videoPath = videoUrl.toExternalForm();
            Media videoMedia = new Media(videoPath);
            videoPlayer = new MediaPlayer(videoMedia);
            videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            videoPlayer.setMute(true);
            backgroundMediaView.setMediaPlayer(videoPlayer);
            videoPlayer.play();

            videoPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == MediaPlayer.Status.HALTED) {
                    System.err.println("Video player error: " + videoPlayer.getError());
                }
            });
        } catch (Exception e) {
            System.err.println("Error loading video: " + e.getMessage());
            e.printStackTrace();
            root.setStyle("-fx-background-color: black;");
        }

        // Load background music using MediaPlayer
        try {
            java.net.URL musicUrl = getClass().getResource("/assets/music/music2.mp3");
            if (musicUrl == null) {
                throw new IllegalStateException("Menu music file not found at /assets/music/music2.mp3.");
            }
            
            Media musicMedia = new Media(musicUrl.toExternalForm());
            musicPlayer = new MediaPlayer(musicMedia);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            // Use the current global music volume
            double volume = FXGL.getSettings().getGlobalMusicVolume();
            musicPlayer.setVolume(volume);
            musicPlayer.play();
            
            // Add the volume listener AFTER the player is fully initialized
            FXGL.getSettings().globalMusicVolumeProperty().removeListener(volumeListener);
            FXGL.getSettings().globalMusicVolumeProperty().addListener(volumeListener);
            
        } catch (Exception e) {
            System.err.println("Error loading menu music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onSceneShown() {
        startMedia();
    }

    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 24; -fx-background-color: #18b7e7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 24; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;"));
    }

    @FXML
    private void startGame() {
        // Make sure we completely stop the menu music before starting the game
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
            videoPlayer = null;
        }
        // Start the game with a clean audio state
        FXGL.getGameController().startNewGame();
    }

    @FXML
    private void showSettings() {
        // Don't stop the media, let the music continue playing
        FXGL.getSceneService().pushSubScene(new SettingsScene(this));
    }

    @FXML
    private void showLeaderboard() {
        if (isLeaderboardOpen) {
            return;
        }
        
        String currentUsername = GameApp.getStoredPlayerName();
        LeaderboardUI leaderboardUI = new LeaderboardUI(currentUsername);
        isLeaderboardOpen = true;
        FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
        leaderboardUI.getCloseButton().setOnAction(e -> {
            isLeaderboardOpen = false;
            // Refresh the scene to clear the overlay
            FXGL.getSceneService().popSubScene();
            FXGL.getSceneService().pushSubScene(new MainMenuScene());
        });
    }

    @FXML
    private void exitGame() {
        stopMedia();
        FXGL.getGameController().exit();
    }

    @FXML
    private void logout() {
        stopMedia();
        FXGL.getSceneService().popSubScene();
        FXGL.getSceneService().pushSubScene(new LoginScene());
        GameApp gameApp = (GameApp) FXGL.getAppCast();
        gameApp.setLoggedIn(false);
    }

    /**
     * Stops and disposes all media resources.
     * Made public so it can be called during scene transitions.
     */
    public void stopMedia() {
        // Remove the volume listener first to prevent NullPointerException
        FXGL.getSettings().globalMusicVolumeProperty().removeListener(volumeListener);
        
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
                videoPlayer.dispose();
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
            videoPlayer = null;
        }
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
                musicPlayer.dispose();
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
            musicPlayer = null;
        }
    }

    /**
     * Directly updates the volume of the currently playing menu music.
     */
    public void updateMenuMusicVolume(double volume) {
        if (musicPlayer != null && musicPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            try {
                musicPlayer.setVolume(volume);
            } catch (Exception e) {
                // Silently ignore any errors when setting volume
            }
        }
    }
}