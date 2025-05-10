package org.example.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.core.GameApp;
import org.example.scenes.MainMenuScene;

public class PauseController {

    @FXML private StackPane root;
    @FXML private Rectangle overlay;
    @FXML private Text title;
    @FXML private Slider musicVolumeSlider;
    @FXML private Slider soundEffectsVolumeSlider;
    @FXML private Label musicVolumeLabel;
    @FXML private Label soundEffectsVolumeLabel;
    @FXML private Button resumeButton;
    @FXML private Button mainMenuButton;

    private GameApp gameApp;

    @FXML
    private void initialize() {
        // Set initial slider values
        double musicVolume = FXGL.getSettings().getGlobalMusicVolume();
        double soundVolume = FXGL.getSettings().getGlobalSoundVolume();
        
        musicVolumeSlider.setValue(musicVolume);
        soundEffectsVolumeSlider.setValue(soundVolume);
        
        // Update labels with percentage values
        updateMusicVolumeLabel(musicVolume);
        updateSoundVolumeLabel(soundVolume);
        
        // Add listeners to sliders for real-time updates
        musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = newVal.doubleValue();
            FXGL.getSettings().setGlobalMusicVolume(value);
            updateMusicVolumeLabel(value);
        });
        
        soundEffectsVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = newVal.doubleValue();
            FXGL.getSettings().setGlobalSoundVolume(value);
            updateSoundVolumeLabel(value);
        });
        
        // Add hover effects to buttons
        addHoverEffect(resumeButton);
        addHoverEffect(mainMenuButton);
    }
    
    private void updateMusicVolumeLabel(double value) {
        int percentage = (int) (value * 100);
        musicVolumeLabel.setText(percentage + "%");
    }
    
    private void updateSoundVolumeLabel(double value) {
        int percentage = (int) (value * 100);
        soundEffectsVolumeLabel.setText(percentage + "%");
    }
    
    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 18; -fx-background-color: #18b7e7; -fx-text-fill: white; -fx-font-weight: bold;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 18; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold;"));
    }
    
    @FXML
    private void resumeGame() {
        // Close the pause menu and resume the game
        FXGL.getSceneService().popSubScene();
        if (gameApp != null) {
            gameApp.resumeGameTimers();
        }
    }
    
    @FXML
    private void returnToMainMenu() {
        // Stop game timers and return to main menu
        if (gameApp != null) {
            FXGL.getSceneService().popSubScene(); // Remove the pause menu
            gameApp.gotoNewMainMenu();
        }
    }
    
    public void setGameApp(GameApp gameApp) {
        this.gameApp = gameApp;
    }
} 