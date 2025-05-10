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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private volatile boolean isActive = false;
    private int currentWave = 1;
    private Entity player;
    private final List<String> availableEnemyTypes = new ArrayList<>();
    private final AtomicInteger activeEnemyCount = new AtomicInteger(0); // Thread-safe counter

    // Wave configuration parameters
    private final int WAVE_DURATION_SECONDS = 30; // Reduced from 45 to 30 seconds for faster progression
    private final int BASE_ENEMIES_PER_WAVE = 35; // Reduced from 45 to 35 to decrease lag
    private final double ENEMY_INCREASE_FACTOR = 1.4; // Reduced from 1.5 to 1.4 to control enemy scaling
    private final double BASE_SPAWN_INTERVAL = 0.9; // Increased from 0.8 to 0.9 to reduce spawn frequency
    private final double MIN_SPAWN_INTERVAL = 0.12; // Increased from 0.08 to 0.12 to prevent too many enemies

    // Optimization parameters
    private final int MAX_ENEMIES_HARD_CAP = 225; // Reduced from 350 to 225 for better performance
    private final int CLEANUP_FREQUENCY_MS = 1500; // Reduced from 2000 to 1500ms for more frequent cleanup
    private boolean cleanupScheduled = false;
    private long lastCleanupTime = 0;
    private final int VIEW_MARGIN = 1200; // Reduced from 1500 to 1200 to clean up offscreen enemies sooner
    private final long ENEMY_LIFETIME_NS = 20_000_000_000L; // Reduced from 25s to 20s for faster cleanup

    // Enemy pool management
    private final Map<Integer, List<String>> waveEnemyPools = new HashMap<>(); // Maps wave number → available enemy types
    private final Map<Integer, Double> waveSpawnRates = new HashMap<>(); // Maps wave number → spawn interval
    private final Map<Integer, Integer> waveMaxEnemies = new HashMap<>(); // Maps wave number → max enemies
    private final Map<Integer, String> waveFormations = new HashMap<>(); // Maps wave number → spawn formation

    // Object pool for enemy entities (future implementation)
    private final Map<String, List<Entity>> enemyPool = new ConcurrentHashMap<>();

    // Batch processing for formations
    private final List<SpawnCommand> pendingSpawns = new ArrayList<>();
    private boolean formationInProgress = false;

    // Task references
    private Runnable spawnTask;
    private Runnable cleanupTask;

    // Simple command pattern for batched spawning
    private static class SpawnCommand {
        String type;
        double x;
        double y;
        int delay;

        SpawnCommand(String type, double x, double y, int delay) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.delay = delay;
        }
    }

    // Listener to track enemy removal
    private final EntityWorldListener enemyRemovalListener = new EntityWorldListener() {
        @Override
        public void onEntityAdded(Entity entity) {
            // No action needed for added entities
        }

        @Override
        public void onEntityRemoved(Entity entity) {
            if (entity.getType() == EntityType.ENEMY) {
                activeEnemyCount.decrementAndGet();
                // No logging in production code
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
            synchronized (WaveManager.class) {
                if (instance == null) {
                    instance = new WaveManager();
                }
            }
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
        availableEnemyTypes.add("beeEnemy");     // Bee - Swarming enemy (Wave 3+)
        availableEnemyTypes.add("dragonflyEnemy"); // Dragonfly - Advanced enemy (Wave 4+)
        availableEnemyTypes.add("giantFlyEnemy"); // Giant Fly - Mini boss (Wave 5+)
        availableEnemyTypes.add("tankEnemy");    // Mantis - Tank enemy (Wave 6+)
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
            if (wave >= 3) enemyPool.add("beeEnemy");
            if (wave >= 3) enemyPool.add("dragonflyEnemy"); // Earlier introduction (wave 3 instead of 4)
            if (wave >= 4) enemyPool.add("giantFlyEnemy"); // Earlier introduction (wave 4 instead of 5)
            if (wave >= 5) enemyPool.add("tankEnemy"); // Earlier introduction (wave 5 instead of 6)
            waveEnemyPools.put(wave, enemyPool);

            // Faster spawn rates for more challenge - Vampire Survivors style
            double spawnInterval = Math.max(BASE_SPAWN_INTERVAL * Math.pow(0.85, wave - 1), MIN_SPAWN_INTERVAL);
            
            // Special case for wave 3 (bees) - make them spawn even more frequently
            if (wave == 3) {
                spawnInterval = Math.max(spawnInterval * 0.5, MIN_SPAWN_INTERVAL); // 50% faster spawn rate for bees
            }
            
            waveSpawnRates.put(wave, spawnInterval);

            // More enemies per wave - Vampire Survivors style
            int maxEnemies = (int)(BASE_ENEMIES_PER_WAVE * Math.pow(ENEMY_INCREASE_FACTOR, wave - 1));
            
            // Special case for wave 3 (bees) - increase their count
            if (wave == 3) {
                maxEnemies = (int)(maxEnemies * 2.0); // 2.0x bee count instead of 2.5x to reduce lag
            }
            
            maxEnemies = Math.min(maxEnemies, MAX_ENEMIES_HARD_CAP);
            waveMaxEnemies.put(wave, maxEnemies);

            // More frequent special formations
            if (wave % 4 == 0) { // Changed from every 3rd to every 4th wave to reduce lag
                String[] formations = {"circle", "line", "spiral", "pincer", "cross"};
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
        this.currentWave = 1;
        this.isActive = true;
        this.activeEnemyCount.set(0); // Reset counter
        FXGL.getWorldProperties().setValue("wave", currentWave);

        // Add listener for enemy removal
        FXGL.getGameWorld().addWorldListener(enemyRemovalListener);

        scheduleEnemySpawning();
        scheduleCleanupTask();
    }

    /**
     * Schedule the periodic cleanup task
     */
    private void scheduleCleanupTask() {
        cleanupTask = () -> {
            if (!isActive) return;
            cleanupOldEnemies();
        };

        // Run cleanup every 2 seconds instead of with every spawn
        FXGL.getGameTimer().runAtInterval(cleanupTask, Duration.millis(CLEANUP_FREQUENCY_MS));
    }

    /**
     * Stop the wave system (used when pausing or ending the game)
     */
    public void stop() {
        this.isActive = false;
        this.activeEnemyCount.set(0); // Reset counter
        FXGL.getGameWorld().removeWorldListener(enemyRemovalListener);

        // Cancel tasks
        if (spawnTask != null) {
            FXGL.getGameTimer().clear();
            spawnTask = null;
        }

        if (cleanupTask != null) {
            FXGL.getGameTimer().clear();
            cleanupTask = null;
        }
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
                announceNewWave();
            }

            // Skip spawning if a formation is in progress to reduce lag
            if (formationInProgress) {
                return;
            }

            // Check current enemy count before spawning to reduce lag
            int currentCount = activeEnemyCount.get();
            if (currentCount > MAX_ENEMIES_HARD_CAP * 0.8) {
                // If we're at 80% of max capacity, skip spawning to prevent lag
                return;
            }

            double spawnInterval = waveSpawnRates.getOrDefault(currentWave, BASE_SPAWN_INTERVAL);
            double spawnChance = 0.6 / spawnInterval; // Reduced from 0.7 to 0.6 to control spawn rate
            int spawnCount = 1;

            // More frequent multi-spawns - Vampire Survivors style
            if (currentWave > 3 && random.nextDouble() < 0.2) { // Reduced from 0.25 to 0.2
                spawnCount = Math.min(currentWave / 4, 2); // Reduced max from 3 to 2 to decrease lag
            }

            if (random.nextDouble() < spawnChance) {
                for (int i = 0; i < spawnCount; i++) {
                    spawnEnemyForCurrentWave(survivalTime);
                }
            }
        };

        // Reduced frequency - check every 0.7 seconds instead of 0.5
        FXGL.getGameTimer().runAtInterval(spawnTask, Duration.seconds(0.7));
    }

    /**
     * Spawn an enemy based on the current wave's configuration
     * @param survivalTime Current survival time for scaling
     */
    private void spawnEnemyForCurrentWave(int survivalTime) {
        if (!isActive || player == null) {
            return;
        }

        List<String> enemyPool = waveEnemyPools.getOrDefault(currentWave, List.of("enemy"));
        String formation = waveFormations.getOrDefault(currentWave, "random");

        int baseMax = waveMaxEnemies.getOrDefault(currentWave, BASE_ENEMIES_PER_WAVE);
        double timeScaling = 1.0 + (survivalTime / 600.0); // Increased scaling (900 to 600)
        int maxEnemies = (int)(baseMax * timeScaling);
        maxEnemies = Math.min(maxEnemies, MAX_ENEMIES_HARD_CAP);
        int waveAllowance = Math.min(currentWave * 3, 30); // Increased from 2 to 3, and 20 to 30

        // Skip spawn if we exceed the enemy cap
        if (activeEnemyCount.get() >= maxEnemies + waveAllowance) {
            return;
        }

        String enemyType;
        
        // Special case for wave 3 - higher chance to spawn bees for swarming behavior
        if (currentWave == 3 && enemyPool.contains("beeEnemy") && random.nextDouble() < 0.90) { // Increased from 0.85 to 0.90
            enemyType = "beeEnemy";
        }
        // Special boss spawn logic with higher frequency - Vampire Survivors style
        else if (currentWave >= 4 && random.nextDouble() < 0.05 * (currentWave / 4.0)) { // Increased from 0.03 to 0.05
            int bossIndex = Math.min(enemyPool.size() - 1, enemyPool.size() - 2);
            enemyType = enemyPool.get(Math.max(bossIndex, 0));
        } else {
            enemyType = enemyPool.get(random.nextInt(enemyPool.size()));
        }

        // Standard random spawn is most common
        spawnEnemyOutsideViewport(enemyType);
    }

    /**
     * Cleanup enemies that are too far from the viewport to reduce entity count
     */
    private void cleanupOldEnemies() {
        // Throttle cleanups
        long now = System.nanoTime();
        if (now - lastCleanupTime < CLEANUP_FREQUENCY_MS * 1_000_000) {
            return;
        }
        lastCleanupTime = now;

        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY);
        int cleaned = 0;

        // More aggressive cleanup for performance
        int cleanupCap = Math.min(20, enemies.size() / 5); // Increased from 10 to 20 max cleanups per cycle

        // First prioritize enemies that are both old and far away
        for (Entity enemy : enemies) {
            if (cleaned >= cleanupCap) break;

            double x = enemy.getX();
            double y = enemy.getY();

            // Default to current time if missing
            long spawnTime = enemy.getProperties().exists("spawnTime") ?
                    enemy.getObject("spawnTime") : System.nanoTime();

            long lifetime = System.nanoTime() - spawnTime;
            boolean isOld = lifetime > ENEMY_LIFETIME_NS;
            boolean isFarAway = (x < viewMinX - VIEW_MARGIN || x > viewMaxX + VIEW_MARGIN ||
                    y < viewMinY - VIEW_MARGIN || y > viewMaxY + VIEW_MARGIN);

            // More aggressive cleanup
            if (isOld && isFarAway) {
                enemy.getComponent(EnemyComponent.class).die();
                cleaned++;
            }
        }

        // If we're near the enemy cap, be more aggressive with cleanup
        if (activeEnemyCount.get() > MAX_ENEMIES_HARD_CAP * 0.7 && cleaned < cleanupCap) {
            for (Entity enemy : enemies) {
                if (cleaned >= cleanupCap) break;
                
                double x = enemy.getX();
                double y = enemy.getY();
                boolean isFarAway = (x < viewMinX - VIEW_MARGIN || x > viewMaxX + VIEW_MARGIN ||
                        y < viewMinY - VIEW_MARGIN || y > viewMaxY + VIEW_MARGIN);
                
                if (isFarAway) {
                    enemy.getComponent(EnemyComponent.class).die();
                    cleaned++;
                }
            }
        }
    }

    /**
     * Announce a new wave with console prints and UI notifications
     */
    private void announceNewWave() {
        // Special wave handling - more frequent formations
        if (currentWave % 4 == 0) { // Changed from every 3rd to every 4th wave to reduce lag
            String formation = waveFormations.getOrDefault(currentWave, "random");
            
            // Delay special formation to let any frame drops recover
            FXGL.getGameTimer().runOnceAfter(() -> {
                String enemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));

                // Batch enemy formations to reduce individual entity creation overhead
                batchSpawnFormation(enemyType, formation);
            }, Duration.seconds(2)); // Reduced from 3 to 2 seconds for faster pacing
        }

        // Milestone waves with multi-formations
        if (currentWave % 8 == 0) { // Changed from every 6th to every 8th wave to reduce lag
            // Further delay to prevent lag by spacing out formation spawns
            FXGL.getGameTimer().runOnceAfter(() -> {
                String enemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));

                // Batch process first formation
                batchSpawnFormation(enemyType, "circle");
            }, Duration.seconds(3)); 

            // Add extra delay between formations to prevent lag spikes
            FXGL.getGameTimer().runOnceAfter(() -> {
                String secondEnemyType = waveEnemyPools.get(currentWave).get(
                        random.nextInt(waveEnemyPools.get(currentWave).size()));

                // Batch process second formation
                batchSpawnFormation(secondEnemyType, "spiral");
            }, Duration.seconds(8)); // Increased from 6 to 8 seconds to reduce lag
        }
    }

    /**
     * Batch process enemy formation spawns to reduce overhead
     * @param enemyType The type of enemy to spawn
     * @param formation The formation pattern to use
     */
    private void batchSpawnFormation(String enemyType, String formation) {
        formationInProgress = true;
        pendingSpawns.clear();

        switch (formation) {
            case "circle":
                prepareCircleFormation(enemyType);
                break;
            case "line":
                prepareLineFormation(enemyType);
                break;
            case "spiral":
                prepareSpiralFormation(enemyType);
                break;
            case "pincer":
                preparePincerFormation(enemyType); // New formation
                break;
            case "cross":
                prepareCrossFormation(enemyType); // New formation
                break;
            default:
                prepareCircleFormation(enemyType);
        }

        // Process spawn commands in batches to reduce overhead
        processPendingSpawns();
    }

    /**
     * Process pending spawn commands with efficient batching
     */
    private void processPendingSpawns() {
        if (pendingSpawns.isEmpty()) {
            formationInProgress = false;
            return;
        }

        // Group by delay to batch process
        Map<Integer, List<SpawnCommand>> delayGroups = new HashMap<>();
        for (SpawnCommand cmd : pendingSpawns) {
            delayGroups.computeIfAbsent(cmd.delay, k -> new ArrayList<>()).add(cmd);
        }

        // Process each delay group
        delayGroups.forEach((delay, commands) -> {
            FXGL.getGameTimer().runOnceAfter(() -> {
                // Execute all commands at this delay timing at once
                commands.forEach(cmd -> {
                    SpawnData data = new SpawnData(cmd.x, cmd.y);
                    data.put("player", player);
                    Entity enemy = FXGL.getGameWorld().spawn(cmd.type, data);
                    enemy.setProperty("spawnTime", System.nanoTime());
                    activeEnemyCount.incrementAndGet();
                });

                // Check if this was the last batch
                if (delay == delayGroups.keySet().stream().mapToInt(i -> i).max().orElse(0)) {
                    formationInProgress = false;
                }
            }, Duration.millis(delay));
        });

        pendingSpawns.clear();
    }

    /**
     * Prepare a circle formation of enemies (batched)
     * @param enemyType The type of enemy to spawn
     */
    private void prepareCircleFormation(String enemyType) {
        double radius = 800;
        // Reduce enemies in formations to improve performance
        final int count = Math.min(8 + Math.min((currentWave - 3), 7), 15); // Reduced from 20 to 15 max

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

            // Add to pending spawn commands instead of direct spawning
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 150 * i)); // Increased from 100 to 150ms for less lag
        }
    }

    /**
     * Prepare a line formation of enemies (batched)
     * @param enemyType The type of enemy to spawn
     */
    private void prepareLineFormation(String enemyType) {
        double distance = 800;
        // Reduce enemies in formations to improve performance
        int count = Math.min(6 + Math.min((currentWave - 3), 4), 10); // Reduced from 14 to 10 max
        double spacing = 100; // Increased from 80 to 100 to reduce entity count

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

            // Add to pending spawn commands instead of direct spawning
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms for less lag
        }
    }

    /**
     * Prepare a spiral formation of enemies (batched)
     * @param enemyType The type of enemy to spawn
     */
    private void prepareSpiralFormation(String enemyType) {
        // Reduce enemies in formations to improve performance
        final int count = Math.min(8 + Math.min((currentWave - 3), 7), 15); // Reduced from 20 to 15 max
        double baseRadius = 650; // Increased from 600 to 650
        double radiusIncrement = 50; // Increased from 40 to 50

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

            // Add to pending spawn commands instead of direct spawning
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms for less lag
        }
    }

    /**
     * Prepare a pincer formation that surrounds the player from two sides
     * @param enemyType The type of enemy to spawn
     */
    private void preparePincerFormation(String enemyType) {
        Point2D playerPos = player.getPosition();
        double spacing = 100; // Increased from 80 to 100
        int count = Math.min(5 + currentWave / 3, 8); // Reduced from 12 to 8 max
        
        // Determine random axis (horizontal or vertical)
        boolean isHorizontal = random.nextBoolean();
        
        double startX1, startY1, startX2, startY2;
        double dirX = 0, dirY = 0;
        
        if (isHorizontal) {
            // Left and right sides
            startX1 = playerPos.getX() - 800;
            startY1 = playerPos.getY() - (count * spacing) / 2;
            startX2 = playerPos.getX() + 800;
            startY2 = playerPos.getY() - (count * spacing) / 2;
            dirX = 0;
            dirY = 1;
        } else {
            // Top and bottom sides
            startX1 = playerPos.getX() - (count * spacing) / 2;
            startY1 = playerPos.getY() - 800;
            startX2 = playerPos.getX() - (count * spacing) / 2;
            startY2 = playerPos.getY() + 800;
            dirX = 1;
            dirY = 0;
        }
        
        // First side of pincer
        for (int i = 0; i < count; i++) {
            double x = startX1 + dirX * spacing * i;
            double y = startY1 + dirY * spacing * i;
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 150 * i)); // Increased from 100 to 150ms
        }
        
        // Second side of pincer
        for (int i = 0; i < count; i++) {
            double x = startX2 + dirX * spacing * i;
            double y = startY2 + dirY * spacing * i;
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 150 * i)); // Increased from 100 to 150ms
        }
    }

    /**
     * Prepare a cross formation that attacks from four directions
     * @param enemyType The type of enemy to spawn
     */
    private void prepareCrossFormation(String enemyType) {
        Point2D playerPos = player.getPosition();
        double distance = 800;
        int countPerArm = Math.min(3 + currentWave / 4, 6); // Reduced from 8 to 6 max
        double spacing = 120; // Increased from 100 to 120
        
        // Top arm
        for (int i = 0; i < countPerArm; i++) {
            double x = playerPos.getX();
            double y = playerPos.getY() - distance + (i * spacing);
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms
        }
        
        // Right arm
        for (int i = 0; i < countPerArm; i++) {
            double x = playerPos.getX() + distance - (i * spacing);
            double y = playerPos.getY();
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms
        }
        
        // Bottom arm
        for (int i = 0; i < countPerArm; i++) {
            double x = playerPos.getX();
            double y = playerPos.getY() + distance - (i * spacing);
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms
        }
        
        // Left arm
        for (int i = 0; i < countPerArm; i++) {
            double x = playerPos.getX() - distance + (i * spacing);
            double y = playerPos.getY();
            pendingSpawns.add(new SpawnCommand(enemyType, x, y, 200 * i)); // Increased from 150 to 200ms
        }
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
        activeEnemyCount.set(0);
        formationInProgress = false;
        pendingSpawns.clear();
        spawnTask = null;
        cleanupTask = null;
        FXGL.getWorldProperties().setValue("wave", 1);
        FXGL.getGameWorld().removeWorldListener(enemyRemovalListener);
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
        activeEnemyCount.incrementAndGet();
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
