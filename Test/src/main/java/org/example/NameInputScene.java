package org.example;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class NameInputScene extends FXGLMenu {
    private AudioClip menuMusic;

    public NameInputScene() {
        super(MenuType.MAIN_MENU);
        System.out.println("Constructing NameInputScene...");
    }

    @Override
    public void onCreate() {
        loadLoginUI();
        // Load background music
        try {
            java.net.URL musicUrl = getClass().getResource("/assets/music/login_music.mp3");
            if (musicUrl == null) {
                throw new IllegalStateException("Menu music file not found at /assets/music/login_music.mp3.");
            }
            menuMusic = new AudioClip(musicUrl.toExternalForm());
            menuMusic.setCycleCount(AudioClip.INDEFINITE);
            menuMusic.setVolume(0.5);
            menuMusic.play();
            System.out.println("Menu music loaded and playing successfully in NameInputScene");
        } catch (Exception e) {
            System.err.println("Error loading menu music in NameInputScene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLoginUI() {
        System.out.println("NameInputScene onCreate called");
        try {
            System.out.println("Attempting to load LoginScene.fxml from path: /fxml/LoginScene.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScene.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("LoginScene.fxml not found at /fxml/LoginScene.fxml. Ensure the file exists in src/main/resources/fxml/");
            }
            Pane loginPane = loader.load();
            System.out.println("LoginScene.fxml loaded successfully");

            LoginController controller = loader.getController();
            controller.setLoginSuccessCallback(() -> {
                System.out.println("Login successful - clearing login UI");
                getContentRoot().getChildren().clear();
                onDestroy();
                System.out.println("NameInputScene content root children after clear: " + getContentRoot().getChildren());
            });

            loginPane.setTranslateX(FXGL.getAppWidth() / 2.0 - loginPane.getPrefWidth() / 2);
            loginPane.setTranslateY(FXGL.getAppHeight() / 2.0 - loginPane.getPrefHeight() / 2);

            getContentRoot().getChildren().add(loginPane);
            System.out.println("NameInputScene constructed and added to content root");
        } catch (Exception e) {
            System.err.println("Error loading login UI: " + e.getMessage());
            e.printStackTrace();
            FXGL.getDialogService().showMessageBox("Failed to load login UI. Using fallback UI.", () -> {
                loadFallbackUI();
            });
        }
    }

    private void loadFallbackUI() {
        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        Text errorText = new Text("Login UI failed to load. Please enter your username:");
        errorText.setFill(Color.WHITE);
        TextField userField = new TextField();
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            String username = userField.getText().trim();
            if (!username.isEmpty()) {
                GameApp.startGameWithName(username);
                fireNewGame();
            }
        });
        vbox.getChildren().addAll(errorText, userField, loginButton);
        getContentRoot().getChildren().add(vbox);
    }

    public void reloadLoginUI() {
        System.out.println("Reloading login UI in NameInputScene...");
        getContentRoot().getChildren().clear();
        loadLoginUI();
        // Restart the menu music only if not playing
        if (menuMusic != null) {
            menuMusic.stop();
            menuMusic.play();
            System.out.println("Menu music restarted in NameInputScene");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (menuMusic != null) {
            menuMusic.stop();
            System.out.println("Menu music stopped in NameInputScene");
        }
    }
}