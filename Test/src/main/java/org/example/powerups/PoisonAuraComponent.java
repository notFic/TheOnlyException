package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;
import org.example.core.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class PoisonAuraComponent extends Component {

    // Base stats at level 1
    private static final double BASE_RADIUS = 100; // Radius of the poison aura
    private static final double BASE_DAMAGE_PER_SECOND = 7;
    private static final double BASE_DAMAGE_INTERVAL = 0.5; // seconds between damage ticks

    // Current calculated values based on level
    private double radius = BASE_RADIUS;
    private double damagePerSecond = BASE_DAMAGE_PER_SECOND;
    private double damageInterval = BASE_DAMAGE_INTERVAL;
    
    private double damageTimer = 0;
    private int level = 1;
    private PlayerComponent playerComponent;

    // Debugging Hitbox
    private Circle debugHitbox;
    private boolean isActive = true;

    @Override
    public void onAdded() {
        playerComponent = entity.getComponent(PlayerComponent.class);
        updateStatsBasedOnLevel();
        showHitbox(); // Initialize hitbox position when component is added
    }
    
    /**
     * Updates the aura stats based on current level
     */
    private void updateStatsBasedOnLevel() {
        if (playerComponent == null) return;
        
        level = playerComponent.getWeaponLevel("poison");
        
        // Reset to base values
        radius = BASE_RADIUS;
        damagePerSecond = BASE_DAMAGE_PER_SECOND;
        damageInterval = BASE_DAMAGE_INTERVAL;
        
        // Apply level upgrades
        // Level 2: Increase radius by 30%
        if (level >= 2) {
            radius *= 1.3;
        }
        
        // Level 3: Increase damage by 50%
        if (level >= 3) {
            damagePerSecond *= 1.5;
        }
        
        // Level 4: Decrease cooldown (damage interval) by 30%
        if (level >= 4) {
            damageInterval *= 0.7;
        }
        
        // Level 5: Increase radius by another 30%
        if (level >= 5) {
            radius *= 1.3;
        }
        
        // Level 6: Increase damage by another 50%
        if (level >= 6) {
            damagePerSecond *= 1.5;
        }
        
        // Level 7: Increase radius by another 30%
        if (level >= 7) {
            radius *= 1.3;
        }
        
        System.out.println("Poison Aura updated to level " + level + 
                           ": Radius=" + radius + 
                           ", Damage=" + damagePerSecond + 
                           ", Interval=" + damageInterval);
        
        // Update the hitbox if it exists
        if (debugHitbox != null) {
            debugHitbox.setRadius(radius);
        }
    }

    public void activatePowerUp() {
        isActive = true;
        updateStatsBasedOnLevel();
        showHitbox();
    }

    public void deactivatePowerUp() {
        isActive = false;
        hideHitbox();
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        Point2D playerCenterWorld = entity.getCenter();
        Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

        // Update the timer
        damageTimer += tpf;

        if (damageTimer >= damageInterval) {
            damageTimer = 0; // Reset timer after dealing damage

            List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY)
                    .stream()
                    .filter(Entity::isActive)
                    .collect(Collectors.toList());

            for (Entity enemy : enemies) {
                double distance = enemy.getPosition().distance(playerCenterWorld);

                if (distance <= radius) {
                    EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
                    showPoisonAura(enemy.getCenter());
                    
                    // Apply full damage per second, regardless of interval
                    // This means the faster the interval, the more DPS will be applied
                    enemyComponent.damage((int)damagePerSecond, enemy.getCenter());
                }
            }
        }

        if (debugHitbox != null) {
            debugHitbox.setCenterX(playerCenterScreen.getX());
            debugHitbox.setCenterY(playerCenterScreen.getY());
        }
    }

    private void showPoisonAura(Point2D position) {
        // Show visual effects here (particles, etc.)
    }

    private void showHitbox() {
        if (debugHitbox == null) {
            Point2D playerCenterWorld = entity.getCenter();
            Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

            debugHitbox = new Circle(playerCenterScreen.getX(), playerCenterScreen.getY(), radius);
            debugHitbox.setFill(Color.color(1, 0, 0, 0.1));
            debugHitbox.setStroke(Color.rgb(255, 0, 0, 0.3));
            debugHitbox.setStrokeWidth(2);
            FXGL.getGameScene().addUINode(debugHitbox);
        } else {
            Point2D playerCenterWorld = entity.getCenter();
            Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

            debugHitbox.setCenterX(playerCenterScreen.getX());
            debugHitbox.setCenterY(playerCenterScreen.getY());
            debugHitbox.setRadius(radius);
        }
    }

    private void hideHitbox() {
        if (debugHitbox != null) {
            FXGL.getGameScene().removeUINode(debugHitbox);
            debugHitbox = null;
        }
    }
    
    /**
     * Returns the description for the current level or next level if nextLevel is true
     * @param level Current level
     * @param nextLevel Whether to get next level description
     * @return Description string for the level
     */
    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;
        
        switch (level) {
            case 1: return "Damages enemies within range";
            case 2: return "Increase radius by 30%";
            case 3: return "Increase damage by 50%";
            case 4: return "Decrease cooldown by 30%";
            case 5: return "Increase radius by 30%";
            case 6: return "Increase damage by 50%";
            case 7: return "Increase radius by 30%";
            default: return level > 7 ? "MAXED OUT" : "Damages enemies within range";
        }
    }
}