
//TRIED TO MAKE IT LESS LAGGY(?)
package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.EntityWorldListener;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import org.example.components.EnemyComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages wave-based enemy spawning system similar to Vampire Survivors.
 * Handles increasing difficulty, enemy variety, and spawn formations.
 *
 * This system:
 * 1. Creates progressively harder waves with more enemies
 * 2. Introduces new enemy types as waves progress
 * 3. Creates special formations at milestone waves (every 5th wave)
 * 4. Scales enemy count and spawn rate over time
 * 5. Uses pooled enemy objects for better performance
 */
public class WaveManager {
    private static WaveManager instance; // Singleton instance
    private final Random random = new Random();
    private boolean isActive = false;
    private int currentWave = 1;
    private Entity player;
    private final List<String> availableEnemyTypes = new ArrayList<>();
    private int activeEnemyCount = 0; // Tracks active enemies

    // Wave configuration parameters
    private final int WAVE_DURATION_SECONDS = 60; // Each wave lasts 60 seconds
    private final int BASE_ENEMIES_PER_WAVE = 20; // Base enemies for wave 1
    private final double ENEMY_INCREASE_FACTOR = 1.3; // Reduced from 1.5 to 1.3 for slower growth
    private final double BASE_SPAWN_INTERVAL = 1.5; // Initial spawn interval
    private final double MIN_SPAWN_INTERVAL = 0.15; // Minimum spawn interval

    // Enemy pool management
    private final Map<Integer, List<String>> waveEnemyPools = new HashMap<>(); // Maps wave number → available enemy types
    private final Map<Integer, Double> waveSpawnRates = new HashMap<>(); // Maps wave number → spawn interval
    private final Map<Integer, Integer> waveMaxEnemies = new HashMap<>(); // Maps wave number → max enemies
    private final Map<Integer, String> waveFormations = new HashMap<>(); // Maps wave number → spawn formation

    // Timer reference for spawning
    private Runnable spawnTask;

    // Listener to track enemy removal
    private final EntityWorldListener enemyRemovalListener = new EntityWorldListener() {
        @Override
        public void onEntityAdded(Entity entity) {
            // No action needed for added entities
        }

        @Override
        public void onEntityRemoved(Entity entity) {
            if (entity.getType() == EntityType.ENEMY) {
                activeEnemyCount--;
                System.out.println("Enemy removed (damage). Active enemies: " + activeEnemyCount);
            }
        }
    };

    // Private constructor to enforce singleton pattern
    private WaveManager() {
        initializeEnemyTypes();
        initializeWaveConfigurations();
    }

    /**
     * Get the singleton instance of WaveManager
     * @return The shared WaveManager instance
     */
    public static WaveManager getInstance() {
        if (instance == null) {
            instance = new WaveManager();
            System.out.println("WaveManager singleton initialized");
        }
        return instance;
    }

    /**
     * Initialize the available enemy types
     * Different enemy types are introduced at different wave numbers
     */
    private void initializeEnemyTypes() {
        availableEnemyTypes.add("enemy");        // Maggot - Basic enemy (Wave 1+)
        availableEnemyTypes.add("fastEnemy");    // Beetle - Fast enemy (Wave 2+)
        availableEnemyTypes.add("tankEnemy");    // Mantis - Tank enemy (Wave 3+)
        availableEnemyTypes.add("beeEnemy");     // Bee - Medium enemy (Wave 4+)
        availableEnemyTypes.add("dragonflyEnemy"); // Dragonfly - Advanced enemy (Wave 5+)
        availableEnemyTypes.add("giantFlyEnemy"); // Giant Fly - Mini boss (Wave 6+)
    }

    /**
     * Initialize wave configurations
     * This sets up:
     * - Which enemy types appear in each wave
     * - How quickly enemies spawn in each wave
     * - Maximum number of enemies in each wave
     * - Special formation patterns for milestone waves
     */
    private void initializeWaveConfigurations() {
        for (int wave = 1; wave <= 20; wave++) {
            List<String> enemyPool = new ArrayList<>();
            if (wave >= 1) enemyPool.add("enemy");
            if (wave >= 2) enemyPool.add("fastEnemy");
            if (wave >= 3) enemyPool.add("tankEnemy");
            if (wave >= 4) enemyPool.add("beeEnemy");
            if (wave >= 5) enemyPool.add("dragonflyEnemy");
            if (wave >= 6) enemyPool.add("giantFlyEnemy");
            waveEnemyPools.put(wave, enemyPool);

            double spawnInterval = Math.max(BASE_SPAWN_INTERVAL * Math.pow(0.85, wave - 1), MIN_SPAWN_INTERVAL);
            waveSpawnRates.put(wave, spawnInterval);

            int maxEnemies = (int)(BASE_ENEMIES_PER_WAVE * Math.pow(ENEMY_INCREASE_FACTOR, wave - 1));
            waveMaxEnemies.put(wave, maxEnemies);

            if (wave % 5 == 0) {
                String[] formations = {"circle", "line", "spiral"};
                waveFormations.put(wave, formations[wave % formations.length]);
            } else {
                waveFormations.put(wave, "random");
            }
        }
    }

