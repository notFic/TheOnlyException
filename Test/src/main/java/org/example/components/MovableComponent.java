package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import org.example.interfaces.IMovable;

import java.util.HashMap;
import java.util.Map;

/**
 * A base component for any entity that can move in the game.
 * Implements the IMovable interface for standardized movement behavior.
 */
public abstract class MovableComponent extends GameComponent implements IMovable {
    
    protected double baseSpeed;
    protected double currentSpeedModifier = 1.0;
    protected Map<String, SpeedModifier> activeModifiers = new HashMap<>();
    
    /**
     * Create a new movable component with the specified base speed.
     * @param baseSpeed Initial movement speed
     */
    public MovableComponent(double baseSpeed) {
        this.baseSpeed = baseSpeed;
        this.speed = baseSpeed;
    }
    
    /**
     * Implementation of IMovable.move
     * Move the entity in the specified direction with the specified speed multiplier.
     * 
     * @param direction Normalized direction vector
     * @param speedMultiplier Speed multiplier for this movement
     */
    @Override
    public void move(Point2D direction, double speedMultiplier) {
        if (direction.equals(Point2D.ZERO)) {
            return;
        }
        
        Point2D normalizedDirection = direction.normalize();
        entity.translate(normalizedDirection.multiply(speed * speedMultiplier));
    }
    
    /**
     * Implementation of IMovable.getSpeed
     * Get the current base speed (without modifiers).
     * 
     * @return Current base speed
     */
    @Override
    public double getSpeed() {
        return baseSpeed;
    }
    
    /**
     * Get the actual current speed with all modifiers applied.
     * @return Current speed with modifiers
     */
    public double getCurrentSpeed() {
        return baseSpeed * currentSpeedModifier;
    }
    
    /**
     * Implementation of IMovable.setSpeed
     * Set a new base speed for the entity.
     * 
     * @param speed New base speed
     */
    @Override
    public void setSpeed(double speed) {
        this.baseSpeed = speed;
        updateCurrentSpeed();
    }
    
    /**
     * Implementation of IMovable.applySpeedModifier
     * Apply a speed modifier for a specified duration.
     * 
     * @param modifier Modification factor (1.0 = normal speed)
     * @param durationMillis Duration in milliseconds, or -1 for permanent
     */
    @Override
    public void applySpeedModifier(double modifier, long durationMillis) {
        String id = "mod_" + System.currentTimeMillis();
        SpeedModifier speedMod = new SpeedModifier(modifier, id);
        activeModifiers.put(id, speedMod);
        
        updateCurrentSpeed();
        
        // If not permanent, set a timer to remove the modifier
        if (durationMillis > 0) {
            FXGL.getGameTimer().runOnceAfter(() -> {
                activeModifiers.remove(id);
                updateCurrentSpeed();
            }, Duration.millis(durationMillis));
        }
    }
    
    /**
     * Remove a specific speed modifier by ID.
     * @param id Modifier ID to remove
     */
    public void removeSpeedModifier(String id) {
        activeModifiers.remove(id);
        updateCurrentSpeed();
    }
    
    /**
     * Clear all speed modifiers and reset to base speed.
     */
    public void clearSpeedModifiers() {
        activeModifiers.clear();
        currentSpeedModifier = 1.0;
        speed = baseSpeed;
    }
    
    /**
     * Recalculate the current speed based on all active modifiers.
     */
    private void updateCurrentSpeed() {
        // Start with the base speed multiplier
        currentSpeedModifier = 1.0;
        
        // Apply all active modifiers
        for (SpeedModifier mod : activeModifiers.values()) {
            currentSpeedModifier *= mod.getModifier();
        }
        
        // Update the actual speed
        speed = baseSpeed * currentSpeedModifier;
    }
    
    /**
     * Inner class to represent a speed modifier.
     */
    private static class SpeedModifier {
        private final double modifier;
        private final String id;
        
        public SpeedModifier(double modifier, String id) {
            this.modifier = modifier;
            this.id = id;
        }
        
        public double getModifier() {
            return modifier;
        }
        
        public String getId() {
            return id;
        }
    }
} 