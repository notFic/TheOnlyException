package org.example.interfaces;

/**
 * Interface for game elements that can be upgraded.
 */
public interface IUpgradable {
    /**
     * Upgrade this element to the next level.
     * @return true if the upgrade was successful
     */
    boolean upgrade();
    
    /**
     * Get the current upgrade level.
     * @return Current level
     */
    int getLevel();
    
    /**
     * Get the maximum possible upgrade level.
     * @return Maximum level
     */
    int getMaxLevel();
    
    /**
     * Check if this element can be upgraded further.
     * @return true if upgrades are available
     */
    boolean canUpgrade();
} 