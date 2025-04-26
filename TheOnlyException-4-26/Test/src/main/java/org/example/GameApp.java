package org.example;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

// Main game application class managing game world, UI, and physics
public class GameApp extends GameApplication {

    private Entity player; // Player entity
    private static String storedPlayerName = "Unknown"; // Player's username
    private Random random = new Random(); // For enemy spawning
    private boolean isTimerRunning = true; // Controls game timers
    private String userType = "Gun";

    // Configure game window and main menu
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype");
        settings.setVersion("0.1.5");
//        settings.setMainMenuEnabled(true);
//        settings.setSceneFactory(new SceneFactory() {
//            @Override
//            public FXGLMenu newMainMenu() {
//                return new NameInputScene();
//            }
//        });
    }

    // Initialize global game variables
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("playerName", storedPlayerName);
        vars.put("score", 0);
        vars.put("health", 100);
        vars.put("survivalTime", 0);
        vars.put("level", 1);
        vars.put("exp", 0);
        vars.put("totalDamage", 0); // Tracks damage dealt
        vars.put("kills", 0); // Tracks enemies killed
        System.out.println("Game vars initialized with playerName: " + storedPlayerName);
    }

    // Store player's username
    public static void startGameWithName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            System.out.println("Static method called with name: " + name);
            storedPlayerName = name;
        }
    }

    // Setup UI elements (name, health, timer, level, EXP)
    @Override
    protected void initUI() {
        Text nameText = getUIFactoryService().newText("", 20);
        nameText.textProperty().bind(getWorldProperties().stringProperty("playerName").concat("'s Game"));
        nameText.setFill(Color.WHITE);
        addUINode(nameText, 20, 20);

        Text healthText = getUIFactoryService().newText("", 24);
        healthText.textProperty().bind(getWorldProperties().intProperty("health").asString("Health: %d"));
        healthText.setFill(Color.RED);
        healthText.setStyle("-fx-font-weight: bold;");
        addUINode(healthText, 20, 50);

        Text timerText = getUIFactoryService().newText("", 24);
        timerText.textProperty().bind(getWorldProperties().intProperty("survivalTime").asString("Time: %d s"));
        timerText.setFill(Color.YELLOW);
        timerText.setStyle("-fx-font-weight: bold;");
        addUINode(timerText, 20, 80);

        Text levelText = getUIFactoryService().newText("", 24);
        levelText.textProperty().bind(getWorldProperties().intProperty("level").asString("Level: %d"));
        levelText.setFill(Color.CYAN);
        levelText.setStyle("-fx-font-weight: bold;");
        addUINode(levelText, 20, 110);

        Text expText = getUIFactoryService().newText("", 24);
        expText.textProperty().bind(getWorldProperties().intProperty("exp").asString("EXP: %d"));
        expText.setFill(Color.YELLOWGREEN);
        expText.setStyle("-fx-font-weight: bold;");
        addUINode(expText, 20, 140);
    }

    // Bind movement keys (WASD) to player actions
    @Override
    protected void initInput() {
        onKey(KeyCode.A, () -> player.getComponent(PlayerComponent.class).moveLeft());
        onKey(KeyCode.D, () -> player.getComponent(PlayerComponent.class).moveRight());
        onKey(KeyCode.W, () -> player.getComponent(PlayerComponent.class).moveUp());
        onKey(KeyCode.S, () -> player.getComponent(PlayerComponent.class).moveDown());
    }

    // Initialize game world, spawn entities, and setup timers
    @Override
    protected void initGame() {
        // Clear previous game state
        System.out.println("Clearing existing entities from game world");
        FXGL.getGameWorld().getEntities().forEach(Entity::removeFromWorld);
        FXGL.getGameTimer().clear();

        resetGameState(); // Reset game variables and player state

        FXGL.getGameWorld().addEntityFactory(new GameEntityFactor());

        // Ensure player name is set
        if (!storedPlayerName.equals("Unknown")) {
            FXGL.getWorldProperties().setValue("playerName", storedPlayerName);
            System.out.println("Re-applying stored player name: " + storedPlayerName);
        }

        String playerName = FXGL.getWorldProperties().getString("playerName");
        System.out.println("Player name from world properties: " + playerName);

        // Show welcome notification
        FXGL.runOnce(() -> {
            String currentName = FXGL.getWorldProperties().getString("playerName");
            System.out.println("Showing welcome notification for: " + currentName);
            FXGL.getNotificationService().pushNotification("Welcome, " + currentName + "!");
        }, Duration.seconds(0.2));

        // Set game world dimensions
        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;

        // Spawn background
        SpawnData backgroundData = new SpawnData(0, 0);
        backgroundData.put("worldWidth", worldWidth);
        backgroundData.put("worldHeight", worldHeight);
        spawn("tiledBackground", backgroundData);

        // Spawn player
        SpawnData playerData = new SpawnData(worldWidth / 2.0, worldHeight / 2.0);
        playerData.put("gameApp", this);
        player = spawn("player", playerData);

        // Center camera on player
        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        // Start survival timer
        isTimerRunning = true;
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getWorldProperties().getInt("survivalTime");
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        if(userType.equals("Sword")){
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    player.getComponent(PlayerComponent.class).shootTripleBurst();
                }
            }, Duration.seconds(0.5));
        } else {
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    player.getComponent(PlayerComponent.class).swordSlash();
                }
            }, Duration.seconds(0.5));
        }

        // Spawn enemies at intervals
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("enemy");
        }, Duration.seconds(1));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("fastEnemy");
        }, Duration.seconds(2));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("tankEnemy");
        }, Duration.seconds(3));
    }

    // Stop all game timers
    public void stopTimer() {
        isTimerRunning = false;
    }

    // Spawn enemies outside the viewport
    private void spawnEnemyOutsideViewport(String enemyType) {
        // Get viewport bounds
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();

        // Determine spawn position
        double x, y;
        int margin = 50; // Distance outside viewport

        int side = random.nextInt(4);
        switch (side) {
            case 0: // Top
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMinY - margin;
                break;
            case 1: // Right
                x = viewMaxX + margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            case 2: // Bottom
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMaxY + margin;
                break;
            case 3: // Left
                x = viewMinX - margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            default:
                x = viewMinX;
                y = viewMinY;
        }

        // 20% chance to spawn in corners
        if (random.nextDouble() < 0.2) {
            x = viewMinX + (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = viewMinY + (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    // Define collision physics (bullet-enemy, player-enemy)
    @Override
    protected void initPhysics() {
        // Bullet hits enemy
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            BulletComponent bulletComponent = bullet.getComponent(BulletComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

            int damage = bulletComponent.getDamage();
            enemyComponent.damage(damage);
            FXGL.getWorldProperties().increment("totalDamage", damage); // Track damage
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1); // Track kills
            }
            bullet.removeFromWorld();
        });

        // Enemy damages player
        onCollision(EntityType.PLAYER, EntityType.ENEMY, (player, enemy) -> {
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

            long now = System.nanoTime();
            if (now - enemyComponent.getLastDamageTime() >= 1_000_000_000) {
                int damage = enemyComponent.getDamage();
                playerComponent.damage(damage);
                enemyComponent.setLastDamageTime(now);
            }
        });

        // Slash hits enemy
        onCollisionBegin(EntityType.SLASH, EntityType.ENEMY, (slash, enemy) -> {
            // Assuming sword has a component like BulletComponent for damage info
            SwordComponent swordComponent = slash.getComponent(SwordComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            int damage = swordComponent.getDamage();
            enemyComponent.damage(damage);
            FXGL.getWorldProperties().increment("totalDamage", damage); // Track damage
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1); // Track kills
            }
            slash.removeFromWorld();
        });

    }

    // Reset game state for a new session
    public void resetGameState() {
        FXGL.getInput().clearAll(); // Clear input mappings
        getWorldProperties().setValue("survivalTime", 0);
        getWorldProperties().setValue("health", 100);
        getWorldProperties().setValue("score", 0);
        getWorldProperties().setValue("level", 1);
        getWorldProperties().setValue("exp", 0);
        getWorldProperties().setValue("totalDamage", 0);
        getWorldProperties().setValue("kills", 0);
        isTimerRunning = true;
        System.out.println("Game state reset for new session");

        // Reset player state if entity exists
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).resetPlayerState();
        }
    }

    // Launch the game
    public static void main(String[] args) {
        launch(args);
    }
}