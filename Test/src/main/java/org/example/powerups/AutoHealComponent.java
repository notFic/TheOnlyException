package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.example.components.PlayerComponent;
import org.example.core.GameApp;

public class AutoHealComponent extends Component {
    private static final double HEAL_INTERVAL = 3.0; // Seconds between heals
    private static final int BASE_HEAL_AMOUNT = 10; // HP healed normally
    private static final int CRITICAL_HEAL_AMOUNT = 20; // HP healed when health < 30% (60 HP)
    private static final double CRITICAL_HEALTH_THRESHOLD = 0.3; // 30% of max health (60 HP)
    private static final double INVULNERABILITY_DURATION = 2.0; // Seconds of invulnerability
    private static final double INVULNERABILITY_COOLDOWN = 20.0; // Seconds between activations
    private static final double INVULNERABILITY_HEALTH_THRESHOLD = 0.1; // 10% of max health (20 HP)
    private static final int MAX_HEALTH_CAP = 200; // Maximum health cap for healing

    private boolean isActive = false;
    private double healTimer = 0.0;
    private int level = 0;
    private boolean isInvulnerable = false;
    private double invulnerabilityTimer = 0.0;
    private double cooldownTimer = 0.0;
    private PlayerComponent playerComponent;
    private GameApp gameApp;

    @Override
    public void onAdded() {
        playerComponent = entity.getComponent(PlayerComponent.class);
        gameApp = playerComponent.getGameApp();
        level = playerComponent.getWeaponLevel("auto_heal");
        activatePowerUp();
    }

    public void activatePowerUp() {
        if (isActive) return;
        isActive = true;
        healTimer = 0.0;
        cooldownTimer = INVULNERABILITY_COOLDOWN; // Allow immediate use
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        healTimer += tpf;

        if (healTimer >= HEAL_INTERVAL) {
            healTimer = 0.0;
            int healAmount = getHealAmount();
            heal(healAmount);
        }

        if (isInvulnerable) {
            invulnerabilityTimer += tpf;
            if (invulnerabilityTimer >= INVULNERABILITY_DURATION) {
                isInvulnerable = false;
                invulnerabilityTimer = 0.0;
                cooldownTimer = 0.0;
                FXGL.getNotificationService().pushNotification("Safe Mode ended");
            }
        } else if (cooldownTimer < INVULNERABILITY_COOLDOWN) {
            cooldownTimer += tpf;
        }
    }

    private int getHealAmount() {
        boolean isCritical = playerComponent.getHealth() <= MAX_HEALTH_CAP * CRITICAL_HEALTH_THRESHOLD;
        int baseAmount = isCritical ? CRITICAL_HEAL_AMOUNT : BASE_HEAL_AMOUNT;

        // Calculate scaled heal amount based on level
        int scaledAmount = switch (level) {
            case 1 -> baseAmount;
            case 2 -> (int) (baseAmount * 1.2); // +20% heal
            case 3 -> (int) (baseAmount * 1.5); // +50% heal
            case 4 -> (int) (baseAmount * 2.0); // +100% heal
            default -> baseAmount;
        };

        // If scaled heal would push health above 200, revert to default amount
        int currentHealth = playerComponent.getHealth();
        if (currentHealth + scaledAmount > MAX_HEALTH_CAP) {
            return baseAmount;
        }

        return scaledAmount;
    }

    private void heal(int amount) {
        int currentHealth = playerComponent.getHealth();
        int newHealth = Math.min(currentHealth + amount, MAX_HEALTH_CAP); // Cap at 200 HP
        playerComponent.damage(-(newHealth - currentHealth)); // Use damage() with negative value to heal
        FXGL.getWorldProperties().setValue("health", newHealth);

        // Explicitly update health bar and UI
        if (gameApp != null) {
            gameApp.updateExpBar(); // Ensure UI reflects health changes
        }

        // Show healing text and particles
        showHealText(newHealth - currentHealth);
        showHealParticles();
    }

    private void showHealText(int amount) {
        var healText = FXGL.getUIFactoryService().newText("+" + amount + " HP", Color.GREEN, 22);
        var textEntity = FXGL.entityBuilder()
                .at(entity.getPosition().subtract(0, 40)) // Position above player
                .view(healText)
                .zIndex(1000)
                .buildAndAttach();
        FXGL.animationBuilder()
                .duration(Duration.seconds(1.2))
                .translate(textEntity)
                .from(textEntity.getPosition())
                .to(textEntity.getPosition().subtract(0, 50))
                .build().start();
        FXGL.animationBuilder()
                .duration(Duration.seconds(1.2))
                .fadeOut(textEntity)
                .build().start();
        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), Duration.seconds(1.2));
    }

    private void showHealParticles() {
        ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(40);
        try {
            emitter.setSourceImage(FXGL.image("green_particle.png"));
        } catch (Exception e) {
            System.err.println("Warning: green_particle.png not found, using default particle effect");
        }
        emitter.setSize(3, 6);
        emitter.setNumParticles(20);
        emitter.setEmissionRate(0.6);
        emitter.setStartColor(new Color(0.0, 1.0, 0.0, 0.8));
        emitter.setEndColor(new Color(0.0, 1.0, 0.0, 0.0));
        emitter.setExpireFunction(i -> Duration.seconds(0.5));
        Entity particleEntity = FXGL.entityBuilder()
                .at(entity.getCenter().subtract(25, 25))
                .with(new ParticleComponent(emitter))
                .zIndex(1000)
                .buildAndAttach();
        FXGL.getGameTimer().runOnceAfter(particleEntity::removeFromWorld, Duration.seconds(0.5));
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void triggerInvulnerability() {
        if (level >= 2 && !isInvulnerable && cooldownTimer >= INVULNERABILITY_COOLDOWN) {
            isInvulnerable = true;
            invulnerabilityTimer = 0.0;
            FXGL.getNotificationService().pushNotification("Safe Mode: Invulnerable for 2 seconds!");
        }
    }

    public boolean canTriggerInvulnerability() {
        return level >= 2 && !isInvulnerable && cooldownTimer >= INVULNERABILITY_COOLDOWN;
    }

    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;
        return switch (level) {
            case 1 -> "Heals 10 HP every 3s (20 HP if <60 HP), caps at 200 HP";
            case 2 -> "Gain Safe Mode: Invulnerable for 2s when <20 HP (20s cooldown)";
            case 3 -> "Increase healing by 20% (12/24 HP), resets to 10/20 HP if >200 HP";
            case 4 -> "Increase healing by 50% (15/30 HP), resets to 10/20 HP if >200 HP";
            case 5 -> "Increase healing by 100% (20/40 HP), resets to 10/20 HP if >200 HP";
            default -> level > 5 ? "MAXED OUT" : "Heals periodically, caps at 200 HP";
        };
    }
}