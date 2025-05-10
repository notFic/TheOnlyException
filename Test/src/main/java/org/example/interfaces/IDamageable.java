package org.example.interfaces;

import javafx.geometry.Point2D;

/**
 * Interface for game entities that can take damage.
 * This provides a standard way to handle damage across different entity types.
 */
public interface IDamageable {
    /**
     * Apply damage to this entity.
     * @param amount Amount of damage to apply
     * @param source Source of the damage (can be null)
     * @param hitPosition Position where the hit occurred
     * @return Actual amount of damage applied (may differ due to resistance, etc.)
     */
    double damage(double amount, Object source, Point2D hitPosition);
    
    /**
     * Get the current health of this entity.
     * @return Current health value
     */
    int getHealth();
    
    /**
     * Check if this entity is destroyed (health <= 0).
     * @return true if entity is destroyed
     */
    boolean isDestroyed();
} 