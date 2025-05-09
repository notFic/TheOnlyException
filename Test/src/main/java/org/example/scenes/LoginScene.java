package org.example.scenes;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import org.example.controllers.LoginController;

public class LoginScene extends FXGLMenu {

    private LoginController controller;
    private static AudioClip loginMusic;

    public LoginScene() {
        super(MenuType.MAIN_MENU);
        System.out.println("Constructing LoginScene...");
        loadLoginUI();
        playLoginMusic();
    }

    private void loadLoginUI() {
        try {
            System.out.println("Attempting to load LoginScene.fxml from path: /fxml/LoginScene.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("LoginScene.fxml not found at /fxml/LoginScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Pane loginPane = loader.load();
            controller = loader.getController();

            loginPane.setTranslateX(FXGL.getAppWidth() / 2.0 - loginPane.getPrefWidth() / 2);
            loginPane.setTranslateY(FXGL.getAppHeight() / 2.0 - loginPane.getPrefHeight() / 2);

            getContentRoot().getChildren().add(loginPane);
            System.out.println("LoginScene constructed and added to content root");
        } catch (Exception e) {
            System.err.println("Error loading LoginScene UI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void playLoginMusic() {
        try {
            java.net.URL musicUrl = getClass().getResource("/assets/music/login_music.mp3");
            if (musicUrl == null) {
                throw new IllegalStateException("Login music file not found at /assets/music/login_music.mp3.");
            }
            loginMusic = new AudioClip(musicUrl.toExternalForm());
            loginMusic.setCycleCount(AudioClip.INDEFINITE);
            loginMusic.setVolume(0.5);
            loginMusic.play();
            System.out.println("Login music loaded and playing successfully");
        } catch (Exception e) {
            System.err.println("Error loading login music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopLoginMusic() {
        loginMusic.stop();
    }

    // Public method to access the controller
    public LoginController getLoginController() {
        return controller;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) {
            controller.cleanup();
        }
        if (loginMusic != null) {
            loginMusic.stop();
            System.out.println("Login music stopped in onDestroy");
            loginMusic = null;
        }
        getContentRoot().getChildren().clear();
        System.out.println("LoginScene content root cleared in onDestroy");
    }
}