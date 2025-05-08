package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.OffscreenCleanComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameEntityFactor implements EntityFactory {

    /*
    dinhi i-define ang mga entities (player, bullet, enemy, etc) and ang sa pag
    create nila in a centralized way. if need mo mag create ug new na entity, refer to this file
    but when it comes to actually spawning them in game, refer to GameApp.java
     */

    private static final boolean showHitbox = false; // SWITCH TO TRUE FOR DEBUGGING PURPOSES

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        return entityBuilder()
                .from(data)
                .view(new Rectangle(getAppWidth(), getAppHeight(), Color.SKYBLUE))
                .build();
    }

    @Spawns("tiledBackground")
    public Entity newTiledBackground(SpawnData data) {
        int worldWidth = data.get("worldWidth");
        int worldHeight = data.get("worldHeight");

        // TILE SIZE
        double tileWidth = 24;
        double tileHeight = 24;

        int tilesX = (int) Math.ceil((double) worldWidth / tileWidth);
        int tilesY = (int) Math.ceil((double) worldHeight / tileHeight);

        // CREATE CANVAS WITH FULL WORLD SIZE
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(worldWidth, worldHeight);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // TEMPORARY BACKGROUND || GENERIC AHH GRASS
        var backgroundImage = FXGL.image("dasd.png");

        // DRAW THE TILE PATTERN
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                gc.drawImage(
                        backgroundImage,
                        0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(),
                        x * tileWidth, y * tileHeight, tileWidth, tileHeight
                );
            }
        }

        return entityBuilder()
                .type(EntityType.BACKGROUND)
                .at(0, 0)
                .view(canvas)
                .zIndex(-100)
                .build();
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        // HITBOX SIZE
        double width = 24;
        double height = 45;

        Rectangle hitbox = new Rectangle(width, height);

        if (showHitbox) {
            // VISIBLE HITBOX FOR DEBUGGING
            hitbox.setFill(Color.color(0, 1, 0, 0.3)); // SEMI-TRANSPARENT GREEN
            hitbox.setStroke(Color.RED);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.PLAYER)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new PlayerComponent())
                .collidable()
                .build();
    }

    @Spawns("beeEnemy")
    public Entity newBeeEnemy(SpawnData data) {
        Entity player = (Entity) data.get("player");

        Rectangle hitbox = new Rectangle(40, 20); // adjust to sprite

        if (showHitbox) {
            hitbox.setFill(Color.color(1, 1, 0, 0.3)); // yellow tint
            hitbox.setStroke(Color.BLACK);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 3.5, 25, 3, "bee"))
                .collidable()
                .build();
    }


    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);
        // HITBOX SIZE
        double width = 45;
        double height = 20;

        Rectangle hitbox = new Rectangle(width, height);

        if (showHitbox) {
            // VISIBLE HITBOX FOR DEBUGGING
            hitbox.setFill(Color.color(1, 0, 0, 0.3)); // // SEMI-TRANSPARENT RED
            hitbox.setStroke(Color.GREEN);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 1.5, 50, 5, "maggot")) // NORMAL
                .collidable()
                .build();
    }

    @Spawns("fastEnemy")
    public Entity newFastEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);
        // HITBOX SIZE
        double width = 45;
        double height = 20;

        Rectangle hitbox = new Rectangle(width, height);

        if (showHitbox) {
            // VISIBLE HITBOX FOR DEBUGGING
            hitbox.setFill(Color.color(1, 0, 0, 0.3)); // // SEMI-TRANSPARENT RED
            hitbox.setStroke(Color.GREEN);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 3.0, 30, 10, "beetle")) // FAST nis
                .collidable()
                .build();
    }

    @Spawns("giantFlyEnemy")
    public Entity newGiantFlyEnemy(SpawnData data) {
        Entity player = (Entity) data.get("player");

        Rectangle hitbox = new Rectangle(64, 64); // big hitbox

        if (showHitbox) {
            hitbox.setFill(Color.color(0.6, 0.6, 0.6, 0.3));
            hitbox.setStroke(Color.BLACK);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 0.9, 500, 20, "giantfly")) // slow, tanky, strong
                .collidable()
                .build();
    }

    @Spawns("dragonflyEnemy")
    public Entity newDragonflyEnemy(SpawnData data) {
        Entity player = (Entity) data.get("player");

        Rectangle hitbox = new Rectangle(48, 48); // match size

        if (showHitbox) {
            hitbox.setFill(Color.color(0.4, 1.0, 1.0, 0.3));
            hitbox.setStroke(Color.BLACK);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 2.8, 80, 12, "dragonfly")) // moderate stats
                .collidable()
                .build();
    }


    @Spawns("tankEnemy")
    public Entity newTankEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);
        // HITBOX SIZE
        double width = 45;
        double height = 50;

        Rectangle hitbox = new Rectangle(width, height);

        if (showHitbox) {
            // VISIBLE HITBOX FOR DEBUGGING
            hitbox.setFill(Color.color(1, 0, 0, 0.3)); // // SEMI-TRANSPARENT RED
            hitbox.setStroke(Color.GREEN);
            hitbox.setStrokeWidth(2);
        } else {
            hitbox.setFill(Color.TRANSPARENT);
            hitbox.setStroke(Color.TRANSPARENT);
        }

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(hitbox)
                .with(new EnemyComponent(player, 1.0, 300, 15, "mantis")) // SLOW nis
                .collidable()
                .build();
    }

    @Spawns("bullet")
    public Entity newBullet(SpawnData data) {
        return entityBuilder()
                .type(EntityType.BULLET)
                .from(data)
                .viewWithBBox(new Rectangle(10, 10, Color.BLACK))
                .with(new BulletComponent())
                .with(new OffscreenCleanComponent())
                .collidable()
                .build();
    }

    @Spawns("drop")
    public Entity newDrop(SpawnData data) {
        return entityBuilder()
                .type(EntityType.DROP)
                .from(data)
                .viewWithBBox(new Rectangle(15, 15, Color.GOLD))
                .with(new DropComponent())
                .collidable()
                .build();
    }
}