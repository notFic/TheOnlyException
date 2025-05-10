package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.components.PlayerComponent;

public class ShieldComponent extends Component {
    // Shield properties based on level
    private static final int[] SHIELD_CAPACITY = {15, 20, 25, 30, 35}; // Level 1-5 shield capacity
    private static final double[] REFRESH_INTERVAL = {15.0, 15.0, 12.0, 12.0, 10.0}; // Level 1-5 refresh interval
    private static final double[] DAMAGE_REDUCTION_PERCENT = {10.0, 10.0, 15.0, 15.0, 20.0}; // Level 1-5 damage reduction

    private boolean isActive = false;
    private double refreshTimer = 0.0;
    private int level = 0;
    private int currentShieldAmount = 0;
    private int maxShieldAmount = 0;

    // Visual representation
    private Entity shieldBar = null;
    private Rectangle shieldBarBackground = null;
    private Rectangle shieldBarFill = null;
    private final double SHIELD_BAR_WIDTH = 40; // Same as health bar
    private final double SHIELD_BAR_HEIGHT = 5; // Same as health bar
    private final double SHIELD_BAR_Y_OFFSET = 20; // Distance above player health bar

    @Override
    public void onAdded() {
        level = entity.getComponent(PlayerComponent.class).getWeaponLevel("shield");
        if (level > 0) {
            activatePowerUp();
        }
    }

    public void activatePowerUp() {
        if (isActive || level < 1) return;
        isActive = true;
        refreshTimer = 0.0;
        resetShield();
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;
        hideShieldBar();
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        // Update shield refresh timer
        refreshTimer += tpf;
        if (refreshTimer >= getRefreshInterval()) {
            refreshTimer = 0.0;
            resetShield();
        }

        // Update shield bar position
        updateShieldBar();
    }

    private void resetShield() {
        if (level < 1 || level > 5) {
            currentShieldAmount = 0;
            hideShieldBar();
            return;
        }

        maxShieldAmount = SHIELD_CAPACITY[level - 1];
        currentShieldAmount = maxShieldAmount;
        showShieldBar();
        showShieldRefreshEffect();
        FXGL.getNotificationService().pushNotification("Shield refreshed! (" + currentShieldAmount + " capacity)");
    }

    public int absorbDamage(int incomingDamage) {
        if (!isActive || currentShieldAmount <= 0) {
            return incomingDamage;
        }

        // Apply damage reduction
        int reducedDamage = (int) Math.ceil(incomingDamage * (1 - getDamageReductionPercent() / 100.0));

        // Absorb with shield
        if (currentShieldAmount >= reducedDamage) {
            currentShieldAmount -= reducedDamage;
            showShieldHitEffect();
            updateShieldBarFill();

            if (currentShieldAmount <= 0) {
                hideShieldBar();
            }

            return 0;
        } else {
            int remainingDamage = reducedDamage - currentShieldAmount;
            showShieldBreakEffect();
            currentShieldAmount = 0;
            hideShieldBar();

            return remainingDamage;
        }
    }

    private void createShieldBar() {
        shieldBarBackground = new Rectangle(SHIELD_BAR_WIDTH, SHIELD_BAR_HEIGHT, Color.BLACK);
        shieldBarFill = new Rectangle(SHIELD_BAR_WIDTH, SHIELD_BAR_HEIGHT, Color.BLUE);
        var shieldBarGroup = new javafx.scene.Group(shieldBarBackground, shieldBarFill);
        shieldBar = FXGL.entityBuilder().view(shieldBarGroup).zIndex(100).build();
        FXGL.getGameWorld().addEntity(shieldBar);
        updateShieldBar();
    }

    private void updateShieldBarFill() {
        if (shieldBarFill != null && maxShieldAmount > 0) {
            double shieldPercentage = Math.max(0, (double) currentShieldAmount / maxShieldAmount);
            shieldBarFill.setWidth(SHIELD_BAR_WIDTH * Math.min(1.0, shieldPercentage));
        }
    }

