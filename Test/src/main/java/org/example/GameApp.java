package org.example;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player;
    private static String storedPlayerName = "Unknown";
    private Random random = new Random();
    private boolean isTimerRunning = true;

    // GAME SETTINGS
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype");
        settings.setVersion("0.1.5");

        // Set main menu to be shown
        settings.setMainMenuEnabled(true);

        // Set a custom SceneFactory to use our NameInputScene
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new NameInputScene();
            }
        });
    }

    // Add player's name, EXP, and level to global variables
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("playerName", storedPlayerName);
        vars.put("score", 0);
        vars.put("health", 100); // Initialize health to match PlayerComponent
        vars.put("survivalTime", 0);
        vars.put("level", 1); // Initialize level
        vars.put("exp", 0); // Initialize EXP
        // Debug
        System.out.println("Game vars initialized with playerName: " + storedPlayerName);
    }

    @Override
    protected void onPreInit() {
        // This runs before the game is fully initialized
        System.out.println("onPreInit called");
    }

    public static void startGameWithName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            System.out.println("Static method called with name: " + name);
            storedPlayerName = name; // Store in our static field
        }
    }

    @Override
    protected void initUI() {
        // Player name display
        Text nameText = getUIFactoryService().newText("", 20);
        nameText.textProperty().bind(
                getWorldProperties().stringProperty("playerName").concat("'s Game")
        );
        nameText.setFill(javafx.scene.paint.Color.WHITE);
        addUINode(nameText, 20, 20);

        // Health display
        Text healthText = getUIFactoryService().newText("", 24);
        healthText.textProperty().bind(
                getWorldProperties().intProperty("health").asString("Health: %d")
        );
        healthText.setFill(javafx.scene.paint.Color.RED);
        healthText.setStyle("-fx-font-weight: bold;");
        addUINode(healthText, 20, 50);

        // Timer display
        Text timerText = getUIFactoryService().newText("", 24);
        timerText.textProperty().bind(
                getWorldProperties().intProperty("survivalTime").asString("Time: %d s")
        );
        timerText.setFill(javafx.scene.paint.Color.YELLOW);
        timerText.setStyle("-fx-font-weight: bold;");
        addUINode(timerText, 20, 80);

        // Level display
        Text levelText = getUIFactoryService().newText("", 24);
        levelText.textProperty().bind(
                getWorldProperties().intProperty("level").asString("Level: %d")
        );
        levelText.setFill(javafx.scene.paint.Color.CYAN);
        levelText.setStyle("-fx-font-weight: bold;");
        addUINode(levelText, 20, 110);

        // EXP display
        Text expText = getUIFactoryService().newText("", 24);
        expText.textProperty().bind(
                getWorldProperties().intProperty("exp").asString("EXP: %d")
        );
        expText.setFill(javafx.scene.paint.Color.GREEN);
        expText.setStyle("-fx-font-weight: bold;");
        addUINode(expText, 20, 140);
    }

    @Override
    protected void initInput() {
        onKey(KeyCode.A, () -> player.getComponent(PlayerComponent.class).moveLeft());
        onKey(KeyCode.D, () -> player.getComponent(PlayerComponent.class).moveRight());
        onKey(KeyCode.W, () -> player.getComponent(PlayerComponent.class).moveUp());
        onKey(KeyCode.S, () -> player.getComponent(PlayerComponent.class).moveDown());
    }

    @Override
    protected void initGame() {
        FXGL.getGameWorld().addEntityFactory(new GameEntityFactor());

        // Double check the player name and ensure it's correctly set
        if (!storedPlayerName.equals("Unknown")) {
            FXGL.getWorldProperties().setValue("playerName", storedPlayerName);
            System.out.println("Re-applying stored player name: " + storedPlayerName);
        }

        String playerName = FXGL.getWorldProperties().getString("playerName");
        System.out.println("Player name from world properties: " + playerName);

        // Display welcome message with player's name
        FXGL.runOnce(() -> {
            String currentName = FXGL.getWorldProperties().getString("playerName");
            System.out.println("Showing welcome notification for: " + currentName);
            FXGL.getNotificationService().pushNotification("Welcome, " + currentName + "!");
        }, Duration.seconds(0.2));

        // GAMEWORLD SIZE
        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;

        // SPAWN BG TILES
        SpawnData backgroundData = new SpawnData(0, 0);
        backgroundData.put("worldWidth", worldWidth);
        backgroundData.put("worldHeight", worldHeight);
        spawn("tiledBackground", backgroundData);

        // Pass GameApp reference to player
        SpawnData playerData = new SpawnData(worldWidth / 2.0, worldHeight / 2.0);
        playerData.put("gameApp", this);
        player = spawn("player", playerData);

        // CAMERA FOLLOW PLAYER
        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        //Timer start
        isTimerRunning = true;
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getWorldProperties().getInt("survivalTime");
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        // SHOOT EVERY 1s
        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning){
                player.getComponent(PlayerComponent.class).shootTripleBurst();
            }
        }, javafx.util.Duration.seconds(0.5));

        // SPAWN ENEMY EVERY 2s
        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("enemy");
        }, javafx.util.Duration.seconds(1));

        // SPAWN nis EVERY 5s
        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("fastEnemy");
        }, javafx.util.Duration.seconds(2));

        // SPAWN nis EVERY 5s
        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("tankEnemy");
        }, javafx.util.Duration.seconds(3));
    }

    public void stopTimer(){
        isTimerRunning = false;
    }

    private void spawnEnemyOutsideViewport(String enemyType) {
        // GET VIEWPORT BOUNDS
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();

        double x, y;
        int margin = 50; // HOW FAR OUTSIDE OF VIEWPORT TO SPAWN

        // CHOOSE WHICH SIDE TO SPAWN
        int side = random.nextInt(4);

        switch (side) {
            case 0: // TOP
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMinY - margin;
                break;
            case 1: // RIGHT
                x = viewMaxX + margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            case 2: // BOTTOM
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMaxY + margin;
                break;
            case 3: // LEFT
                x = viewMinX - margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            default:
                x = viewMinX;
                y = viewMinY;
        }

        // SPAWN CORNERS
        if (random.nextDouble() < 0.2) { // 20% CHANCE TO SPAWN IN CORNER
            x = viewMinX + (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = viewMinY + (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    @Override
    protected void initPhysics() {
        // BULLET DAMAGE TO ENEMY
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            BulletComponent bulletComponent = bullet.getComponent(BulletComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            int damage = bulletComponent.getDamage();

            enemyComponent.damage(damage);

            bullet.removeFromWorld();
        });

        // ENEMY DAMAGE TO PLAYER
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}