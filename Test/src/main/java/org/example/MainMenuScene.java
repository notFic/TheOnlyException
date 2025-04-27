package org.example;

import com.almasb.fxgl.app.scene.GameSubScene;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class MainMenuScene extends GameSubScene {
    private MainMenuController controller;

    public MainMenuScene() {
        super(FXGL.getAppWidth(), FXGL.getAppHeight());
        System.out.println("Constructing MainMenuScene...");

        try {
            System.out.println("Attempting to load MainMenuScene.fxml from path: /fxml/MainMenuScene.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenuScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("MainMenuScene.fxml not found at /fxml/MainMenuScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Pane mainMenuPane = loader.load();
            controller = loader.getController();
            System.out.println("MainMenuScene.fxml loaded successfully");

            getContentRoot().getChildren().add(mainMenuPane);
            System.out.println("MainMenuScene constructed and added to content root");
        } catch (Exception e) {
            System.err.println("Error loading MainMenuScene UI: " + e.getMessage());
            e.printStackTrace();
            FXGL.getDialogService().showMessageBox("Failed to load MainMenuScene UI: " + e.getMessage(), () -> {
                System.exit(0);
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (controller != null) {
            controller.onSceneShown();
        }
    }
}