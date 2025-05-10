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
import org.example.core.SoundManager;

import java.util.List;
import java.util.stream.Collectors;

public class LightningStrikeComponent extends Component {

    // TODO add a method that will activate this method below every 10 seconds once ma toggle ang iyahang set keybind
    // (KEYBIND IS ONLY FOR DEBUGGING PURPOSES ONLY. we want to pass on this implementation when the player wants
    // to chose this specific powerup. Once implemented correctly, we can apply the same logic for other powerups in the future)

    // ACHIEVED ^^

    private PlayerComponent playerComponent;
    
    @Override
    public void onAdded() {
        playerComponent = entity.getComponent(PlayerComponent.class);
    }

    public void activatePowerUp() {
        if (playerComponent == null) return;
        
        int level = playerComponent.getWeaponLevel("lightning");
        if (level <= 0) return;

        System.out.println("Lightning Strike activated at level: " + level + " with cooldown: " + getCooldownForLevel(level) + " seconds");
        
        // Get all active enemies on screen
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY)
                .stream()
                .filter(Entity::isActive)
                .collect(Collectors.toList());

        // Determine number of enemies to hit based on level
        int enemiesToDamage = 3;  // Default for level 1
        if (level >= 4) enemiesToDamage = 5;  // Level 4+
        if (level >= 7) enemiesToDamage = 8;  // Level 7 (max)
        
        enemiesToDamage = Math.min(enemiesToDamage, enemies.size());

        // Calculate damage based on level
        int baseDamage = 30;  // Level 1 damage
        if (level >= 2) baseDamage = (int)(baseDamage * 1.5);  // Level 2+ = 45
        if (level >= 5) baseDamage = (int)(baseDamage * 1.5);  // Level 5+ = 67

        // Strike random enemies
        for (int i = 0; i < enemiesToDamage; i++) {
            if (enemies.isEmpty()) break;
            
            int randomIndex = (int) (Math.random() * enemies.size());
            Entity enemy = enemies.get(randomIndex);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            showLightningStrike(enemy.getCenter());
            SoundManager.getInstance().playSound("lightning"); // Play hit sound
            enemyComponent.damage(baseDamage, enemy.getCenter());

            // Remove from list to avoid damaging same enemy twice
            enemies.remove(randomIndex);
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
//        cameraShake();

        FXGL.getGameTimer().runOnceAfter(lightning::removeFromWorld, Duration.seconds(0.5));
    }

//    void cameraShake(){
//        Node root = FXGL.getGameScene().getRoot();
//
//        TranslateTransition shake = new TranslateTransition(Duration.seconds(0.1), root);
//        shake.setFromY(-3);
//        shake.setToY(3);
//        shake.setCycleCount(4);
//        shake.setAutoReverse(true);
//        shake.setOnFinished(e -> root.setTranslateY(0)); // reset just in case
//        shake.play();
//    }
    
    /**
     * Returns the description for the current level or next level if nextLevel is true
     * @param level Current level
     * @param nextLevel Whether to get next level description
     * @return Description string for the level
     */
    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;
        
        switch (level) {
            case 1: return "Strike 3 random enemies every 5 secs (30 dmg)";
            case 2: return "Increase damage by 50% (45 dmg)";
            case 3: return "Decrease cooldown to 3 secs";
            case 4: return "Strike 5 enemies";
            case 5: return "Increase damage by 50% (67 dmg)";
            case 6: return "Decrease cooldown to 1.5 secs";
            case 7: return "Strike 8 enemies";
            default: return level > 7 ? "MAXED OUT" : "Strikes random enemies with lightning";
        }
    }
    
    /**
     * Returns the cooldown in seconds for a given level
     * @param level The lightning strike level
     * @return Cooldown in seconds
     */
    public static double getCooldownForLevel(int level) {
        if (level >= 6) return 1.5; // Level 6+
        if (level >= 3) return 3.0; // Level 3+
        return 5.0; // Level 1-2
    }
}