    /**
     * Start the wave system with a reference to the player entity
     * @param player The player entity that enemies will target
     */
    public void start(Entity player) {
        this.player = player;
        boolean state = this.player.isActive();
        this.currentWave = 1;
        this.isActive = true;
        this.activeEnemyCount = 0; // Reset counter
        FXGL.getWorldProperties().setValue("wave", currentWave);
        System.out.println("WaveManager started. Player active: " + state + ", Initial wave: " + currentWave);

        // Add listener for enemy removal
        FXGL.getGameWorld().addWorldListener(enemyRemovalListener);

        scheduleEnemySpawning();
        System.out.println("Wave system fully started. Current wave: " + currentWave);
    }

    /**
     * Stop the wave system (used when pausing or ending the game)
     */
    public void stop() {
        this.isActive = false;
        this.activeEnemyCount = 0; // Reset counter
        FXGL.getGameWorld().removeWorldListener(enemyRemovalListener);
        System.out.println("Wave system stopped");
    }

    /**
     * Schedule enemy spawning based on current wave configuration
     */
    private void scheduleEnemySpawning() {
        spawnTask = () -> {
            if (!isActive) return;

            // Check survivalTime for wave transition
            int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");
            int expectedWave = survivalTime / WAVE_DURATION_SECONDS + 1;
            if (expectedWave > currentWave) {
                currentWave = expectedWave;
                FXGL.getWorldProperties().setValue("wave", currentWave);
                System.out.println("Transitioning to Wave " + currentWave + " at survival time: " + survivalTime + "s");
                announceNewWave();
            }

            double spawnInterval = waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL);
            double spawnChance = 0.5 / spawnInterval;
            int spawnCount = 1;

            if (currentWave > 5 && random.nextDouble() < 0.2) {
                spawnCount = Math.min(currentWave / 4, 3);
            }

            if (random.nextDouble() < spawnChance) {
                for (int i = 0; i < spawnCount; i++) {
                    spawnEnemyForCurrentWave(survivalTime);
                }
            }
        };
        FXGL.getGameTimer().runAtInterval(spawnTask, Duration.seconds(0.5));
    }

    /**
     * Spawn an enemy based on the current wave's configuration
     * @param survivalTime Current survival time for scaling
     */
    private void spawnEnemyForCurrentWave(int survivalTime) {
        if (!isActive || player == null) {
            System.out.println("Skipping enemy spawn: WaveManager inactive or player null");
            return;
        }

        // Cleanup old enemies outside viewport
        cleanupOldEnemies();

        List<String> enemyPool = waveEnemyPools.getOrDefault(currentWave, List.of("enemy"));
        String formation = waveFormations.getOrDefault(currentWave, "random");

        int baseMax = waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE);
        double timeScaling = 1.0 + (survivalTime / 600.0);
        int maxEnemies = (int)(baseMax * timeScaling);
        int waveAllowance = Math.min(currentWave * 3, 30);

        if (activeEnemyCount >= maxEnemies + waveAllowance) {
            System.out.println("Enemy spawn skipped: Active enemies (" + activeEnemyCount + ") exceed max (" + (maxEnemies + waveAllowance) + ")");
            return;
        }

        String enemyType;
        if (currentWave >= 6 && random.nextDouble() < 0.05 * (currentWave / 6.0)) {
            int bossIndex = Math.min(enemyPool.size() - 1, enemyPool.size() - 2);
            enemyType = enemyPool.get(Math.max(bossIndex, 0));
        } else {
            enemyType = enemyPool.get(random.nextInt(enemyPool.size()));
        }

        System.out.println("Spawning enemy for Wave " + currentWave + ": Type=" + enemyType + ", Formation=" + formation + ", Active enemies=" + (activeEnemyCount + 1));

        switch (formation) {
            case "circle":
                spawnEnemyInCircleFormation(enemyType);
                break;
            case "line":
                spawnEnemyInLineFormation(enemyType);
                break;
            case "spiral":
                spawnEnemyInSpiralFormation(enemyType);
                break;
            case "random":
            default:
                spawnEnemyOutsideViewport(enemyType);
                break;
        }
    }

    /**
     * Cleanup enemies that are too far from the viewport to reduce entity count
     */
    private void cleanupOldEnemies() {
        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();
        double margin = 1000;

        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY);
        for (Entity enemy : enemies) {
            double x = enemy.getX();
            double y = enemy.getY();
            long spawnTime = enemy.getProperties().exists("spawnTime") ? enemy.getObject("spawnTime") : System.nanoTime();
            if (!enemy.getProperties().exists("spawnTime")) {
                System.out.println("Warning: Enemy at (" + x + ", " + y + ") missing spawnTime");
            }
            long lifetime = System.nanoTime() - spawnTime;
            boolean isOld = lifetime > 30_000_000_000L; // 30 seconds

            if (isOld && (x < viewMinX - margin || x > viewMaxX + margin || y < viewMinY - margin || y > viewMaxY + margin)) {
                enemy.getComponent(EnemyComponent.class).die(); // Use die for cleanup
                activeEnemyCount--; // Decrement counter (listener handles damage-based removal)
                System.out.println("Removed old enemy at (" + x + ", " + y + ") to reduce lag. Active enemies: " + activeEnemyCount);
            }
        }
    }

    /**
     * Announce a new wave with console prints and UI notifications
     */
    private void announceNewWave() {
        System.out.println("=========================================");
        System.out.println("Wave " + currentWave + " approaching!");
        System.out.println("New wave started: " + currentWave);
        System.out.println("Max enemies: " + waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE));
        System.out.println("Spawn interval: " + waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL));
        System.out.println("Enemy types available: " + waveEnemyPools.getOrDefault(currentWave, List.of("enemy")));

        FXGL.getNotificationService().pushNotification("Wave " + currentWave + " started!");

        if (currentWave % 5 == 0) {
            String formation = waveFormations.getOrDefault(currentWave, "random");
            System.out.println("WARNING: Special formation incoming! Formation: " + formation);
            FXGL.getNotificationService().pushNotification("WARNING: Special " + formation + " formation incoming!");

            FXGL.getGameTimer().runOnceAfter(() -> {
                String enemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));
                System.out.println("Spawning special formation for Wave " + currentWave + " with enemy: " + enemyType);

                String formationType = waveFormations.get(currentWave);
                if (formationType == null) formationType = "circle";

                switch (formationType) {
                    case "line":
                        spawnEnemyInLineFormation(enemyType);
                        break;
                    case "spiral":
                        spawnEnemyInSpiralFormation(enemyType);
                        break;
                    case "circle":
                    default:
                        spawnEnemyInCircleFormation(enemyType);
                        break;
                }
            }, Duration.seconds(2));
        }

        if (currentWave % 10 == 0) {
            System.out.println("DANGER: Massive enemy wave approaching!");
            FXGL.getNotificationService().pushNotification("DANGER: Massive enemy wave approaching!");

            FXGL.getGameTimer().runOnceAfter(() -> {
                String enemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));
                System.out.println("Spawning first formation (circle) for Wave " + currentWave + " with enemy: " + enemyType);
                spawnEnemyInCircleFormation(enemyType);
            }, Duration.seconds(3));

            FXGL.getGameTimer().runOnceAfter(() -> {
                String secondEnemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));
                System.out.println("Spawning second formation (spiral) for Wave " + currentWave + " with enemy: " + secondEnemyType);
                spawnEnemyInSpiralFormation(secondEnemyType);
            }, Duration.seconds(6));
        }
        System.out.println("=========================================");
    }

    /**
     * Get the current wave number
     * @return The current wave number
     */
    public int getCurrentWave() {
        return currentWave;
    }

    /**
     * Get the elapsed time in seconds
     * @return Elapsed time in seconds since wave system started
     */
    public int getElapsedTimeSeconds() {
        return FXGL.getWorldProperties().getInt("survivalTime");
    }

    /**
     * Check if the wave system is active
     * @return True if the wave system is currently running
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Reset the wave system
     * Used when starting a new game
     */
    public void reset() {
        currentWave = 1;
        isActive = false;
        activeEnemyCount = 0; // Reset counter
        spawnTask = null;
        FXGL.getWorldProperties().setValue("wave", 1);
        FXGL.getGameWorld().removeWorldListener(enemyRemovalListener);
        System.out.println("WaveManager reset: Wave set to 1, spawnTask cleared");
    }

    /**
     * Spawn an enemy outside the viewport (standard spawning method)
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyOutsideViewport(String enemyType) {
        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        double x, y;
        int margin = 200;

        int side = random.nextInt(4);
        switch (side) {
            case 0: // Top
                x = viewMinX + random.nextDouble() * FXGL.getAppWidth();
                y = viewMinY - margin;
                break;
            case 1: // Right
                x = viewMaxX + margin;
                y = viewMinY + random.nextDouble() * FXGL.getAppHeight();
                break;
            case 2: // Bottom
                x = viewMinX + random.nextDouble() * FXGL.getAppWidth();
                y = viewMaxY + margin;
                break;
            case 3: // Left
            default:
                x = viewMinX - margin;
                y = viewMinY + random.nextDouble() * FXGL.getAppHeight();
                break;
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        Entity enemy = FXGL.getGameWorld().spawn(enemyType, data);
        enemy.setProperty("spawnTime", System.nanoTime());
        activeEnemyCount++; // Increment counter
    }

    /**
     * Spawn enemies in a circle formation around the player
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyInCircleFormation(String enemyType) {
        double radius = 800;
        final int count = currentWave > 5 ? 8 + Math.min((currentWave - 5), 8) : 8;

        Point2D playerPos = player.getPosition();
        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        for (int i = 0; i < count; i++) {
            final int index = i;
            FXGL.getGameTimer().runOnceAfter(() -> {
                double angle = (2 * Math.PI / count) * index;
                double x = playerPos.getX() + radius * Math.cos(angle);
                double y = playerPos.getY() + radius * Math.sin(angle);

                if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                    double adjustedRadius = radius * 1.5;
                    x = playerPos.getX() + adjustedRadius * Math.cos(angle);
                    y = playerPos.getY() + adjustedRadius * Math.sin(angle);
                }

                SpawnData data = new SpawnData(x, y);
                data.put("player", player);
                Entity enemy = FXGL.getGameWorld().spawn(enemyType, data);
                enemy.setProperty("spawnTime", System.nanoTime());
                activeEnemyCount++; // Increment counter
            }, Duration.millis(100 * i));
        }
    }

    /**
     * Spawn enemies in a line formation
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyInLineFormation(String enemyType) {
        double distance = 800;
        int count = 6;
        double spacing = 80;

        if (currentWave > 5) {
            count = 6 + Math.min((currentWave - 5), 4);
        }

        int side = random.nextInt(4);
        final double finalCount = count;
        final double finalSpacing = spacing;
        double startX, startY, dirX = 0, dirY = 0;

        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        switch (side) {
            case 0: // Top
                startX = viewMinX + FXGL.getAppWidth() / 2 - (count * spacing) / 2;
                startY = viewMinY - distance;
                dirX = 1;
                dirY = 0;
                break;
            case 1: // Right
                startX = viewMaxX + distance;
                startY = viewMinY + FXGL.getAppHeight() / 2 - (count * spacing) / 2;
                dirX = 0;
                dirY = 1;
                break;
            case 2: // Bottom
                startX = viewMinX + FXGL.getAppWidth() / 2 - (count * spacing) / 2;
                startY = viewMaxY + distance;
                dirX = 1;
                dirY = 0;
                break;
            case 3: // Left
            default:
                startX = viewMinX - distance;
                startY = viewMinY + FXGL.getAppHeight() / 2 - (count * spacing) / 2;
                dirX = 0;
                dirY = 1;
                break;
        }

        final double finalStartX = startX;
        final double finalStartY = startY;
        final double finalDirX = dirX;
        final double finalDirY = dirY;

        for (int i = 0; i < count; i++) {
            final int index = i;
            FXGL.getGameTimer().runOnceAfter(() -> {
                double x = finalStartX + finalDirX * finalSpacing * index;
                double y = finalStartY + finalDirY * finalSpacing * index;

                SpawnData data = new SpawnData(x, y);
                data.put("player", player);
                Entity enemy = FXGL.getGameWorld().spawn(enemyType, data);
                enemy.setProperty("spawnTime", System.nanoTime());
                activeEnemyCount++; // Increment counter
            }, Duration.millis(150 * i));
        }
    }

/**
 * Spawn enemies in a spiral formation
 * @param enemyType The type of enemy to spawn
 */
