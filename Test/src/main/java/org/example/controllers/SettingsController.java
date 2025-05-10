package org.example.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.core.ButtonSoundHelper;
import org.example.scenes.MainMenuScene;

public class SettingsController {

    @FXML private StackPane root;
    @FXML private Rectangle overlay;
    @FXML private Text title;
    @FXML private Slider musicVolumeSlider;
    @FXML private Slider soundEffectsVolumeSlider;
    @FXML private Label musicVolumeLabel;
    @FXML private Label soundEffectsVolumeLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private double initialMusicVolume;
    private double initialSoundVolume;
    private MainMenuController mainMenuController;

    @FXML
    private void initialize() {
        // Store initial values
        initialMusicVolume = FXGL.getSettings().getGlobalMusicVolume();
        initialSoundVolume = FXGL.getSettings().getGlobalSoundVolume();
        
        // Set initial slider values
        musicVolumeSlider.setValue(initialMusicVolume);
        soundEffectsVolumeSlider.setValue(initialSoundVolume);
        
        // Update labels with percentage values
        updateMusicVolumeLabel(initialMusicVolume);
        updateSoundVolumeLabel(initialSoundVolume);
        
        // Add listeners to sliders for real-time updates
        musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = newVal.doubleValue();
            
            // First update the FXGL global setting
            FXGL.getSettings().setGlobalMusicVolume(value);
            updateMusicVolumeLabel(value);
            
            // Then directly update the main menu music for immediate feedback
            if (mainMenuController != null) {
                mainMenuController.updateMenuMusicVolume(value);
            }
        });
        
        soundEffectsVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double value = newVal.doubleValue();
            FXGL.getSettings().setGlobalSoundVolume(value);
            updateSoundVolumeLabel(value);
        });
        
        // Add hover effects to buttons
        addHoverEffect(saveButton);
        addHoverEffect(cancelButton);
        
        // Add button click sounds
        ButtonSoundHelper.addClickSoundToAll(saveButton, cancelButton);
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
    private void saveSettings() {
        // Settings are already applied in real-time through the sliders
        // Just need to close the settings menu
        closeSettings();
    }
    
    @FXML
    private void cancel() {
        // Restore original values
        FXGL.getSettings().setGlobalMusicVolume(initialMusicVolume);
        FXGL.getSettings().setGlobalSoundVolume(initialSoundVolume);
        
        // Also directly update the main menu music
        if (mainMenuController != null) {
            mainMenuController.updateMenuMusicVolume(initialMusicVolume);
        }
        
        closeSettings();
    }
    
    private void closeSettings() {
        // Close the settings scene
        FXGL.getSceneService().popSubScene();
    }
    
    public void setMainMenuController(MainMenuController controller) {
        this.mainMenuController = controller;
    }
} 