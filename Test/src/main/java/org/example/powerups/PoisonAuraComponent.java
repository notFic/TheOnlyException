package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;
import org.example.core.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class PoisonAuraComponent extends Component {

    // Base stats at level 1
    private static final double BASE_RADIUS = 100;
    private static final double BASE_DAMAGE_PER_SECOND = 14; // Base damage per second (7 damage per 0.5s tick)
    private static final double BASE_DAMAGE_INTERVAL = 0.5;

    private double radius = BASE_RADIUS;
    private double damagePerSecond = BASE_DAMAGE_PER_SECOND;
    private double damageInterval = BASE_DAMAGE_INTERVAL;

    private double damageTimer = 0;
    private int level = 1;
    private PlayerComponent playerComponent;
    private Entity auraEntity;
    private boolean isActive = true;

    @Override
    public void onAdded() {
        playerComponent = entity.getComponent(PlayerComponent.class);
        updateStatsBasedOnLevel();
        spawnAura();
    }

    private void spawnAura() {
        if (auraEntity != null && auraEntity.isActive()) {
            auraEntity.removeFromWorld();
        }

        // Animation setup with 11 frames
        AnimationChannel auraAnim = new AnimationChannel(
                FXGL.image("realtimedefenseaura.png"),
                11,                   // Number of frames
                64, 64,              // Frame dimensions
                Duration.seconds(1.1), // Duration for full animation
                0, 10                 // Frame range
        );

        AnimatedTexture texture = new AnimatedTexture(auraAnim);
        texture.loop();

        // Calculate scale to match desired radius (64px becomes diameter)
        double scale = (2 * radius) / 64.0;

        auraEntity = FXGL.entityBuilder()
                .type(EntityType.POISON_AURA)
                .at(entity.getCenter().subtract(radius, radius))
                .view(texture)
                .scale(scale, scale)
                .zIndex(100) // Render above player but below UI
                .buildAndAttach();
    }

    private void updateAuraScale() {
        if (auraEntity == null) return;

        double scale = (2 * radius) / 64.0;
        auraEntity.setScaleX(scale);
        auraEntity.setScaleY(scale);
        auraEntity.setPosition(entity.getCenter().subtract(radius, radius));
    }

    private void updateStatsBasedOnLevel() {
        if (playerComponent == null) return;

        level = playerComponent.getWeaponLevel("poison");

        // Reset to base values
        radius = BASE_RADIUS;
        damagePerSecond = BASE_DAMAGE_PER_SECOND;
        damageInterval = BASE_DAMAGE_INTERVAL;

        // Apply level upgrades
        if (level >= 2) radius *= 1.3; // +30% radius
        if (level >= 3) damagePerSecond *= 2; // +100% damage
        if (level >= 4) {
            damageInterval *= 0.4; // -60% interval (faster ticks)
            damagePerSecond *= 2.5; // +150% damage to compensate for faster ticks
        }
        if (level >= 5) radius *= 1.3; // +30% radius
        if (level >= 6) damagePerSecond *= 2; // +100% damage
        if (level >= 7) radius *= 1.3; // +30% radius

        updateAuraScale();
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        // Update aura position to follow player
        if (auraEntity != null) {
            auraEntity.setPosition(entity.getCenter().subtract(radius, radius));
        }

        // Damage logic
        damageTimer += tpf;
        if (damageTimer >= damageInterval) {
            damageTimer = 0;

            Point2D center = entity.getCenter();
            List<Entity> enemies = FXGL.getGameWorld()
                    .getEntitiesByType(EntityType.ENEMY)
                    .stream()
                    .filter(e -> e.isActive() && e.getPosition().distance(center) <= radius)
                    .collect(Collectors.toList());

            for (Entity enemy : enemies) {
                EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
                double damage = damagePerSecond * damageInterval; // Calculate exact damage
                ec.damage((int)Math.round(damage), enemy.getCenter()); // Round to nearest integer
            }
        }
    }

    public void activatePowerUp() {
        isActive = true;
        updateStatsBasedOnLevel();
        if (auraEntity == null || !auraEntity.isActive()) {
            spawnAura();
        }
    }

    public void deactivatePowerUp() {
        isActive = false;
        if (auraEntity != null && auraEntity.isActive()) {
            auraEntity.removeFromWorld();
        }
    }

    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;

        return switch (level) {
            case 1 -> "Damages enemies within 100 range for 7 damage per 0.5s";
            case 2 -> "Increase radius by 30% to 130";
            case 3 -> "Increase damage by 100% to 14 damage per 0.5s";
            case 4 -> "Decrease cooldown by 60% to 0.2s and increase damage by 150% to 14 damage per 0.2s";
            case 5 -> "Increase radius by 30% to 169";
            case 6 -> "Increase damage by 100% to 28 damage per 0.2s";
            case 7 -> "Increase radius by 30% to 220";
            default -> level > 7 ? "MAXED OUT" : "Damages enemies within 100 range for 7 damage per 0.5s";
        };
    }
}