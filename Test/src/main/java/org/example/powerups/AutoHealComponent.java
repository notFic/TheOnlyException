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

public class AutoHealComponent extends Component {
    private static final double HEAL_INTERVAL = 5.0; // Seconds between heals
    private static final int BASE_HEAL_AMOUNT = 5; // HP healed normally
    private static final int CRITICAL_HEAL_AMOUNT = 10; // HP healed when health < 50%
    private static final double CRITICAL_HEALTH_THRESHOLD = 0.5; // 50% of max health (50 HP)
    private static final double INVULNERABILITY_DURATION = 2.0; // Seconds of invulnerability
    private static final double INVULNERABILITY_COOLDOWN = 30.0; // Seconds between activations
    private static final double INVULNERABILITY_HEALTH_THRESHOLD = 0.1; // 10% of max health (10 HP)
    private static final int MAX_HEALTH = 100; // Fixed max health from PlayerComponent

    private boolean isActive = false;
    private double healTimer = 0.0;
    private int level = 0;
    private boolean isInvulnerable = false;
    private double invulnerabilityTimer = 0.0;
    private double cooldownTimer = 0.0;

    @Override
    public void onAdded() {
        level = entity.getComponent(PlayerComponent.class).getWeaponLevel("auto_heal");
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

        PlayerComponent player = entity.getComponent(PlayerComponent.class);
        healTimer += tpf;

        if (healTimer >= HEAL_INTERVAL) {
            healTimer = 0.0;
            int healAmount = player.getHealth() <= MAX_HEALTH * CRITICAL_HEALTH_THRESHOLD
                    ? CRITICAL_HEAL_AMOUNT
                    : BASE_HEAL_AMOUNT;
            heal(player, healAmount);
        }

        if (isInvulnerable) {
            invulnerabilityTimer += tpf;
            if (invulnerabilityTimer >= INVULNERABILITY_DURATION) {
                isInvulnerable = false;
                invulnerabilityTimer = 0.0;
                cooldownTimer = 0.0;
            }
        } else if (cooldownTimer < INVULNERABILITY_COOLDOWN) {
            cooldownTimer += tpf;
        }
    }

    private void heal(PlayerComponent player, int amount) {
        int currentHealth = player.getHealth();
        int newHealth = Math.min(currentHealth + amount, MAX_HEALTH);
        FXGL.getWorldProperties().setValue("health", newHealth);

        // Show healing text
        showHealText(amount);

        // Particle effect for healing
        ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(30);
        try {
            emitter.setSourceImage(FXGL.image("green_particle.png"));
        } catch (Exception e) {
            System.err.println("Warning: green_particle.png not found, using default particle effect");
        }
        emitter.setSize(2, 4);
        emitter.setNumParticles(15);
        emitter.setEmissionRate(0.5);
        emitter.setExpireFunction(i -> Duration.seconds(0.3));
        Entity particleEntity = FXGL.entityBuilder()
                .at(entity.getCenter().subtract(20, 20))
                .with(new ParticleComponent(emitter))
                .zIndex(1000)
                .buildAndAttach();
        FXGL.getGameTimer().runOnceAfter(particleEntity::removeFromWorld, Duration.seconds(0.3));
    }

    private void showHealText(int amount) {
        var healText = FXGL.getUIFactoryService().newText("+" + amount + " HP", Color.GREEN, 18);
        var textEntity = FXGL.entityBuilder()
                .at(entity.getPosition().subtract(0, 30))
                .view(healText)
                .buildAndAttach();
        FXGL.animationBuilder()
                .duration(Duration.seconds(1))
                .translate(textEntity)
                .from(textEntity.getPosition())
                .to(textEntity.getPosition().subtract(0, 30))
                .build().start();
        FXGL.animationBuilder()
                .duration(Duration.seconds(1))
                .fadeOut(textEntity)
                .build().start();
        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), Duration.seconds(1));
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void triggerInvulnerability() {
        if (level >= 2 && !isInvulnerable && cooldownTimer >= INVULNERABILITY_COOLDOWN) {
            isInvulnerable = true;
            invulnerabilityTimer = 3.0;
            FXGL.getNotificationService().pushNotification("Safe Mode: Invulnerable for 2 seconds!");
        }
    }

    public boolean canTriggerInvulnerability() {
        return level >= 2 && !isInvulnerable && cooldownTimer >= INVULNERABILITY_COOLDOWN;
    }
}