    private void showShieldBar() {
        if (shieldBar == null) {
            createShieldBar();
        } else {
            updateShieldBarFill();
            shieldBar.setVisible(true);
        }
    }

    private void hideShieldBar() {
        if (shieldBar != null) {
            shieldBar.setVisible(false);
        }
    }

    private void updateShieldBar() {
        if (shieldBar != null && shieldBar.isVisible()) {
            // Position shield bar above player health bar
            PlayerComponent playerComponent = entity.getComponent(PlayerComponent.class);
            double xPos = entity.getX() + (entity.getWidth() / 2) - (SHIELD_BAR_WIDTH / 2);
            double yPos = entity.getY() - SHIELD_BAR_Y_OFFSET;
            shieldBar.setPosition(xPos, yPos);
        }
    }

    private void showShieldHitEffect() {
        if (shieldBarFill != null) {
            // Flash the shield bar on hit
            shieldBarFill.setFill(Color.LIGHTBLUE);
            FXGL.getGameTimer().runOnceAfter(() -> {
                shieldBarFill.setFill(Color.BLUE);
            }, Duration.seconds(0.1));
        }
    }

    private void showShieldBreakEffect() {
        // Create shield break particle effect
        ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(30);
        emitter.setSourceImage(FXGL.image("particles/glassparticle.png", 10, 10));
        emitter.setSize(2, 5);
        emitter.setNumParticles(20);
        emitter.setEmissionRate(0.5);
        emitter.setStartColor(new Color(0.3, 0.7, 1.0, 0.8));
        emitter.setEndColor(new Color(0.3, 0.7, 1.0, 0.0));
        emitter.setExpireFunction(i -> Duration.seconds(0.5));

        Entity particleEntity = FXGL.entityBuilder()
                .at(entity.getCenter().subtract(20, 20))
                .with(new ParticleComponent(emitter))
                .zIndex(1000)
                .buildAndAttach();

        FXGL.getGameTimer().runOnceAfter(particleEntity::removeFromWorld, Duration.seconds(0.5));
    }

    private void showShieldRefreshEffect() {
        // Create shield refresh particle effect
        ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(30);
        emitter.setSourceImage(FXGL.image("particles/spark.png", 10, 10));
        emitter.setSize(2, 4);
        emitter.setNumParticles(15);
        emitter.setEmissionRate(0.5);
        emitter.setStartColor(new Color(0.3, 0.7, 1.0, 0.8));
        emitter.setEndColor(new Color(0.3, 0.7, 1.0, 0.0));
        emitter.setExpireFunction(i -> Duration.seconds(0.5));

        Entity particleEntity = FXGL.entityBuilder()
                .at(entity.getCenter().subtract(20, 20))
                .with(new ParticleComponent(emitter))
                .zIndex(1000)
                .buildAndAttach();

        FXGL.getGameTimer().runOnceAfter(particleEntity::removeFromWorld, Duration.seconds(0.5));
    }

    public int getCurrentShieldAmount() {
        return currentShieldAmount;
    }

    public double getDamageReductionPercent() {
        return level > 0 && level <= 5 ? DAMAGE_REDUCTION_PERCENT[level - 1] : DAMAGE_REDUCTION_PERCENT[0];
    }

    public double getRefreshInterval() {
        return level > 0 && level <= 5 ? REFRESH_INTERVAL[level - 1] : REFRESH_INTERVAL[0];
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public void onRemoved() {
        if (shieldBar != null) {
            shieldBar.removeFromWorld();
        }
    }

    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;
        return switch (level) {
            case 1 -> "Deploys a shield with 15 HP, 10% damage reduction (15s refresh)";
            case 2 -> "Increases shield capacity to 20 HP";
            case 3 -> "Increases shield capacity to 25 HP, reduces refresh interval to 12s";
            case 4 -> "Increases shield capacity to 30 HP, increases damage reduction to 15%";
            case 5 -> "Increases shield capacity to 35 HP, increases damage reduction to 20%, reduces refresh interval to 10s";
            default -> level > 5 ? "MAXED OUT" : "Deploys a protective shield";
        };
    }
}