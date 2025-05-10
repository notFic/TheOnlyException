package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.geometry.Point2D;
import javafx.util.Duration;

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
    private int elapsedTimeSeconds = 0;
    private Entity player;
    private final List<String> availableEnemyTypes = new ArrayList<>();

    // Wave configuration parameters
    private final int WAVE_DURATION_SECONDS = 60; // Each wave lasts 60 seconds
    private final int BASE_ENEMIES_PER_WAVE = 20; // Base enemies for wave 1
    private final double ENEMY_INCREASE_FACTOR = 1.5; // 50% more enemies each wave
    private final double BASE_SPAWN_INTERVAL = 1.5; // Initial spawn interval
    private final double MIN_SPAWN_INTERVAL = 0.15; // Minimum spawn interval

    // Enemy pool management
    private final Map<Integer, List<String>> waveEnemyPools = new HashMap<>(); // Maps wave number → available enemy types
    private final Map<Integer, Double> waveSpawnRates = new HashMap<>(); // Maps wave number → spawn interval
    private final Map<Integer, Integer> waveMaxEnemies = new HashMap<>(); // Maps wave number → max enemies
    private final Map<Integer, String> waveFormations = new HashMap<>(); // Maps wave number → spawn formation

    // Timer references to manage and restart timers
    // ADDED: Store timer references to prevent loss during reset
    private Runnable timeTrackingTask;
    private Runnable spawnTask;

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
        // Set up enemy pools for each wave
        for (int wave = 1; wave <= 20; wave++) {
            List<String> enemyPool = new ArrayList<>();

            // Wave 1: Only basic enemies
            if (wave >= 1) enemyPool.add("enemy");

            // Wave 2+: Add fast enemies
            if (wave >= 2) enemyPool.add("fastEnemy");

            // Wave 3+: Add tank enemies
            if (wave >= 3) enemyPool.add("tankEnemy");

            // Wave 4+: Add bee enemies
            if (wave >= 4) enemyPool.add("beeEnemy");

            // Wave 5+: Add dragonfly enemies
            if (wave >= 5) enemyPool.add("dragonflyEnemy");

            // Wave 6+: Add mini-boss enemies (giant flies)
            if (wave >= 6) enemyPool.add("giantFlyEnemy");

            waveEnemyPools.put(wave, enemyPool);

            // Define spawn rates (decreasing interval = increasing rate)
            double spawnInterval = Math.max(BASE_SPAWN_INTERVAL * Math.pow(0.85, wave - 1), MIN_SPAWN_INTERVAL);
            waveSpawnRates.put(wave, spawnInterval);

            // Define max enemies per wave
            int maxEnemies = (int)(BASE_ENEMIES_PER_WAVE * Math.pow(ENEMY_INCREASE_FACTOR, wave - 1));
            waveMaxEnemies.put(wave, maxEnemies);

            // Set formations
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
        var state = this.player.isActive();
        this.currentWave = 1;
        this.elapsedTimeSeconds = 0;
        this.isActive = true;
        FXGL.getWorldProperties().setValue("wave", currentWave);
        System.out.println("WaveManager started. Player active: " + state + ", Initial wave: " + currentWave);

        // ADDED: Store time tracking task to allow restarting
        timeTrackingTask = () -> {
            if (state && isActive) { // CHANGED: Added isActive check to prevent running when stopped
                elapsedTimeSeconds++;
                System.out.println("WaveManager time tick: " + elapsedTimeSeconds + "s, Current wave: " + currentWave + ", Player active: " + state + ", WaveManager active: " + isActive);
                if (elapsedTimeSeconds % WAVE_DURATION_SECONDS == 0) {
                    currentWave++;
                    System.out.println("Transitioning to Wave " + currentWave + " at time: " + elapsedTimeSeconds + "s");
                    announceNewWave();
                }
            } else {
                System.out.println("WaveManager time tick skipped: Player active=" + state + ", WaveManager active=" + isActive);
            }
        };
        FXGL.getGameTimer().runAtInterval(timeTrackingTask, Duration.seconds(1));

        scheduleEnemySpawning();
        System.out.println("Wave system fully started. Current wave: " + currentWave);
    }

    /**
     * Stop the wave system (used when pausing or ending the game)
     */
    public void stop() {
        this.isActive = false;
        System.out.println("Wave system stopped");
    }

    /**
     * Schedule enemy spawning based on current wave configuration
     */
    private void scheduleEnemySpawning() {
        // ADDED: Store spawn task to allow restarting
        spawnTask = () -> {
            if (!isActive) return;

            double spawnInterval = waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL);
            double spawnChance = 0.5 / spawnInterval;
            int spawnCount = 1;

            if (currentWave > 5 && random.nextDouble() < 0.3) {
                spawnCount = Math.min(currentWave / 3, 4);
            }

            if (random.nextDouble() < spawnChance) {
                for (int i = 0; i < spawnCount; i++) {
                    spawnEnemyForCurrentWave();
                }
            }
        };
        FXGL.getGameTimer().runAtInterval(spawnTask, Duration.seconds(0.3));
    }

    /**
     * Spawn an enemy based on the current wave's configuration
     */
    private void spawnEnemyForCurrentWave() {
        if (!isActive || player == null) {
            System.out.println("Skipping enemy spawn: WaveManager inactive or player null");
            return;
        }

        List<String> enemyPool = waveEnemyPools.getOrDefault(currentWave, List.of("enemy"));
        String formation = waveFormations.getOrDefault(currentWave, "random");

        int baseMax = waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE);
        double timeScaling = 1.0 + (elapsedTimeSeconds / 300.0);
        int maxEnemies = (int)(baseMax * timeScaling);

        int currentEnemyCount = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).size();
        int waveAllowance = Math.min(currentWave * 5, 50);

        if (currentEnemyCount >= maxEnemies + waveAllowance) {
            System.out.println("Enemy spawn skipped: Current enemies (" + currentEnemyCount + ") exceed max (" + (maxEnemies + waveAllowance) + ")");
            return;
        }

        String enemyType;
        if (currentWave >= 6 && random.nextDouble() < 0.1 * (currentWave / 6.0)) {
            int bossIndex = Math.min(enemyPool.size() - 1, enemyPool.size() - 2);
            enemyType = enemyPool.get(Math.max(bossIndex, 0));
        } else {
            enemyType = enemyPool.get(random.nextInt(enemyPool.size()));
        }

        System.out.println("Spawning enemy for Wave " + currentWave + ": Type=" + enemyType + ", Formation=" + formation + ", Total enemies=" + (currentEnemyCount + 1));

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
     * Announce a new wave with console prints and UI notifications
     */
    private void announceNewWave() {
        System.out.println("=========================================");
        System.out.println("Wave " + currentWave + " approaching!");
        System.out.println("New wave started: " + currentWave);
        System.out.println("Max enemies: " + waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE));
        System.out.println("Spawn interval: " + waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL));
        System.out.println("Enemy types available: " + waveEnemyPools.getOrDefault(currentWave, List.of("enemy")));

        // ADDED: Push notification for wave start
        FXGL.getNotificationService().pushNotification("Wave " + currentWave + " started!");

        // Special announcement for milestone waves
        if (currentWave % 5 == 0) {
            String formation = waveFormations.getOrDefault(currentWave, "random");
            System.out.println("WARNING: Special formation incoming! Formation: " + formation);
            // ADDED: Notification for milestone wave
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
            }, Duration.seconds(1.5));
        }

        if (currentWave % 10 == 0) {
            System.out.println("DANGER: Massive enemy wave approaching!");
            // ADDED: Notification for massive wave
            FXGL.getNotificationService().pushNotification("DANGER: Massive enemy wave approaching!");

            FXGL.getGameTimer().runOnceAfter(() -> {
                String enemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));
                System.out.println("Spawning first formation (circle) for Wave " + currentWave + " with enemy: " + enemyType);
                spawnEnemyInCircleFormation(enemyType);

                FXGL.getGameTimer().runOnceAfter(() -> {
                    String secondEnemyType = waveEnemyPools.get(currentWave).get(
                            random.nextInt(waveEnemyPools.get(currentWave).size()));
                    System.out.println("Spawning second formation (spiral) for Wave " + currentWave + " with enemy: " + secondEnemyType);
                    spawnEnemyInSpiralFormation(secondEnemyType);

                    FXGL.getGameTimer().runOnceAfter(() -> {
                        String thirdEnemyType = waveEnemyPools.get(currentWave).get(
                                random.nextInt(waveEnemyPools.get(currentWave).size()));
                        System.out.println("Spawning third formation (line) for Wave " + currentWave + " with enemy: " + thirdEnemyType);
                        spawnEnemyInLineFormation(thirdEnemyType);
                    }, Duration.seconds(2));
                }, Duration.seconds(3));
            }, Duration.seconds(2));
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
        return elapsedTimeSeconds;
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
        elapsedTimeSeconds = 0;
        isActive = false;
        timeTrackingTask = null;
        spawnTask = null;
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
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    /**
     * Spawn enemies in a circle formation around the player
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyInCircleFormation(String enemyType) {
        double radius = 800;
        int count = 12;

        if (currentWave > 5) {
            count = 12 + Math.min((currentWave - 5) * 2, 12);
        }

        Point2D playerPos = player.getPosition();

        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = playerPos.getX() + radius * Math.cos(angle);
            double y = playerPos.getY() + radius * Math.sin(angle);

            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                double adjustedRadius = radius * 1.5;
                x = playerPos.getX() + adjustedRadius * Math.cos(angle);
                y = playerPos.getY() + adjustedRadius * Math.sin(angle);
            }

            SpawnData data = new SpawnData(x, y);
            data.put("player", player);
            FXGL.getGameWorld().spawn(enemyType, data);
        }
    }

    /**
     * Spawn enemies in a line formation
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyInLineFormation(String enemyType) {
        double distance = 800;
        int count = 8;
        double spacing = 80;

        if (currentWave > 5) {
            count = 8 + Math.min((currentWave - 5), 7);
        }

        int side = random.nextInt(4);
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

        for (int i = 0; i < count; i++) {
            double x = startX + dirX * spacing * i;
            double y = startY + dirY * spacing * i;

            SpawnData data = new SpawnData(x, y);
            data.put("player", player);
            FXGL.getGameWorld().spawn(enemyType, data);
        }
    }

    /**
     * Spawn enemies in a spiral formation
     * @param enemyType The type of enemy to spawn
     */
    private void spawnEnemyInSpiralFormation(String enemyType) {
        int count = 16;
        double baseRadius = 700;
        double radiusIncrement = 40;

        if (currentWave > 5) {
            count = 16 + Math.min((currentWave - 5) * 2, 16);
        }

        Point2D playerPos = player.getPosition();

        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i * 1.5;
            double radius = baseRadius + radiusIncrement * i;
            double x = playerPos.getX() + radius * Math.cos(angle);
            double y = playerPos.getY() + radius * Math.sin(angle);

            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                radius = radius * 1.5;
                x = playerPos.getX() + radius * Math.cos(angle);
                y = playerPos.getY() + radius * Math.sin(angle);
            }

            SpawnData data = new SpawnData(x, y);
            data.put("player", player);
            FXGL.getGameWorld().spawn(enemyType, data);
        }
    }
}







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
//    private int elapsedTimeSeconds = 0;
//    private Entity player;
//    private final List<String> availableEnemyTypes = new ArrayList<>();
//
//    // Wave configuration parameters
//    private final int WAVE_DURATION_SECONDS = 60; // Each wave lasts 60 seconds (increased from 30)
//    private final int BASE_ENEMIES_PER_WAVE = 20; // Increased from 10 to 20 for more enemies in wave 1
//    private final double ENEMY_INCREASE_FACTOR = 1.5; // Increased from 1.2 to 1.5 (50% more enemies each wave)
//    private final double BASE_SPAWN_INTERVAL = 1.5; // Decreased from 2.0 to 1.5 for faster initial spawning
//    private final double MIN_SPAWN_INTERVAL = 0.15; // Decreased from 0.2 to 0.15 for faster maximum spawn rate
//
//    // Enemy pool management
//    private final Map<Integer, List<String>> waveEnemyPools = new HashMap<>(); // Maps wave number → available enemy types
//    private final Map<Integer, Double> waveSpawnRates = new HashMap<>(); // Maps wave number → spawn interval
//    private final Map<Integer, Integer> waveMaxEnemies = new HashMap<>(); // Maps wave number → max enemies
//    private final Map<Integer, String> waveFormations = new HashMap<>(); // Maps wave number → spawn formation
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
//        // Set up enemy pools for each wave
//        for (int wave = 1; wave <= 20; wave++) {
//            List<String> enemyPool = new ArrayList<>();
//
//            // Wave 1: Only basic enemies
//            if (wave >= 1) enemyPool.add("enemy");
//
//            // Wave 2+: Add fast enemies
//            if (wave >= 2) enemyPool.add("fastEnemy");
//
//            // Wave 3+: Add tank enemies
//            if (wave >= 3) enemyPool.add("tankEnemy");
//
//            // Wave 4+: Add bee enemies
//            if (wave >= 4) enemyPool.add("beeEnemy");
//
//            // Wave 5+: Add dragonfly enemies
//            if (wave >= 5) enemyPool.add("dragonflyEnemy");
//
//            // Wave 6+: Add mini-boss enemies (giant flies)
//            if (wave >= 6) enemyPool.add("giantFlyEnemy");
//
//            waveEnemyPools.put(wave, enemyPool);
//
//            // Define spawn rates (decreasing interval = increasing rate)
//            // Formula creates an exponential decrease in spawn interval
//            double spawnInterval = Math.max(BASE_SPAWN_INTERVAL * Math.pow(0.85, wave - 1), MIN_SPAWN_INTERVAL);
//            waveSpawnRates.put(wave, spawnInterval);
//
//            // Define max enemies per wave
//            // Formula creates an exponential increase in max enemies
//            int maxEnemies = (int)(BASE_ENEMIES_PER_WAVE * Math.pow(ENEMY_INCREASE_FACTOR, wave - 1));
//            waveMaxEnemies.put(wave, maxEnemies);
//
//            // Set formations
//            if (wave % 5 == 0) {
//                // Every 5th wave is a special formation wave
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
//        var state = this.player.isActive();
//        this.currentWave = 1;
//        this.elapsedTimeSeconds = 0;
//        this.isActive = true; // Ensure isActive is set
//        FXGL.getWorldProperties().setValue("wave", currentWave);
//        System.out.println("WaveManager started. Player active: " + state + ", Initial wave: " + currentWave);
//
//        // Set up the time tracking timer
//        FXGL.getGameTimer().runAtInterval(() -> {
//            if (state) {
//                elapsedTimeSeconds++;
//                System.out.println("WaveManager time tick: " + elapsedTimeSeconds + "s, Current wave: " + currentWave);
//
//                // Check for wave transition
//                if (elapsedTimeSeconds % WAVE_DURATION_SECONDS == 0) {
//                    currentWave++;
//                    System.out.println("Transitioning to Wave " + currentWave + " at time: " + elapsedTimeSeconds + "s");
//                    announceNewWave();
//                }
//            }
//        }, Duration.seconds(1));
//
//        // Set up enemy spawning timer
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
//     * Uses a half-second interval to check if enemies should spawn
//     */
//    private void scheduleEnemySpawning() {
//        // This will be updated whenever the wave changes
//        FXGL.getGameTimer().runAtInterval(() -> {
//            if (!isActive) return;
//
//            // Get spawn interval for the current wave
//            double spawnInterval = waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL);
//
//            // Calculate chance to spawn based on interval
//            // Lower interval means higher chance to spawn per check
//            double spawnChance = 0.5 / spawnInterval;
//
//            // Potentially spawn multiple enemies based on wave number
//            int spawnCount = 1;
//
//            // For higher waves, occasionally spawn multiple enemies at once
//            if (currentWave > 5 && random.nextDouble() < 0.3) {
//                spawnCount = Math.min(currentWave / 3, 4); // Up to 4 enemies at once in later waves
//            }
//
//            // Try to spawn enemies based on chance
//            if (random.nextDouble() < spawnChance) {
//                for (int i = 0; i < spawnCount; i++) {
//                    spawnEnemyForCurrentWave();
//                }
//            }
//
//        }, Duration.seconds(0.3)); // Decreased from 0.5 to 0.3 for more frequent spawn checks
//    }
//
//    /**
//     * Spawn an enemy based on the current wave's configuration
//     * This is the core method that decides:
//     * - Which enemy type to spawn
//     * - How many to spawn
//     * - In what formation
//     */
//    private void spawnEnemyForCurrentWave() {
//        if (!isActive || player == null) {
//            System.out.println("Skipping enemy spawn: WaveManager inactive or player null");
//            return;
//        }
//
//        List<String> enemyPool = waveEnemyPools.getOrDefault(currentWave, List.of("enemy"));
//        String formation = waveFormations.getOrDefault(currentWave, "random");
//
//        int baseMax = waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE);
//        double timeScaling = 1.0 + (elapsedTimeSeconds / 300.0);
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
//     * Announce a new wave with console prints only
//     * Provides information about the wave and any special formations
//     */
//    private void announceNewWave() {
//        System.out.println("=========================================");
//        System.out.println("Wave " + currentWave + " approaching!");
//        System.out.println("New wave started: " + currentWave);
//        System.out.println("Max enemies: " + waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE));
//        System.out.println("Spawn interval: " + waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL));
//        System.out.println("Enemy types available: " + waveEnemyPools.getOrDefault(currentWave, List.of("enemy")));
//
//        // Special announcement for milestone waves
//        if (currentWave % 5 == 0) {
//            System.out.println("WARNING: Special formation incoming! Formation: " + waveFormations.getOrDefault(currentWave, "random"));
//
//            // Add a slight delay before spawning the formation to avoid immediate spawning
//            FXGL.getGameTimer().runOnceAfter(() -> {
//                String enemyType = waveEnemyPools.get(currentWave).get(
//                        random.nextInt(waveEnemyPools.get(currentWave).size()));
//                System.out.println("Spawning special formation for Wave " + currentWave + " with enemy: " + enemyType);
//
//                String formation = waveFormations.get(currentWave);
//                if (formation == null) formation = "circle";
//
//                switch (formation) {
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
//        return elapsedTimeSeconds;
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
//        elapsedTimeSeconds = 0;
//        isActive = false;
//    }
//
//    /**
//     * Spawn an enemy outside the viewport (standard spawning method)
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyOutsideViewport(String enemyType) {
//        // Get viewport bounds
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        // Determine spawn position
//        double x, y;
//        int margin = 200; // Increased distance outside viewport from 100 to 200
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
//     * Used for special waves and milestone events
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInCircleFormation(String enemyType) {
//        double radius = 800; // Increased distance from player from 500 to 800
//        int count = 12; // Increased from 8 to 12 for more enemies in the formation
//
//        // For higher waves, spawn even more enemies
//        if (currentWave > 5) {
//            count = 12 + Math.min((currentWave - 5) * 2, 12); // Up to 24 enemies in later waves
//        }
//
//        Point2D playerPos = player.getPosition();
//
//        // Get viewport bounds to check if spawn point is visible
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
//            // Ensure spawn position is outside viewport
//            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
//                // If inside viewport, push it further outside
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
//     * Creates a line of enemies coming from one side of the screen
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInLineFormation(String enemyType) {
//        double distance = 800; // Increased distance from edge from 500 to 800
//        int count = 8; // Increased from 5 to 8 for more enemies in the line
//        double spacing = 80; // Decreased from 100 to 80 for tighter spacing
//
//        // For higher waves, spawn even more enemies
//        if (currentWave > 5) {
//            count = 8 + Math.min((currentWave - 5), 7); // Up to 15 enemies in later waves
//        }
//
//        // Choose a random side
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
//     * Creates a spiral pattern of enemies around the player
//     * @param enemyType The type of enemy to spawn
//     */
//    private void spawnEnemyInSpiralFormation(String enemyType) {
//        int count = 16; // Increased from 12 to 16 for more enemies in the spiral
//        double baseRadius = 700; // Increased starting radius from 300 to 700
//        double radiusIncrement = 40; // Reduced from 50 to 40 for tighter spiral
//
//        // For higher waves, spawn even more enemies
//        if (currentWave > 5) {
//            count = 16 + Math.min((currentWave - 5) * 2, 16); // Up to 32 enemies in later waves
//        }
//
//        Point2D playerPos = player.getPosition();
//
//        // Get viewport bounds to check if spawn point is visible
//        double viewMinX = FXGL.getGameScene().getViewport().getX();
//        double viewMinY = FXGL.getGameScene().getViewport().getY();
//        double viewMaxX = viewMinX + FXGL.getAppWidth();
//        double viewMaxY = viewMinY + FXGL.getAppHeight();
//
//        for (int i = 0; i < count; i++) {
//            double angle = (2 * Math.PI / count) * i * 1.5; // Multiply by 1.5 for a more spread-out spiral
//            double radius = baseRadius + radiusIncrement * i;
//            double x = playerPos.getX() + radius * Math.cos(angle);
//            double y = playerPos.getY() + radius * Math.sin(angle);
//
//            // Ensure spawn position is outside viewport
//            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
//                // If inside viewport, push it further outside
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