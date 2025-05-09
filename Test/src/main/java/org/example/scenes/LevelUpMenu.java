package org.example.scenes;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.example.upgrades.UpgradeRegistry;
import org.example.components.PlayerComponent;
import org.example.core.GameApp;
import org.example.model.OptionType;
import org.example.model.UpgradeOption;

import java.util.*;

public class LevelUpMenu {
    private final PlayerComponent playerComponent;

    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 300;
    private static final int ICON_SIZE = 100;
    private static final int OPTIONS_TO_SHOW = 4;

    public LevelUpMenu(PlayerComponent playerComponent) {
        this.playerComponent = playerComponent;
    }

    public void show() {
        GameApp gameApp = playerComponent.getGameApp();
        if (gameApp != null) {
            gameApp.pauseGameTimers();
        }

        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-border-color: goldenrod; -fx-border-width: 3;");

        Text titleText = new Text("LEVEL UP!");
        titleText.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        titleText.setFill(Color.GOLD);
        titleText.setTextAlignment(TextAlignment.CENTER);

        Text subtitleText = new Text("Choose a weapon or powerup:");
        subtitleText.setFont(Font.font("Verdana", 18));
        subtitleText.setFill(Color.WHITE);

        HBox weaponGrid = new HBox(20);
        weaponGrid.setAlignment(Pos.CENTER);

        UpgradeRegistry registry = UpgradeRegistry.getInstance();
        List<UpgradeOption> selectedOptions = registry.getRandomUpgradeOptions(playerComponent, OPTIONS_TO_SHOW);

        List<Button> optionButtons = new ArrayList<>();
        for (UpgradeOption option : selectedOptions) {
            Button button = createUpgradeCard(option);
            optionButtons.add(button);
            weaponGrid.getChildren().add(button);
        }

        container.getChildren().addAll(titleText, subtitleText, weaponGrid);

        Button[] buttons = optionButtons.toArray(new Button[0]);

        FXGL.getDialogService().showBox("Level Up", container, buttons);
    }

    private Button createUpgradeCard(UpgradeOption option) {
        VBox cardContent = new VBox(10);
        cardContent.setAlignment(Pos.CENTER);
        cardContent.setPadding(new Insets(15));
        cardContent.setPrefSize(CARD_WIDTH, CARD_HEIGHT);

        Button card = new Button();
        card.setGraphic(cardContent);
        card.setStyle(
                "-fx-background-color: rgba(60, 60, 60, 0.8); " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(80, 80, 80, 0.8); " +
                        "-fx-border-color: gold; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(60, 60, 60, 0.8); " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        ));

        card.setOnAction(e -> {
            int currentLevel = playerComponent.getWeaponLevel(option.getId());
            playerComponent.onWeaponSelectedNoResume(option.getId(), currentLevel + 1);

            GameApp gameApp = playerComponent.getGameApp();
            if (gameApp != null) {
                FXGL.runOnce(() -> {
                    FXGL.getWorldProperties().setValue("exp", playerComponent.getExp());
                    gameApp.updateExpBar();
                    gameApp.resumeGameTimers();
                }, Duration.seconds(0.2));
            }
        });

        card.setUserData(option);

        HBox categoryBox = new HBox();
        categoryBox.setAlignment(Pos.CENTER);

        String categoryType = (option.getType() == OptionType.WEAPON) ? "WEAPON" : "POWERUP";
        Text categoryText = new Text(categoryType);
        categoryText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        categoryText.setFill(Color.WHITE);

        Rectangle categoryBadge = new Rectangle(80, 20);
        categoryBadge.setArcWidth(10);
        categoryBadge.setArcHeight(10);

        if (option.getType() == OptionType.WEAPON) {
            categoryBadge.setFill(Color.web("#8B0000"));
        } else {
            categoryBadge.setFill(Color.web("#006400"));
        }

        StackPane badge = new StackPane(categoryBadge, categoryText);
        categoryBox.getChildren().add(badge);

        Text upgradeName = new Text(option.getName());
        upgradeName.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        upgradeName.setFill(Color.WHITE);

        int currentLevel = playerComponent.getWeaponLevel(option.getId());
        int nextLevel = currentLevel + 1;
        Text levelText = new Text(currentLevel == 0 ? "NEW!" : "Level " + nextLevel);
        levelText.setFont(Font.font("Verdana", 14));
        levelText.setFill(currentLevel == 0 ? Color.GOLD : Color.LIGHTGREEN);

        Rectangle placeholder = new Rectangle(ICON_SIZE, ICON_SIZE);
        placeholder.setFill(option.getColor());
        placeholder.setArcWidth(10);
        placeholder.setArcHeight(10);
        placeholder.setStroke(Color.WHITE);
        placeholder.setStrokeWidth(2);

        Text initial = new Text(option.getName().substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        initial.setFill(Color.WHITE);

        StackPane iconPane = new StackPane(placeholder, initial);

        StackPane iconWrapper = new StackPane(iconPane);
        iconWrapper.setMinSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setMaxSize(ICON_SIZE, ICON_SIZE);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.BLACK);
        iconWrapper.setEffect(shadow);

        String description = option.getDescription();
        if ("lightning".equals(option.getId())) {
            if (currentLevel >= 7) {
                description = "MAXED OUT";
            } else {
                description = org.example.powerups.LightningStrikeComponent.getLevelDescription(currentLevel, true);
            }
        } else if ("poison".equals(option.getId())) {
            if (currentLevel >= 7) {
                description = "MAXED OUT";
            } else {
                description = org.example.powerups.PoisonAuraComponent.getLevelDescription(currentLevel, true);
            }
        }

        Text descriptionText = new Text(description);
        descriptionText.setFont(Font.font("Verdana", 14));
        descriptionText.setFill(Color.LIGHTGRAY);
        descriptionText.setWrappingWidth(CARD_WIDTH - 30);
        descriptionText.setTextAlignment(TextAlignment.CENTER);

        cardContent.getChildren().addAll(categoryBox, upgradeName, levelText, iconWrapper, descriptionText);

        return card;
    }

    private void showDamageText(double dmg, Point2D hitPosition) {
        var damageText = FXGL.getUIFactoryService().newText(String.valueOf((int) dmg), Color.WHITE, 24);
        var textEntity = FXGL.entityBuilder()
                .at(hitPosition.subtract(10, 30))
                .view(damageText)
                .zIndex(1000)
                .buildAndAttach();

        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(1), damageText);
        tt.setByX(-60); // move left
        tt.setByY(-80); // move up
        tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1), damageText);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        tt.play();
        ft.play();

        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), javafx.util.Duration.seconds(1));
    }
}