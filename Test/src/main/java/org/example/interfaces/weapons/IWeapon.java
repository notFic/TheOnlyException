package org.example.interfaces.weapons;

import javafx.geometry.Point2D;

/**
 * Interface for weapons that can attack targets.
 */
public interface IWeapon {
    /**
     * Fire the weapon in a specific direction.
     * @param direction Direction to fire
     * @return true if the weapon was fired, false if on cooldown
     */
    boolean fire(Point2D direction);
    
    /**
     * Get the base damage of this weapon.
     * @return Base damage amount
     */
    int getDamage();
    
    /**
     * Get the cooldown between attacks in milliseconds.
     * @return Cooldown in milliseconds
     */
    long getCooldown();
    
    /**
     * Check if the weapon is ready to fire.
     * @return true if weapon can be fired
     */
    boolean isReady();
    
    /**
     * Get the remaining cooldown time in milliseconds.
     * @return Remaining cooldown time
     */
    long getRemainingCooldown();
} 