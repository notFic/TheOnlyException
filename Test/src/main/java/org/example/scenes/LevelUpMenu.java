package org.example.scenes;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
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

    // Constants for the menu design
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 300;
    private static final int ICON_SIZE = 100;
    private static final int OPTIONS_TO_SHOW = 4;

    public LevelUpMenu(PlayerComponent playerComponent) {
        this.playerComponent = playerComponent;
    }

    public void show() {
        // Pause the game timers before showing the menu
        GameApp gameApp = playerComponent.getGameApp();
        if (gameApp != null) {
            gameApp.pauseGameTimers();
        }

        // Create the main container
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-border-color: goldenrod; -fx-border-width: 3;");

        // Create the title
        Text titleText = new Text("LEVEL UP!");
        titleText.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        titleText.setFill(Color.GOLD);
        titleText.setTextAlignment(TextAlignment.CENTER);

        // Create subtitle
        Text subtitleText = new Text("Choose a weapon or powerup:");
        subtitleText.setFont(Font.font("Verdana", 18));
        subtitleText.setFill(Color.WHITE);

        // Create the grid of weapon options
        HBox weaponGrid = new HBox(20);
        weaponGrid.setAlignment(Pos.CENTER);

        // Get random upgrade options to display from the registry
        UpgradeRegistry registry = UpgradeRegistry.getInstance();
        List<UpgradeOption> selectedOptions = registry.getRandomUpgradeOptions(playerComponent, OPTIONS_TO_SHOW);

        // Create buttons for each upgrade option
        List<Button> optionButtons = new ArrayList<>();
        for (UpgradeOption option : selectedOptions) {
            Button button = createUpgradeCard(option);
            optionButtons.add(button);
            weaponGrid.getChildren().add(button);
        }

        // Add components to the container
        container.getChildren().addAll(titleText, subtitleText, weaponGrid);

        // Convert list to array for the dialog
        Button[] buttons = optionButtons.toArray(new Button[0]);

        // Show the dialog without pausing the engine
        FXGL.getDialogService().showBox("Level Up", container, buttons);
    }

    private Button createUpgradeCard(UpgradeOption option) {
        VBox cardContent = new VBox(10);
        cardContent.setAlignment(Pos.CENTER);
        cardContent.setPadding(new Insets(15));
        cardContent.setPrefSize(CARD_WIDTH, CARD_HEIGHT);

        // Create a button that wraps the card content
        Button card = new Button();
        card.setGraphic(cardContent);
        card.setStyle(
                "-fx-background-color: rgba(60, 60, 60, 0.8); " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        // Add hover effect
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

        // Add click event
        card.setOnAction(e -> {
            int currentLevel = playerComponent.getWeaponLevel(option.getId());

            // Update the weapon level
            playerComponent.onWeaponSelectedNoResume(option.getId(), currentLevel + 1);

            // Resume game timers with slight delay to avoid speed-up
            GameApp gameApp = playerComponent.getGameApp();
            if (gameApp != null) {
                FXGL.runOnce(() -> {
                    // Force an immediate UI update to reset exp bar to the correct value
                    FXGL.getWorldProperties().setValue("exp", playerComponent.getExp());
                    gameApp.updateExpBar();

                    // Resume game timers
                    gameApp.resumeGameTimers();
                }, Duration.seconds(0.2));
            }
        });

        // Store the option with the button for reference
        card.setUserData(option);

        // Create category badge
        HBox categoryBox = new HBox();
        categoryBox.setAlignment(Pos.CENTER);

        String categoryType = (option.getType() == OptionType.WEAPON) ? "WEAPON" : "POWERUP";
        Text categoryText = new Text(categoryType);
        categoryText.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        categoryText.setFill(Color.WHITE);

        // Create badge background with different color for each type
        Rectangle categoryBadge = new Rectangle(80, 20);
        categoryBadge.setArcWidth(10);
        categoryBadge.setArcHeight(10);

        if (option.getType() == OptionType.WEAPON) {
            categoryBadge.setFill(Color.web("#8B0000")); // Dark red for weapons
        } else {
            categoryBadge.setFill(Color.web("#006400")); // Dark green for powerups
        }

        StackPane badge = new StackPane(categoryBadge, categoryText);
        categoryBox.getChildren().add(badge);

        // Create upgrade title
        Text upgradeName = new Text(option.getName());
        upgradeName.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        upgradeName.setFill(Color.WHITE);

        // Show the next level instead of current level
        int currentLevel = playerComponent.getWeaponLevel(option.getId());
        int nextLevel = currentLevel + 1;
        
        Text levelText = new Text(currentLevel == 0 ? "NEW!" : "Level " + nextLevel);
        levelText.setFont(Font.font("Verdana", 14));
        levelText.setFill(currentLevel == 0 ? Color.GOLD : Color.LIGHTGREEN);

        // Create the icon (image or fallback placeholder)
        StackPane iconPane;
        try {
            // Load the power-up image
            ImageView imageView = new ImageView(FXGL.image(option.getImagePath()));
            imageView.setFitWidth(ICON_SIZE);
            imageView.setFitHeight(ICON_SIZE);
            imageView.setPreserveRatio(true);
            iconPane = new StackPane(imageView);
        } catch (Exception e) {
            // Fallback to original placeholder if image fails to load
            Rectangle placeholder = new Rectangle(ICON_SIZE, ICON_SIZE);
            placeholder.setFill(option.getColor());
            placeholder.setArcWidth(10);
            placeholder.setArcHeight(10);
            placeholder.setStroke(Color.WHITE);
            placeholder.setStrokeWidth(2);

            Text initial = new Text(option.getName().substring(0, 1).toUpperCase());
            initial.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
            initial.setFill(Color.WHITE);

            iconPane = new StackPane(placeholder, initial);
            System.err.println("Warning: Failed to load image for " + option.getImagePath() + ": " + e.getMessage());
        }

        // Create a wrapper with proper dimensions
        StackPane iconWrapper = new StackPane(iconPane);
        iconWrapper.setMinSize(ICON_SIZE, ICON_SIZE);
        iconWrapper.setMaxSize(ICON_SIZE, ICON_SIZE);

        // Add drop shadow effect to the icon
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.BLACK);
        iconWrapper.setEffect(shadow);

        // Create upgrade description with next level info for specific upgrades
        String description = option.getDescription();
        if ("gun".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 7) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                switch (currentLevel + 1) {
                    case 1: description = "Shoot 3 bullets every 0.75s (10 damage each)"; break;
                    case 2: description = "Increase damage by 100% (20 damage each)"; break;
                    case 3: description = "Can pierce through 1 enemy"; break;
                    case 4: description = "Decrease cooldown to 0.50s"; break;
                    case 5: description = "Increase damage by 100% (40 damage each)"; break;
                    case 6: description = "Can pierce through 3 enemies"; break;
                    case 7: description = "Decrease cooldown to 0.30s"; break;
                }
            }
        } else if ("lightning".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 7) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                description = org.example.powerups.LightningStrikeComponent.getLevelDescription(currentLevel, true);
            }
        } else if ("poison".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 7) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                description = org.example.powerups.PoisonAuraComponent.getLevelDescription(currentLevel, true);
            }
        } else if ("shield".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 5) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                description = org.example.powerups.ShieldComponent.getLevelDescription(currentLevel, true);
            }
        } else if ("auto_heal".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 5) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                description = org.example.powerups.AutoHealComponent.getLevelDescription(currentLevel, true);
            }
        } else if ("fire_trail".equals(option.getId())) {
            // If at max level, show maxed out message
            if (currentLevel >= 5) {
                description = "MAXED OUT";
            } else {
                // Show only the next level description
                description = org.example.powerups.FireTrailComponent.getLevelDescription(currentLevel, true);
            }
        }

        Text descriptionText = new Text(description);
        descriptionText.setFont(Font.font("Verdana", 14));
        descriptionText.setFill(Color.LIGHTGRAY);
        descriptionText.setWrappingWidth(CARD_WIDTH - 30);
        descriptionText.setTextAlignment(TextAlignment.CENTER);

        // Add all elements to the card content
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

        // Animate the damageText node directly
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