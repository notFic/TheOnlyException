package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;
import org.example.core.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class LightningStrikeComponent extends Component {

    // TODO add a method that will activate this method below every 10 seconds once ma toggle ang iyahang set keybind
    // (KEYBIND IS ONLY FOR DEBUGGING PURPOSES ONLY. we want to pass on this implementation when the player wants
    // to chose this specific powerup. Once implemented correctly, we can apply the same logic for other powerups in the future)

    // ACHIEVED ^^

    // Lightning strike level attributes
    private int enemiesToStrike = 3;  // Default for level 1
    private int damage = 30;         // Default for level 1

    public void activatePowerUp() {
        // Get all active enemies on screen
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY)
                .stream()
                .filter(Entity::isActive)
                .collect(Collectors.toList());

        // Damage random enemies (or all if there are less than the allowed amount)
        int enemiesToDamage = Math.min(enemiesToStrike, enemies.size());
        for (int i = 0; i < enemiesToDamage; i++) {
            int randomIndex = (int) (Math.random() * enemies.size());
            Entity enemy = enemies.get(randomIndex);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            showLightningStrike(enemy.getCenter());
            enemyComponent.damage(damage, enemy.getCenter());

            // Remove from list to avoid damaging same enemy twice
            enemies.remove(randomIndex);
        }
    }

    // Update the component based on player's lightning level
    public void updateForLevel(int level) {
        switch (level) {
            case 1:
                // Level 1: Strike 3 random enemies, 30 damage
                enemiesToStrike = 3;
                damage = 30;
                break;
            case 2:
                // Level 2: Increased damage by 50%
                enemiesToStrike = 3;
                damage = 45;  // 30 + 50%
                break;
            case 3:
                // Level 3: Same as level 2 but cooldown decreased in PlayerComponent
                enemiesToStrike = 3;
                damage = 45;
                break;
            case 4:
                // Level 4: Strike 5 enemies
                enemiesToStrike = 5;
                damage = 45;
                break;
            case 5:
                // Level 5: Increased damage by another 50%
                enemiesToStrike = 5;
                damage = 68;  // 45 + 50% (rounded up)
                break;
            case 6:
                // Level 6: Same as level 5 but cooldown decreased in PlayerComponent
                enemiesToStrike = 5;
                damage = 68;
                break;
            case 7:
                // Level 7: Strike 8 enemies
                enemiesToStrike = 8;
                damage = 68;
                break;
            default:
                // Should not happen, but default to level 1
                enemiesToStrike = 3;
                damage = 30;
                break;
        }
    }

    // Gets upgrade description for next level
    public static String getNextLevelDescription(int currentLevel) {
        switch (currentLevel) {
            case 0:
                return "Level 1: Strike 3 random enemies every 5 secs (30 dmg)";
            case 1:
                return "Level 2: Increase damage by 50%";
            case 2:
                return "Level 3: Decrease cooldown to 3 secs";
            case 3:
                return "Level 4: Strike 5 enemies";
            case 4:
                return "Level 5: Increase damage by 50%";
            case 5:
                return "Level 6: Decrease cooldown to 1.5 secs";
            case 6:
                return "Level 7 (MAX): Strike 8 enemies";
            default:
                return "MAXIMUM LEVEL";
        }
    }

    private void showLightningStrike(Point2D position) {
        Image image = FXGL.image("lightning_strike.png");
        int frameWidth = (int) image.getWidth() / 5; // 5 frames
        int frameHeight = (int) image.getHeight();

        AnimationChannel channel = new AnimationChannel(image, 5, frameWidth, frameHeight, Duration.seconds(0.4), 0, 4);
        AnimatedTexture animatedTexture = new AnimatedTexture(channel);
        animatedTexture.play(); // plays the loaded image

        double strikeX = position.getX() - (frameWidth / 2.0); // center horizontally
        double strikeY = 0; // from top of screen

        Entity lightning = FXGL.entityBuilder()
                .at(strikeX, strikeY)
                .view(animatedTexture)
                .zIndex(1000)
                .buildAndAttach();

        double strikeHeight = position.getY();
        lightning.setScaleY(strikeHeight / frameHeight);
        cameraShake();

        FXGL.getGameTimer().runOnceAfter(lightning::removeFromWorld, Duration.seconds(0.5));
    }

    void cameraShake(){
        Node root = FXGL.getGameScene().getRoot();

        TranslateTransition shake = new TranslateTransition(Duration.seconds(0.1), root);
        shake.setFromY(-3);
        shake.setToY(3);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> root.setTranslateY(0)); // reset just in case
        shake.play();
    }
}