private void spawnEnemyInSpiralFormation(String enemyType) {
    final int count = currentWave > 5 ? 10 + Math.min((currentWave - 5), 10) : 10;
    double baseRadius = 700;
    double radiusIncrement = 40;

    Point2D playerPos = player.getPosition();
    double viewMinX = FXGL.getGameScene().getViewport().getX();
    double viewMinY = FXGL.getGameScene().getViewport().getY();
    double viewMaxX = viewMinX + FXGL.getAppWidth();
    double viewMaxY = viewMinY + FXGL.getAppHeight();

    for (int i = 0; i < count; i++) {
        final int index = i;
        FXGL.getGameTimer().runOnceAfter(() -> {
            double angle = (2 * Math.PI / count) * index * 1.5;
            double radius = baseRadius + radiusIncrement * index;
            double x = playerPos.getX() + radius * Math.cos(angle);
            double y = playerPos.getY() + radius * Math.sin(angle);

            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                radius = radius * 1.5;
                x = playerPos.getX() + radius * Math.cos(angle);
                y = playerPos.getY() + radius * Math.sin(angle);
            }

            SpawnData data = new SpawnData(x, y);
            data.put("player", player);
            Entity enemy = FXGL.getGameWorld().spawn(enemyType, data);
            enemy.setProperty("spawnTime", System.nanoTime());
            activeEnemyCount++; // Increment counter
        }, Duration.millis(120 * i));
    }
}
}





