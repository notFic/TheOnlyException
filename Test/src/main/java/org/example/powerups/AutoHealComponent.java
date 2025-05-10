package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import org.example.components.PlayerComponent;
import org.example.core.GameApp;

public class AutoHealComponent extends Component {
    private boolean isActive = false;
    private int level = 0;
    private PlayerComponent playerComponent;
    private GameApp gameApp;

    @Override
    public void onAdded() {
        playerComponent = entity.getComponent(PlayerComponent.class);
        gameApp = playerComponent.getGameApp();
        level = playerComponent.getWeaponLevel("auto_heal");
        activatePowerUp();
    }

    public void activatePowerUp() {
        if (isActive) return;
        isActive = true;
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;
    }

    public static String getLevelDescription(int level, boolean nextLevel) {
        if (nextLevel) level++;
        return switch (level) {
            case 1 -> "Enemies have 5% chance to drop healing food";
            case 2 -> "Enemies have 10% chance to drop healing food";
            case 3 -> "Enemies have 15% chance to drop healing food";
            case 4 -> "Enemies have 20% chance to drop healing food";
            case 5 -> "Enemies have 25% chance to drop healing food";
            default -> level > 5 ? "MAXED OUT" : "Enemies have a chance to drop healing food";
        };
    }
}