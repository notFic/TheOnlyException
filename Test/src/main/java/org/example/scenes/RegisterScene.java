package org.example.scenes;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import org.example.controllers.RegisterController;

public class RegisterScene extends FXGLMenu {

    private RegisterController controller;

    public RegisterScene() {
        super(MenuType.MAIN_MENU);
        System.out.println("Constructing RegisterScene...");
        loadRegisterUI();
    }

    // Method to set the parent LoginScene
    public void setParentLoginScene(LoginScene loginScene) {
    }

    private void loadRegisterUI() {
        try {
            System.out.println("Attempting to load RegisterScene.fxml from path: /fxml/RegisterScene.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("RegisterScene.fxml not found at /fxml/RegisterScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Pane registerPane = loader.load();
            controller = loader.getController();

            // Set up callback for successful registration
            controller.setSwitchToMainMenuCallback(() -> {
                // Now continue with transition
                System.out.println("Registration successful");
                FXGL.getSceneService().popSubScene();
                FXGL.getGameController().gotoMainMenu();
            });

            registerPane.setTranslateX(FXGL.getAppWidth() / 2.0 - registerPane.getPrefWidth() / 2);
            registerPane.setTranslateY(FXGL.getAppHeight() / 2.0 - registerPane.getPrefHeight() / 2);

            getContentRoot().getChildren().add(registerPane);
            System.out.println("RegisterScene constructed and added to content root");
        } catch (Exception e) {
            System.err.println("Error loading RegisterScene UI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Public method to access the controller
    public RegisterController getRegisterController() {
        return controller;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) {
            controller.cleanup();
        }
        getContentRoot().getChildren().clear();
        System.out.println("RegisterScene content root cleared in onDestroy");
    }
}