//OLD WAVE MANAGER
//package org.example.core;
//
//import com.almasb.fxgl.dsl.FXGL;
//import com.almasb.fxgl.entity.Entity;
//import com.almasb.fxgl.entity.SpawnData;
//import javafx.geometry.Point2D;
//import javafx.util.Duration;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
///**
// * Manages wave-based enemy spawning system similar to Vampire Survivors.
// * Handles increasing difficulty, enemy variety, and spawn formations.
// *
// * This system:
// * 1. Creates progressively harder waves with more enemies
// * 2. Introduces new enemy types as waves progress
// * 3. Creates special formations at milestone waves (every 5th wave)
// * 4. Scales enemy count and spawn rate over time
// * 5. Uses pooled enemy objects for better performance
// */
//public class WaveManager {
//    private static WaveManager instance; // Singleton instance
//    private final Random random = new Random();
//    private boolean isActive = false;
//    private int currentWave = 1;
//    private Entity player;
//    private final List<String> availableEnemyTypes = new ArrayList<>();
//
//    // Wave configuration parameters
//    private final int WAVE_DURATION_SECONDS = 60; // Each wave lasts 60 seconds
//    private final int BASE_ENEMIES_PER_WAVE = 20; // Base enemies for wave 1
//    private final double ENEMY_INCREASE_FACTOR = 1.5; // 50% more enemies each wave
//    private final double BASE_SPAWN_INTERVAL = 1.5; // Initial spawn interval
//    private final double MIN_SPAWN_INTERVAL = 0.15; // Minimum spawn interval
//
//    // Enemy pool management
//    private final Map<Integer, List<String>> waveEnemyPools = new HashMap<>(); // Maps wave number → available enemy types
//    private final Map<Integer, Double> waveSpawnRates = new HashMap<>(); // Maps wave number → spawn interval
//    private final Map<Integer, Integer> waveMaxEnemies = new HashMap<>(); // Maps wave number → max enemies
//    private final Map<Integer, String> waveFormations = new HashMap<>(); // Maps wave number → spawn formation
//
//    // Timer reference for spawning
//    private Runnable spawnTask;
//
//    // Private constructor to enforce singleton pattern
//    private WaveManager() {
//        initializeEnemyTypes();
//        initializeWaveConfigurations();
//    }
//
//    /**
//     * Get the singleton instance of WaveManager
//     * @return The shared WaveManager instance
//     */
//    public static WaveManager getInstance() {
//        if (instance == null) {
//            instance = new WaveManager();
//            System.out.println("WaveManager singleton initialized");
//        }
//        return instance;
//    }
//
//    /**
//     * Initialize the available enemy types
//     * Different enemy types are introduced at different wave numbers
//     */
//    private void initializeEnemyTypes() {
//        availableEnemyTypes.add("enemy");        // Maggot - Basic enemy (Wave 1+)
//        availableEnemyTypes.add("fastEnemy");    // Beetle - Fast enemy (Wave 2+)
//        availableEnemyTypes.add("tankEnemy");    // Mantis - Tank enemy (Wave 3+)
//        availableEnemyTypes.add("beeEnemy");     // Bee - Medium enemy (Wave 4+)
//        availableEnemyTypes.add("dragonflyEnemy"); // Dragonfly - Advanced enemy (Wave 5+)
//        availableEnemyTypes.add("giantFlyEnemy"); // Giant Fly - Mini boss (Wave 6+)
//    }
//
//    /**
//     * Initialize wave configurations
//     * This sets up:
//     * - Which enemy types appear in each wave
//     * - How quickly enemies spawn in each wave
//     * - Maximum number of enemies in each wave
//     * - Special formation patterns for milestone waves
//     */
//    private void initializeWaveConfigurations() {
//        for (int wave = 1; wave <= 20; wave++) {
//            List<String> enemyPool = new ArrayList<>();
//            if (wave >= 1) enemyPool.add("enemy");
//            if (wave >= 2) enemyPool.add("fastEnemy");
//            if (wave >= 3) enemyPool.add("tankEnemy");
//            if (wave >= 4) enemyPool.add("beeEnemy");
//            if (wave >= 5) enemyPool.add("dragonflyEnemy");
//            if (wave >= 6) enemyPool.add("giantFlyEnemy");
//            waveEnemyPools.put(wave, enemyPool);
//
//            double spawnInterval = Math.max(BASE_SPAWN_INTERVAL * Math.pow(0.85, wave - 1), MIN_SPAWN_INTERVAL);
//            waveSpawnRates.put(wave, spawnInterval);
//
//            int maxEnemies = (int)(BASE_ENEMIES_PER_WAVE * Math.pow(ENEMY_INCREASE_FACTOR, wave - 1));
//            waveMaxEnemies.put(wave, maxEnemies);
//
//            if (wave % 5 == 0) {
//                String[] formations = {"circle", "line", "spiral"};
//                waveFormations.put(wave, formations[wave % formations.length]);
//            } else {
//                waveFormations.put(wave, "random");
//            }
//        }
//    }
//
//    /**
//     * Start the wave system with a reference to the player entity
//     * @param player The player entity that enemies will target
//     */
//    public void start(Entity player) {
//        this.player = player;
//        boolean state = this.player.isActive();
//        this.currentWave = 1;
//        this.isActive = true;
//        FXGL.getWorldProperties().setValue("wave", currentWave);
//        System.out.println("WaveManager started. Player active: " + state + ", Initial wave: " + currentWave);
//
//        scheduleEnemySpawning();
//        System.out.println("Wave system fully started. Current wave: " + currentWave);
//    }
//
//    /**
//     * Stop the wave system (used when pausing or ending the game)
//     */
//    public void stop() {
//        this.isActive = false;
//        System.out.println("Wave system stopped");
//    }
//
//    /**
//     * Schedule enemy spawning based on current wave configuration
//     */
//    private void scheduleEnemySpawning() {
//        spawnTask = () -> {
//            if (!isActive) return;
//
//            // Check survivalTime for wave transition
//            int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");
//            int expectedWave = survivalTime / WAVE_DURATION_SECONDS + 1;
//            if (expectedWave > currentWave) {
//                currentWave = expectedWave;
//                FXGL.getWorldProperties().setValue("wave", currentWave);
//                System.out.println("Transitioning to Wave " + currentWave + " at survival time: " + survivalTime + "s");
//                announceNewWave();
//            }
//
//            double spawnInterval = waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL);
//            double spawnChance = 0.5 / spawnInterval;
//            int spawnCount = 1;
//
//            if (currentWave > 5 && random.nextDouble() < 0.3) {
//                spawnCount = Math.min(currentWave / 3, 4);
//            }
//
//            if (random.nextDouble() < spawnChance) {
//                for (int i = 0; i < spawnCount; i++) {
//                    spawnEnemyForCurrentWave(survivalTime);
//                }
//            }
//        };
//        FXGL.getGameTimer().runAtInterval(spawnTask, Duration.seconds(0.3));
//    }
//
//    /**
//     * Spawn an enemy based on the current wave's configuration
//     * @param survivalTime Current survival time for scaling
//     */
//    private void spawnEnemyForCurrentWave(int survivalTime) {
//        if (!isActive || player == null) {
//            System.out.println("Skipping enemy spawn: WaveManager inactive or player null");
//            return;
//        }
//
//        List<String> enemyPool = waveEnemyPools.getOrDefault(currentWave, List.of("enemy"));
//        String formation = waveFormations.getOrDefault(currentWave, "random");
//
//        int baseMax = waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE);
//        double timeScaling = 1.0 + (survivalTime / 300.0);
//        int maxEnemies = (int)(baseMax * timeScaling);
//
//        int currentEnemyCount = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).size();
//        int waveAllowance = Math.min(currentWave * 5, 50);
//
//        if (currentEnemyCount >= maxEnemies + waveAllowance) {
//            System.out.println("Enemy spawn skipped: Current enemies (" + currentEnemyCount + ") exceed max (" + (maxEnemies + waveAllowance) + ")");
//            return;
//        }
//
//        String enemyType;
//        if (currentWave >= 6 && random.nextDouble() < 0.1 * (currentWave / 6.0)) {
//            int bossIndex = Math.min(enemyPool.size() - 1, enemyPool.size() - 2);
//            enemyType = enemyPool.get(Math.max(bossIndex, 0));
//        } else {
//            enemyType = enemyPool.get(random.nextInt(enemyPool.size()));
//        }
//
//        System.out.println("Spawning enemy for Wave " + currentWave + ": Type=" + enemyType + ", Formation=" + formation + ", Total enemies=" + (currentEnemyCount + 1));
//
//        switch (formation) {
//            case "circle":
//                spawnEnemyInCircleFormation(enemyType);
//                break;
//            case "line":
//                spawnEnemyInLineFormation(enemyType);
//                break;
//            case "spiral":
//                spawnEnemyInSpiralFormation(enemyType);
//                break;
//            case "random":
//            default:
//                spawnEnemyOutsideViewport(enemyType);
//                break;
//        }
//    }
//
//    /**
//     * Announce a new wave with console prints and UI notifications
//     */
//    private void announceNewWave() {
//        System.out.println("=========================================");
//        System.out.println("Wave " + currentWave + " approaching!");
//        System.out.println("New wave started: " + currentWave);
//        System.out.println("Max enemies: " + waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE));
//        System.out.println("Spawn interval: " + waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL));
//        System.out.println("Enemy types available: " + waveEnemyPools.getOrDefault(currentWave, List.of("enemy")));
//
//        FXGL.getNotificationService().pushNotification("Wave " + currentWave + " started!");
//
//        if (currentWave % 5 == 0) {
//            String formation = waveFormations.getOrDefault(currentWave, "random");
//            System.out.println("WARNING: Special formation incoming! Formation: " + formation);
//            FXGL.getNotificationService().pushNotification("WARNING: Special " + formation + " formation incoming!");
//
//            FXGL.getGameTimer().runOnceAfter(() -> {
//                String enemyType = waveEnemyPools.get(currentWave).get(
//                        random.nextInt(waveEnemyPools.get(currentWave).size()));
//                System.out.println("Spawning special formation for Wave " + currentWave + " with enemy: " + enemyType);
//
//                String formationType = waveFormations.get(currentWave);
//                if (formationType == null) formationType = "circle";
//
//                switch (formationType) {
//                    case "line":
//                        spawnEnemyInLineFormation(enemyType);
//                        break;
//                    case "spiral":
//                        spawnEnemyInSpiralFormation(enemyType);
//                        break;
//                    case "circle":
//                    default:
//                        spawnEnemyInCircleFormation(enemyType);
//                        break;
//                }
//            }, Duration.seconds(1.5));
//        }
//
//        if (currentWave % 10 == 0) {
//            System.out.println("DANGER: Massive enemy wave approaching!");
//            FXGL.getNotificationService().pushNotification("DANGER: Massive enemy wave approaching!");
//
//            FXGL.getGameTimer().runOnceAfter(() -> {
//                String enemyType = waveEnemyPools.get(currentWave).get(
//                        random.nextInt(waveEnemyPools.get(currentWave).size()));
//                System.out.println("Spawning first formation (circle) for Wave " + currentWave + " with enemy: " + enemyType);
//                spawnEnemyInCircleFormation(enemyType);
//
//                FXGL.getGameTimer().runOnceAfter(() -> {
//                    String secondEnemyType = waveEnemyPools.get(currentWave).get(
//                            random.nextInt(waveEnemyPools.get(currentWave).size()));
//                    System.out.println("Spawning second formation (spiral) for Wave " + currentWave + " with enemy: " + secondEnemyType);
//                    spawnEnemyInSpiralFormation(secondEnemyType);
//
//                    FXGL.getGameTimer().runOnceAfter(() -> {
//                        String thirdEnemyType = waveEnemyPools.get(currentWave).get(
//                                random.nextInt(waveEnemyPools.get(currentWave).size()));
//                        System.out.println("Spawning third formation (line) for Wave " + currentWave + " with enemy: " + thirdEnemyType);
//                        spawnEnemyInLineFormation(thirdEnemyType);
//                    }, Duration.seconds(2));
//                }, Duration.seconds(3));
//            }, Duration.seconds(2));
//        }
//        System.out.println("=========================================");
//    }
//
//    /**
//     * Get the current wave number
//     * @return The current wave number
//     */
//    public int getCurrentWave() {
//        return currentWave;
//    }
//
//    /**
//     * Get the elapsed time in seconds
//     * @return Elapsed time in seconds since wave system started
//     */
//    public int getElapsedTimeSeconds() {
//        return FXGL.getWorldProperties().getInt("survivalTime");
//    }
//
//    /**
//     * Check if the wave system is active
//     * @return True if the wave system is currently running
//     */
//    public boolean isActive() {
//        return isActive;
//    }
//
//    /**
//     * Reset the wave system
//     * Used when starting a new game
//     */
//    public void reset() {
//        currentWave = 1;
//        isActive = false;
//        spawnTask = null;
//        FXGL.getWorldProperties().setValue("wave", 1);
//        System.out.println("WaveManager reset: Wave set to 1, spawnTask cleared");
//    }
//
//    /**
//     * Spawn an enemy outside the viewport (standard spawning method)
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyOutsideViewport(String enemyType) {
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        double x, y;
//        int margin = 200;
//
//        int side = random.nextInt(4);
//        switch (side) {
//            case 0: // Top
//                x = viewMinX + random.nextDouble() * FXGL.getAppWidth();
//                y = viewMinY - margin;
//                break;
//            case 1: // Right
//                x = viewMaxX + margin;
//                y = viewMinY + random.nextDouble() * FXGL.getAppHeight();
//                break;
//            case 2: // Bottom
//                x = viewMinX + random.nextDouble() * FXGL.getAppWidth();
//                y = viewMaxY + margin;
//                break;
//            case 3: // Left
//            default:
//                x = viewMinX - margin;
//                y = viewMinY + random.nextDouble() * FXGL.getAppHeight();
//                break;
//        }
//
//        SpawnData data = new SpawnData(x, y);
//        data.put("player", player);
//        FXGL.getGameWorld().spawn(enemyType, data);
//    }
//
//    /**
//     * Spawn enemies in a circle formation around the player
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInCircleFormation(String enemyType) {
//        double radius = 800;
//        int count = 12;
//
//        if (currentWave > 5) {
//            count = 12 + Math.min((currentWave - 5) * 2, 12);
//        }
//
//        Point2D playerPos = player.getPosition();
//
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        for (int i = 0; i < count; i++) {
//            double angle = (2 * Math.PI / count) * i;
//            double x = playerPos.getX() + radius * Math.cos(angle);
//            double y = playerPos.getY() + radius * Math.sin(angle);
//
//            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
//                double adjustedRadius = radius * 1.5;
//                x = playerPos.getX() + adjustedRadius * Math.cos(angle);
//                y = playerPos.getY() + adjustedRadius * Math.sin(angle);
//            }
//
//            SpawnData data = new SpawnData(x, y);
//            data.put("player", player);
//            FXGL.getGameWorld().spawn(enemyType, data);
//        }
//    }
//
//    /**
//     * Spawn enemies in a line formation
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInLineFormation(String enemyType) {
//        double distance = 800;
//        int count = 8;
//        double spacing = 80;
//
//        if (currentWave > 5) {
//            count = 8 + Math.min((currentWave - 5), 7);
//        }
//
//        int side = random.nextInt(4);
//        double startX, startY, dirX = 0, dirY = 0;
//
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        switch (side) {
//            case 0: // Top
//                startX = viewMinX + FXGL.getAppWidth() / 2 - (count * spacing) / 2;
//                startY = viewMinY - distance;
//                dirX = 1;
//                dirY = 0;
//                break;
//            case 1: // Right
//                startX = viewMaxX + distance;
//                startY = viewMinY + FXGL.getAppHeight() / 2 - (count * spacing) / 2;
//                dirX = 0;
//                dirY = 1;
//                break;
//            case 2: // Bottom
//                startX = viewMinX + FXGL.getAppWidth() / 2 - (count * spacing) / 2;
//                startY = viewMaxY + distance;
//                dirX = 1;
//                dirY = 0;
//                break;
//            case 3: // Left
//            default:
//                startX = viewMinX - distance;
//                startY = viewMinY + FXGL.getAppHeight() / 2 - (count * spacing) / 2;
//                dirX = 0;
//                dirY = 1;
//                break;
//        }
//
//        for (int i = 0; i < count; i++) {
//            double x = startX + dirX * spacing * i;
//            double y = startY + dirY * spacing * i;
//
//            SpawnData data = new SpawnData(x, y);
//            data.put("player", player);
//            FXGL.getGameWorld().spawn(enemyType, data);
//        }
//    }
//
//    /**
//     * Spawn enemies in a spiral formation
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInSpiralFormation(String enemyType) {
//        int count = 16;
//        double baseRadius = 700;
//        double radiusIncrement = 40;
//
//        if (currentWave > 5) {
//            count = 16 + Math.min((currentWave - 5) * 2, 16);
//        }
//
//        Point2D playerPos = player.getPosition();
//
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        for (int i = 0; i < count; i++) {
//            double angle = (2 * Math.PI / count) * i * 1.5;
//            double radius = baseRadius + radiusIncrement * i;
//            double x = playerPos.getX() + radius * Math.cos(angle);
//            double y = playerPos.getY() + radius * Math.sin(angle);
//
//            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
//                radius = radius * 1.5;
//                x = playerPos.getX() + radius * Math.cos(angle);
//                y = playerPos.getY() + radius * Math.sin(angle);
//            }
//
//            SpawnData data = new SpawnData(x, y);
//            data.put("player", player);
//            FXGL.getGameWorld().spawn(enemyType, data);
//        }
//    }
//}
//
//
