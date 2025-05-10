package org.example.scenes;

import com.almasb.fxgl.app.scene.GameSubScene;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.example.controllers.PauseController;
import org.example.core.GameApp;

/**
 * Scene displayed when the game is paused with ESC key.
 */
public class PauseScene extends GameSubScene {
    private PauseController controller;
    private GameApp gameApp;

    public PauseScene(GameApp gameApp) {
        super(FXGL.getAppWidth(), FXGL.getAppHeight());
        this.gameApp = gameApp;
        loadPauseUI();
    }

    private void loadPauseUI() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PauseScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("PauseScene.fxml not found at /fxml/PauseScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Parent root = loader.load();

            controller = loader.getController();
            if (controller != null) {
                controller.setGameApp(gameApp);
            }
            getContentRoot().getChildren().add(root);
        } catch (Exception e) {
            System.err.println("Error loading PauseScene UI: " + e.getMessage());
            e.printStackTrace();
            FXGL.getDialogService().showMessageBox("Failed to load PauseScene UI.", () -> {
                // Fallback UI if needed
                FXGL.getSceneService().popSubScene();
                gameApp.resumeGameTimers();
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Pause game timers when this scene is created
        gameApp.pauseGameTimers();
    }
} 