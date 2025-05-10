package org.example.scenes;

import com.almasb.fxgl.app.scene.GameSubScene;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.example.controllers.MainMenuController;
import org.example.controllers.SettingsController;

public class SettingsScene extends GameSubScene {
    private SettingsController controller;
    private MainMenuController mainMenuController;

    public SettingsScene(MainMenuController mainMenuController) {
        super(FXGL.getAppWidth(), FXGL.getAppHeight());
        this.mainMenuController = mainMenuController;
        loadSettingsUI();
    }

    private void loadSettingsUI() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettingsScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("SettingsScene.fxml not found at /fxml/SettingsScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Parent root = loader.load();

            controller = loader.getController();
            if (controller != null) {
                controller.setMainMenuController(mainMenuController);
            }
            getContentRoot().getChildren().add(root);
        } catch (Exception e) {
            System.err.println("Error loading SettingsScene UI: " + e.getMessage());
            e.printStackTrace();
            FXGL.getDialogService().showMessageBox("Failed to load SettingsScene UI.", () -> {
                // Fallback UI if needed
                FXGL.getSceneService().popSubScene();
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
} 