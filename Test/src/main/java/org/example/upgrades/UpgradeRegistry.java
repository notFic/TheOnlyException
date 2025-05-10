package org.example.upgrades;

import javafx.scene.paint.Color;
import org.example.components.PlayerComponent;
import org.example.model.OptionType;
import org.example.model.UpgradeOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// LIST OF AVAILABLE UPGRADES
public class UpgradeRegistry {
    // Singleton instance
    private static UpgradeRegistry instance;

    // List of all available upgrades
    private final List<UpgradeOption> allUpgrades;

    private UpgradeRegistry() {
        allUpgrades = Arrays.asList(
// Weapons
                new UpgradeOption("gun", "Null Blaster", "Your primary weapon that shoots bursts of bullets", Color.WHITE, OptionType.WEAPON, "nullblaster.png"),
                //new UpgradeOption("lightning", "Short Circuit", "Strikes random enemies with volts", Color.BLUE, OptionType.WEAPON, "shortcircuit.png"),
                //new UpgradeOption("poison", "Real-Time Defense", "Damages enemies within range", Color.GREENYELLOW, OptionType.WEAPON, "realtimedefense.png"),
                //new UpgradeOption("fire_trail", "Smolder Protocol", "Damages enemies standing on the trail over time", Color.RED, OptionType.WEAPON, "smolderprotocol.png"),
                new UpgradeOption("explosive_mines", "Data Wipe", "Spawns memory leak zones with a countdown that explodes", Color.ORANGE, OptionType.WEAPON, "datawipe.png"),
                // Powerups
                new UpgradeOption("shield", "Firewall Shield", "Absorbs damage and reduces damage taken by 10%", Color.CYAN, OptionType.POWERUP, "firewallshield.png"),
                new UpgradeOption("auto_heal", "System Restore", "Periodically repairs the player's system", Color.LIMEGREEN, OptionType.POWERUP, "systemrestore.png")
        );
    }

    // Get the singleton instance
    public static UpgradeRegistry getInstance() {
        if (instance == null) {
            instance = new UpgradeRegistry();
        }
        return instance;
    }

    // Get all upgrades
    public List<UpgradeOption> getAllUpgrades() {
        return Collections.unmodifiableList(allUpgrades);
    }

    // Get upgrades of specific type
    public List<UpgradeOption> getUpgradesByType(OptionType type) {
        return allUpgrades.stream()
            .filter(upgrade -> upgrade.getType() == type)
            .collect(Collectors.toList());
    }

    // Find upgrade by ID
    public UpgradeOption getUpgradeById(String id) {
        return allUpgrades.stream()
                .filter(upgrade -> upgrade.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Get random selection of upgrade options
    public List<UpgradeOption> getRandomUpgradeOptions(PlayerComponent playerComponent, int count) {
        List<UpgradeOption> allOptions = new ArrayList<>(allUpgrades);

        // Prioritize upgrades the player already has
        List<UpgradeOption> playerUpgrades = new ArrayList<>();
        List<UpgradeOption> weaponOptions = new ArrayList<>();
        List<UpgradeOption> powerupOptions = new ArrayList<>();

        for (UpgradeOption option : allOptions) {
            int upgradeLevel = playerComponent.getWeaponLevel(option.getId());
            
            // Skip maxed upgrades
            if (isMaxed(option.getId(), upgradeLevel)) {
                continue;
            }
            
            if (upgradeLevel > 0) {
                playerUpgrades.add(option);
            } else {
                if (option.getType() == OptionType.WEAPON) {
                    weaponOptions.add(option);
                } else {
                    powerupOptions.add(option);
                }
            }
        }

        // Shuffle all lists
        Collections.shuffle(playerUpgrades, new Random());
        Collections.shuffle(weaponOptions, new Random());
        Collections.shuffle(powerupOptions, new Random());

        // Create the result list, starting with at least one upgrade the player already has (if possible)
        List<UpgradeOption> result = new ArrayList<>();

        // Add one upgrade the player already has (if any)
        if (!playerUpgrades.isEmpty()) {
            result.add(playerUpgrades.remove(0));
        }

        // Ensure at least one weapon option
        if (result.isEmpty() || result.stream().noneMatch(o -> o.getType() == OptionType.WEAPON)) {
            if (!weaponOptions.isEmpty()) {
                result.add(weaponOptions.remove(0));
            }
        }

        // Ensure at least one powerup option
        if (result.stream().noneMatch(o -> o.getType() == OptionType.POWERUP)) {
            if (!powerupOptions.isEmpty()) {
                result.add(powerupOptions.remove(0));
            }
        }

        // Combine remaining options
        List<UpgradeOption> remainingOptions = new ArrayList<>();
        remainingOptions.addAll(playerUpgrades);
        remainingOptions.addAll(weaponOptions);
        remainingOptions.addAll(powerupOptions);
        Collections.shuffle(remainingOptions, new Random());

        // Fill the remaining slots
        while (result.size() < count && !remainingOptions.isEmpty()) {
            result.add(remainingOptions.remove(0));
        }

        // Shuffle the final selection
        Collections.shuffle(result, new Random());

        return result;
    }

    // Helper method to check if an upgrade is maxed
    private boolean isMaxed(String upgradeId, int currentLevel) {
        return switch (upgradeId) {
            case "shield", "auto_heal" -> currentLevel >= 5;
            case "explosive_mines" -> currentLevel >= 6;
            case "gun", "lightning", "poison", "fire_trail" -> currentLevel >= 7;
            default -> false;
        };
    }
}