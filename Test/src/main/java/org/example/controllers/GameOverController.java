package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.example.core.GameApp;

public class GameOverController {

    @FXML
    private Label gameOverLabel;

    @FXML
    private Label statsLabel;

    @FXML
    private Button backToMenuButton;

    private GameApp gameApp;
    private Runnable backToMenuCallback;

    public void initialize() {
        System.out.println("GameOverController initialized");
        System.out.println("gameOverLabel: " + (gameOverLabel != null ? "not null" : "null"));
        System.out.println("statsLabel: " + (statsLabel != null ? "not null" : "null"));
        System.out.println("backToMenuButton: " + (backToMenuButton != null ? "not null" : "null"));
        if (gameOverLabel != null) {
            System.out.println("gameOverLabel font: " + gameOverLabel.getFont().getName());
        }
    }

    public void setGameApp(GameApp gameApp) {
        this.gameApp = gameApp;
    }

    public void setStats(int survivalTime, int totalDamage, int kills) {
        System.out.println("Setting stats: survivalTime=" + survivalTime + ", totalDamage=" + totalDamage + ", kills=" + kills);
        statsLabel.setText(
                "Survival Time: " + survivalTime + " s\n" +
                        "Total Damage: " + totalDamage + "\n" +
                        "Kills: " + kills
        );
    }

    public void setBackToMenuCallback(Runnable callback) {
        this.backToMenuCallback = callback;
    }

    @FXML
    private void handleBackToMenu() {
        System.out.println("Back to Main Menu button clicked");
        if (backToMenuCallback != null) {
            backToMenuCallback.run();
        }
    }

    public Button getBackToMenuButton() {
        return backToMenuButton;
    }
}