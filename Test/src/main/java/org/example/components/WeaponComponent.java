package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Point2D;
import org.example.interfaces.weapons.IWeapon;

/**
 * Base component for all weapons in the game.
 * Implements IWeapon interface to standardize weapon behavior.
 */
public abstract class WeaponComponent extends GameComponent implements IWeapon {
    
    protected double baseDamage;
    protected long cooldownMillis;
    protected long lastFiredTime = 0;
    
    /**
     * Create a new weapon component.
     * @param baseDamage Base damage of the weapon
     * @param cooldownMillis Cooldown between shots in milliseconds
     */
    public WeaponComponent(double baseDamage, long cooldownMillis) {
        this.baseDamage = baseDamage;
        this.cooldownMillis = cooldownMillis;
    }
    
    @Override
    protected void initialize() {
        // Default initialization - subclasses should override if needed
    }
    
    /**
     * Implementation of IWeapon.fire
     * Fire the weapon in the specified direction.
     * 
     * @param direction Direction to fire in
     * @return true if the weapon was fired, false if on cooldown
     */
    @Override
    public boolean fire(Point2D direction) {
        // Check if weapon is ready to fire
        if (!isReady()) {
            return false;
        }
        
        // Update the last fired time
        lastFiredTime = System.currentTimeMillis();
        
        // Call the implementation-specific fire method
        return doFire(direction);
    }
    
    /**
     * Implementation-specific firing logic.
     * @param direction Direction to fire in
     * @return true if the weapon was fired successfully
     */
    protected abstract boolean doFire(Point2D direction);
    
    /**
     * Implementation of IWeapon.getDamage
     * Get the base damage of this weapon.
     * 
     * @return Base damage value
     */
    @Override
    public int getDamage() {
        return (int)baseDamage;
    }
    
    /**
     * Get the precise double damage value of this weapon.
     * @return Base damage as a double
     */
    public double getPreciseDamage() {
        return baseDamage;
    }
    
    /**
     * Set a new base damage for this weapon.
     * @param damage New base damage
     */
    public void setBaseDamage(double damage) {
        this.baseDamage = damage;
    }
    
    /**
     * Implementation of IWeapon.getCooldown
     * Get the cooldown between shots in milliseconds.
     * 
     * @return Cooldown in milliseconds
     */
    @Override
    public long getCooldown() {
        return cooldownMillis;
    }
    
    /**
     * Set a new cooldown for this weapon.
     * @param cooldownMillis New cooldown in milliseconds
     */
    public void setCooldown(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }
    
    /**
     * Implementation of IWeapon.isReady
     * Check if the weapon is ready to fire.
     * 
     * @return true if weapon can be fired
     */
    @Override
    public boolean isReady() {
        return System.currentTimeMillis() - lastFiredTime >= cooldownMillis;
    }
    
    /**
     * Implementation of IWeapon.getRemainingCooldown
     * Get the remaining cooldown time in milliseconds.
     * 
     * @return Remaining cooldown time in milliseconds
     */
    @Override
    public long getRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastFiredTime;
        return Math.max(0, cooldownMillis - elapsed);
    }
    
    /**
     * Get the cooldown progress as a value between 0.0 and 1.0.
     * 1.0 means the weapon is ready to fire, less than 1.0 means it's still cooling down.
     * 
     * @return Cooldown progress (0.0 to 1.0)
     */
    public double getCooldownProgress() {
        if (cooldownMillis <= 0) {
            return 1.0;
        }
        
        long elapsed = System.currentTimeMillis() - lastFiredTime;
        return Math.min(1.0, (double) elapsed / cooldownMillis);
    }
    
    @Override
    protected void updateComponent(double tpf) {
        // Default update behavior - subclasses should override if needed
    }
} 