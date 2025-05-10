package org.example.interfaces;

import javafx.geometry.Point2D;

/**
 * Interface for game entities that can move.
 * This provides a standard way to handle movement across different entity types.
 */
public interface IMovable {
    /**
     * Move the entity in a specified direction.
     * @param direction Normalized direction vector
     * @param speed Speed multiplier for this movement
     */
    void move(Point2D direction, double speed);
    
    /**
     * Get the current movement speed of this entity.
     * @return Base movement speed
     */
    double getSpeed();
    
    /**
     * Set the base movement speed of this entity.
     * @param speed New base speed
     */
    void setSpeed(double speed);
    
    /**
     * Apply a movement modifier (boost, slow, etc).
     * @param modifier Modification factor (1.0 = normal speed)
     * @param durationMillis Duration in milliseconds, or -1 for permanent
     */
    void applySpeedModifier(double modifier, long durationMillis);
} 