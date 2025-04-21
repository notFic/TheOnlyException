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

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype");
        settings.setVersion("0.1.5");
        settings.setMainMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new NameInputScene();
            }
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("playerName", storedPlayerName);
        vars.put("score", 0);
        vars.put("health", 100);
        vars.put("survivalTime", 0);
        System.out.println("Game vars initialized with playerName: " + storedPlayerName);
    }

    @Override
    protected void onPreInit() {
        System.out.println("onPreInit called");
    }

    public static void startGameWithName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            System.out.println("Static method called with name: " + name);
            storedPlayerName = name;
        }
    }

    @Override
    protected void initUI() {
        Text nameText = getUIFactoryService().newText("", 20);
        nameText.textProperty().bind(
                getWorldProperties().stringProperty("playerName").concat("'s Game")
        );
        nameText.setFill(javafx.scene.paint.Color.WHITE);
        addUINode(nameText, 20, 20);

        Text healthText = getUIFactoryService().newText("", 24);
        healthText.textProperty().bind(
                getWorldProperties().intProperty("health").asString("Health: %d")
        );
        healthText.setFill(javafx.scene.paint.Color.RED);
        healthText.setStyle("-fx-font-weight: bold;");
        addUINode(healthText, 20, 50);

        Text timerText = getUIFactoryService().newText("", 24);
        timerText.textProperty().bind(
                getWorldProperties().intProperty("survivalTime").asString("Time: %d s")
        );
        timerText.setFill(javafx.scene.paint.Color.YELLOW);
        timerText.setStyle("-fx-font-weight: bold;");
        addUINode(timerText, 20, 80);
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

        // Reset game state for new session
        resetGameState();

        if (!storedPlayerName.equals("Unknown")) {
            FXGL.getWorldProperties().setValue("playerName", storedPlayerName);
            System.out.println("Re-applying stored player name: " + storedPlayerName);
        }

        String playerName = FXGL.getWorldProperties().getString("playerName");
        System.out.println("Player name from world properties: " + playerName);

        FXGL.runOnce(() -> {
            String currentName = FXGL.getWorldProperties().getString("playerName");
            System.out.println("Showing welcome notification for: " + currentName);
            FXGL.getNotificationService().pushNotification("Welcome, " + currentName + "!");
        }, Duration.seconds(0.2));

        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;

        SpawnData backgroundData = new SpawnData(0, 0);
        backgroundData.put("worldWidth", worldWidth);
        backgroundData.put("worldHeight", worldHeight);
        spawn("tiledBackground", backgroundData);

        SpawnData playerData = new SpawnData(worldWidth / 2.0, worldHeight / 2.0);
        playerData.put("gameApp", this);
        player = spawn("player", playerData);

        // Reset player state
        player.getComponent(PlayerComponent.class).resetPlayerState();

        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        isTimerRunning = true;
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getTime();
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning){
                player.getComponent(PlayerComponent.class).shootTripleBurst();
            }
        }, javafx.util.Duration.seconds(0.5));

        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("enemy");
        }, javafx.util.Duration.seconds(1));

        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("fastEnemy");
        }, javafx.util.Duration.seconds(2));

        FXGL.getGameTimer().runAtInterval(() -> {
            if(isTimerRunning)
                spawnEnemyOutsideViewport("tankEnemy");
        }, javafx.util.Duration.seconds(20));
    }

    public void stopTimer() {
        isTimerRunning = false;
    }

    public int getTime() {
        return getWorldProperties().getInt("survivalTime");
    }

    public void resetGameState() {
        getWorldProperties().setValue("survivalTime", 0);
        getWorldProperties().setValue("health", 100);
        getWorldProperties().setValue("score", 0);
        isTimerRunning = true;
        System.out.println("Game state reset for new session");
    }

    private void spawnEnemyOutsideViewport(String enemyType) {
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();

        double x, y;
        int margin = 50;

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

        if (random.nextDouble() < 0.2) {
            x = viewMinX + (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = viewMinY + (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            BulletComponent bulletComponent = bullet.getComponent(BulletComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            int damage = bulletComponent.getDamage();
            enemyComponent.damage(damage);
            bullet.removeFromWorld();
        });

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