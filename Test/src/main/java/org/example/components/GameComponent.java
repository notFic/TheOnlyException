package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import org.example.core.EntityType;
import org.example.interfaces.IDamageable;

/**
 * Abstract base class for all game components with common functionality.
 * Extends the FXGL Component class to provide standardized behavior.
 */
public abstract class GameComponent extends Component implements IDamageable {
    
    // Common references
    protected Entity player;
    protected boolean isActive = true;
    
    // Common properties
    protected int damage = 0;
    protected int health = 1;
    protected double speed = 1.0;
    
    /**
     * Called when the component is added to an entity.
     * Performs common initialization.
     */
    @Override
    public void onAdded() {
        // Find player entity (most components need reference to player)
        this.player = FXGL.getGameWorld()
                .getEntitiesByType(EntityType.PLAYER)
                .stream()
                .findFirst()
                .orElse(null);
        
        // Call component-specific initialization
        initialize();
    }
    
    /**
     * Abstract method for component-specific initialization.
     * Child classes must implement this instead of overriding onAdded.
     */
    protected abstract void initialize();
    
    /**
     * Default update behavior for components.
     * @param tpf time per frame
     */
    @Override
    public void onUpdate(double tpf) {
        if (!isActive || entity == null || !entity.isActive()) {
            return;
        }
        
        // Call component-specific update logic
        updateComponent(tpf);
    }
    
    /**
     * Abstract method for component-specific update logic.
     * Child classes must implement this instead of overriding onUpdate.
     * @param tpf time per frame
     */
    protected abstract void updateComponent(double tpf);
    
    /**
     * Gets the current position of this entity.
     * @return Point2D representing the position
     */
    protected Point2D getPosition() {
        return entity.getPosition();
    }
    
    /**
     * Gets the center position of this entity.
     * @return Point2D representing the center
     */
    protected Point2D getCenter() {
        return entity.getCenter();
    }
    
    /**
     * Gets the distance to another entity.
     * @param other the other entity
     * @return distance as a double
     */
    protected double distanceTo(Entity other) {
        if (other == null || !other.isActive()) {
            return Double.MAX_VALUE;
        }
        return getCenter().distance(other.getCenter());
    }
    
    /**
     * Gets the direction to another entity.
     * @param other the other entity
     * @return normalized direction vector
     */
    protected Point2D directionTo(Entity other) {
        if (other == null || !other.isActive()) {
            return Point2D.ZERO;
        }
        return other.getCenter().subtract(getCenter()).normalize();
    }
    
    /**
     * Move entity in the specified direction.
     * @param direction normalized direction vector
     * @param speedMultiplier speed multiplier
     */
    protected void moveInDirection(Point2D direction, double speedMultiplier) {
        entity.translate(direction.multiply(speed * speedMultiplier));
    }
    
    /**
     * Checks if this entity is colliding with another entity.
     * @param other the other entity
     * @return true if colliding
     */
    protected boolean isCollidingWith(Entity other) {
        return entity.isColliding(other);
    }
    
    /**
     * Sets the damage value for this component.
     * @param damage damage value
     */
    public void setDamage(int damage) {
        this.damage = damage;
    }
    
    /**
     * Gets the current damage value.
     * @return damage value
     */
    public int getDamage() {
        return damage;
    }
    
    /**
     * Sets the health value for this component.
     * @param health health value
     */
    public void setHealth(int health) {
        this.health = health;
    }
    
    /**
     * Gets the current health value.
     * @return health value
     */
    @Override
    public int getHealth() {
        return health;
    }
    
    /**
     * Deactivates this component.
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * Activates this component.
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * Checks if the component is active.
     * @return true if active
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Implementation of IDamageable.damage method.
     * Apply damage to this entity.
     * 
     * @param amount Amount of damage to apply
     * @param source Source of the damage (can be null)
     * @param hitPosition Position where the hit occurred
     * @return Actual amount of damage applied
     */
    @Override
    public double damage(double amount, Object source, Point2D hitPosition) {
        // Default implementation - subclasses should override for specific behavior
        int previousHealth = health;
        health -= amount;
        
        if (health <= 0) {
            health = 0;
        }
        
        return previousHealth - health;
    }
    
    /**
     * Implementation of IDamageable.isDestroyed method.
     * 
     * @return true if the entity's health is 0 or less
     */
    @Override
    public boolean isDestroyed() {
        return health <= 0;
    }
} 