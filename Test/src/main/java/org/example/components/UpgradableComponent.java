package org.example.components;

import com.almasb.fxgl.entity.component.Component;
import org.example.interfaces.IUpgradable;

/**
 * A component that can be upgraded over time.
 * Implements the IUpgradable interface for standardized upgrade behavior.
 */
public abstract class UpgradableComponent extends Component implements IUpgradable {
    
    protected int level = 1;
    protected int maxLevel;
    protected double[] levelBonuses;
    
    /**
     * Create a new upgradable component.
     * 
     * @param maxLevel Maximum level this component can reach
     * @param levelBonuses Array of bonus values for each level (index 0 = level 1)
     */
    public UpgradableComponent(int maxLevel, double[] levelBonuses) {
        this.maxLevel = maxLevel;
        this.levelBonuses = levelBonuses;
    }
    
    /**
     * Implementation of IUpgradable.upgrade
     * Upgrade this component to the next level.
     * 
     * @return true if the upgrade was successful
     */
    @Override
    public boolean upgrade() {
        if (!canUpgrade()) {
            return false;
        }
        
        level++;
        onUpgrade();
        return true;
    }
    
    /**
     * Implementation of IUpgradable.getLevel
     * Get the current level of this component.
     * 
     * @return Current level
     */
    @Override
    public int getLevel() {
        return level;
    }
    
    /**
     * Implementation of IUpgradable.getMaxLevel
     * Get the maximum possible level for this component.
     * 
     * @return Maximum level
     */
    @Override
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Implementation of IUpgradable.canUpgrade
     * Check if this component can be upgraded further.
     * 
     * @return true if upgrades are available
     */
    @Override
    public boolean canUpgrade() {
        return level < maxLevel;
    }
    
    /**
     * Get the current bonus value for the component's level.
     * @return Current level's bonus value
     */
    public double getCurrentLevelBonus() {
        if (levelBonuses == null || levelBonuses.length == 0) {
            return 0;
        }
        
        int index = Math.min(level - 1, levelBonuses.length - 1);
        return levelBonuses[index];
    }
    
    /**
     * Get the bonus value for a specific level.
     * @param targetLevel The level to get the bonus for
     * @return The bonus value for the specified level
     */
    public double getBonusForLevel(int targetLevel) {
        if (levelBonuses == null || levelBonuses.length == 0 || targetLevel < 1) {
            return 0;
        }
        
        int index = Math.min(targetLevel - 1, levelBonuses.length - 1);
        return levelBonuses[index];
    }
    
    /**
     * Method called when this component is upgraded.
     * Subclasses should override this to implement specific upgrade behavior.
     */
    protected abstract void onUpgrade();
} 