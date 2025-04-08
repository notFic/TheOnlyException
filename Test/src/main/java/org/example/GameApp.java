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

    // GAME SETTINGS
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype");
        settings.setVersion("0.1.2");
    }

    // MOVEMENT KEY
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

        // GAMEWORLD SIZE
        getGameScene().setBackgroundColor(javafx.scene.paint.Color.SKYBLUE);
        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;
        player = spawn("player", worldWidth / 2.0, worldHeight / 2.0);

        // CAMERA FOLLOW PLAYER
        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        // SHOOT EVERY 1s
        FXGL.getGameTimer().runAtInterval(() -> {
            player.getComponent(PlayerComponent.class).shootTripleBurst();
        }, Duration.seconds(1));

        // SPAWN ENEMY EVERY 2s
        FXGL.getGameTimer().runAtInterval(() -> {
            spawnEnemyOutsideViewport("enemy");
        }, Duration.seconds(2));

        // SPAWN NIGGERS EVERY 5s
        FXGL.getGameTimer().runAtInterval(() -> {
            spawnEnemyOutsideViewport("fastEnemy");
        }, Duration.seconds(5));
    }

    private void spawnEnemyOutsideViewport(String enemyType) {
        // GET VIEWPORT BOUNDS
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();

        double x, y;
        int margin = 50; // How far outside the viewport to spawn

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

    // COLLISION
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