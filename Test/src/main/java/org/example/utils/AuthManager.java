package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controllers.LoginController;
import org.example.controllers.RegisterController;

import java.io.IOException;

/**
 * Manages authentication flow between login and registration screens
 */
public class AuthManager {
    private Stage stage;
    private Scene loginScene;
    private Scene registerScene;
    private LoginController loginController;
    private RegisterController registerController;
    private Runnable onLoginSuccess;

    public AuthManager(Stage stage, Runnable onLoginSuccess) {
        this.stage = stage;
        this.onLoginSuccess = onLoginSuccess;
        initializeScenes();
    }

    private void initializeScenes() {
        try {
            // Load login scene
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/LoginScene.fxml"));
            Parent loginRoot = loginLoader.load();
            loginController = loginLoader.getController();
            loginScene = new Scene(loginRoot);

            // Load register scene
            FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("/fxml/RegisterScene.fxml"));
            Parent registerRoot = registerLoader.load();
            registerController = registerLoader.getController();
            registerScene = new Scene(registerRoot);

            // Set up callbacks
            loginController.setLoginSuccessCallback(onLoginSuccess);
            loginController.setSwitchToRegisterCallback(this::switchToRegister);
            registerController.setSwitchToLoginCallback(this::switchToLogin);

        } catch (IOException e) {
            System.err.println("Error initializing authentication scenes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showLogin() {
        stage.setScene(loginScene);
        stage.setTitle("Login");
        stage.show();
    }

    public void switchToLogin() {
        if (registerController != null) {
            registerController.cleanup();
        }
        showLogin();
    }

    public void switchToRegister() {
        if (loginController != null) {
            loginController.cleanup();
        }
        stage.setScene(registerScene);
        stage.setTitle("Register");
        stage.show();
    }

    public void cleanup() {
        if (loginController != null) {
            loginController.cleanup();
        }
        if (registerController != null) {
            registerController.cleanup();
        }
    }
}