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
    private static final double SHIELD_REFRESH_INTERVAL = 15.0; // Seconds between shield refreshes
    private static final double DAMAGE_REDUCTION_PERCENT = 10.0; // Damage reduction percentage
    
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
        activatePowerUp();
    }
    
    public void activatePowerUp() {
        if (isActive) return;
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
        if (refreshTimer >= SHIELD_REFRESH_INTERVAL) {
            refreshTimer = 0.0;
            resetShield();
        }
        
        // Update shield bar position
        updateShieldBar();
    }
    
    private void resetShield() {
        if (level < 1 || level > 5) {
            currentShieldAmount = 0;
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
        
        // First apply damage reduction
        int reducedDamage = (int) Math.ceil(incomingDamage * (1 - DAMAGE_REDUCTION_PERCENT / 100.0));
        
        // Then absorb with shield
        if (currentShieldAmount >= reducedDamage) {
            // Shield can absorb all damage
            currentShieldAmount -= reducedDamage;
            showShieldHitEffect();
            updateShieldBarFill();
            
            if (currentShieldAmount <= 0) {
                hideShieldBar();
            }
            
            return 0; // No damage passes through
        } else {
            // Shield only absorbs part of the damage
            int remainingDamage = reducedDamage - currentShieldAmount;
            showShieldBreakEffect();
            currentShieldAmount = 0;
            hideShieldBar();
            
            return remainingDamage; // Return damage that passes through
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
            double shieldPercentage = Math.max(0, currentShieldAmount) / (double) maxShieldAmount;
            shieldBarFill.setWidth(SHIELD_BAR_WIDTH * shieldPercentage);
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
            double yPos = entity.getY() - SHIELD_BAR_Y_OFFSET; // Position above the player
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
        return DAMAGE_REDUCTION_PERCENT;
    }
    
    @Override
    public void onRemoved() {
        if (shieldBar != null) {
            shieldBar.removeFromWorld();
        }
    }
} 