package org.example;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.*;

public class UpgradeManager {

    private final PlayerComponent player;

    public UpgradeManager(PlayerComponent player) {
        this.player = player;
        initUpgrades();
    }

    public static class UpgradeOption {
        public String name;
        public String description;
        public Runnable onApply;

        public UpgradeOption(String name, String description, Runnable onApply) {
            this.name = name;
            this.description = description;
            this.onApply = onApply;
        }
    }

    private List<UpgradeOption> allUpgrades;

    private void initUpgrades() {
        allUpgrades = List.of(
                new UpgradeOption("Max HP +20", "Increase max HP by 20", () -> {
                    player.increaseMaxHealth(20);
                }),
                new UpgradeOption("Speed +0.5", "Move faster", () -> {
                    player.increaseSpeed(0.5);
                }),
                new UpgradeOption("Heal 50%", "Restore 50% HP", () -> {
                    player.healPercent(0.5);
                })

                // Add more upgrades here
        );
    }

    public void showUpgradeChoices() {
        FXGL.getGameController().pauseEngine();

        List<UpgradeOption> choices = new ArrayList<>(allUpgrades);
        Collections.shuffle(choices);
        choices = choices.subList(0, Math.min(3, choices.size()));

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 30;");
        box.setTranslateX(FXGL.getAppWidth() / 2.0 - 150);
        box.setTranslateY(FXGL.getAppHeight() / 2.0 - 100);

        for (UpgradeOption upgrade : choices) {
            Button btn = new Button(upgrade.name + "\n" + upgrade.description);
            btn.setStyle("-fx-font-size: 16; -fx-text-fill: white; -fx-background-color: #444;");
            btn.setOnAction(e -> {
                upgrade.onApply.run();
                player.updateHealthBar(); // update visuals
                FXGL.getSceneService().getOverlayRoot().getChildren().remove(box); // manually remove box
                FXGL.getGameController().resumeEngine();
            });
            box.getChildren().add(btn);
        }

        FXGL.getSceneService().getOverlayRoot().getChildren().add(box);
    }
}
