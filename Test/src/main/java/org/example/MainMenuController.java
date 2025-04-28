package org.example;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

import java.util.List;

public class MainMenuController {

    @FXML
    private StackPane root;

    @FXML
    private MediaView backgroundMediaView;

    @FXML
    private Rectangle overlay;

    @FXML
    private VBox menuBox;

    @FXML
    private Text title;

    @FXML
    private Button startButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button leaderboardButton;

    @FXML
    private Button exitButton;

    @FXML
    private Button logoutButton;

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

    // Method to restart media when the scene is shown again
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
        System.out.println("Fetching leaderboard data...");
        List<Player> topPlayers = LeaderboardDatabase.getTopPlayers(10);

        StringBuilder leaderboardText = new StringBuilder("Leaderboard\n\n");
        if (topPlayers.isEmpty()) {
            leaderboardText.append("No leaderboard data available.");
        } else {
            for (int i = 0; i < topPlayers.size(); i++) {
                Player player = topPlayers.get(i);
                leaderboardText.append(String.format("Rank %d: %s - Survival Time: %d s, Total Damage: %d\n",
                        player.getRank(), player.getUsername(), player.getBestSurvivalTime(), player.getTotalDamage()));
            }
        }

        TextArea textArea = new TextArea(leaderboardText.toString());
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-size: 16; -fx-font-family: 'Arial';");
        textArea.setPrefSize(600, 400);

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefSize(600, 400);
        HBox dialogBox = new HBox(scrollPane);
        dialogBox.setAlignment(Pos.CENTER);

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-font-size: 16; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        addHoverEffect(closeButton);

        FXGL.getDialogService().showBox("Leaderboard", dialogBox, closeButton);
    }

    // Exit the game
    @FXML
    private void exitGame() {
        System.out.println("Exiting game...");
        stopMedia();
        FXGL.getGameController().exit();
    }

    // Logout and return to login screen
    @FXML
    private void logout() {
        System.out.println("Logging out...");
        stopMedia();
        FXGL.getSceneService().popSubScene();
        NameInputScene nameInputScene = (NameInputScene) FXGL.getSceneService().getCurrentScene();
        nameInputScene.reloadLoginUI();
        // Reset login state
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