package org.example;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player;
    private Random random = new Random();

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype");
        settings.setVersion("0.1");
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

        player = spawn("player", getAppWidth() / 2.0, getAppHeight() / 2.0);

        // Set up a timer to make the player shoot 3 projectiles every 1 second
        FXGL.getGameTimer().runAtInterval(() -> {
            player.getComponent(PlayerComponent.class).shootTripleBurst();
        }, Duration.seconds(1));

        // Spawn regular enemies every 2 seconds
        FXGL.getGameTimer().runAtInterval(() -> {
            spawnEnemyOutsideScreen("enemy");
        }, Duration.seconds(2));

        // Spawn fast enemies every 5 seconds
        FXGL.getGameTimer().runAtInterval(() -> {
            spawnEnemyOutsideScreen("fastEnemy");
        }, Duration.seconds(5));
    }

    private void spawnEnemyOutsideScreen(String enemyType) {
        // Choose a random side (0=top, 1=right, 2=bottom, 3=left)
        int side = random.nextInt(4);

        // Position variables
        double x, y;
        int margin = 50; // How far outside the screen to spawn

        switch (side) {
            case 0: // Top
                x = random.nextDouble() * getAppWidth();
                y = -margin;
                break;
            case 1: // Right
                x = getAppWidth() + margin;
                y = random.nextDouble() * getAppHeight();
                break;
            case 2: // Bottom
                x = random.nextDouble() * getAppWidth();
                y = getAppHeight() + margin;
                break;
            case 3: // Left
                x = -margin;
                y = random.nextDouble() * getAppHeight();
                break;
            default:
                x = 0;
                y = 0;
        }

        // Also randomly spawn at corners sometimes
        if (random.nextDouble() < 0.2) { // 20% chance to spawn at corners
            x = (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            System.out.println("Collision!");
            bullet.removeFromWorld();
            enemy.removeFromWorld();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}