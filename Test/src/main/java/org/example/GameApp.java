package org.example;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
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

        // Set up a timer to make the player shoot 3 projectiles every 2 seconds
        FXGL.getGameTimer().runAtInterval(() -> {
            player.getComponent(PlayerComponent.class).shootTripleBurst();
        }, Duration.seconds(2));

        FXGL.run(() -> {
            FXGL.spawn("enemy", 100, 100);
        }, Duration.seconds(1));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
