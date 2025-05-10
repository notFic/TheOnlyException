package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.OffscreenCleanComponent;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.components.LaserComponent;
import org.example.components.SwordComponent;
import org.example.components.VoltChainComponent;
import org.example.components.BulletComponent;
import org.example.components.DropComponent;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;
import org.example.components.FoodComponent;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameEntityFactory implements EntityFactory {

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
        int worldWidth = data.get("worldWidth"); // 2560
        int worldHeight = data.get("worldHeight"); // 1440

        // Create a canvas with the world size
        Canvas canvas = new Canvas(worldWidth, worldHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Load the worlda.png image (1536x1024)
        var backgroundImage = FXGL.image("worlda.png");
        if (backgroundImage == null) {
            System.err.println("Error: worlda.png not found. Using fallback color.");
            gc.setFill(Color.GREEN); // Fallback color if image is missing
            gc.fillRect(0, 0, worldWidth, worldHeight);
        } else {
            // Stretch the image to fill the entire world
            gc.drawImage(backgroundImage, 0, 0, worldWidth, worldHeight);
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
                .with(new EnemyComponent(player, 1.5, 15, 3, "bee"))
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
            hitbox.setFill(Color.color(1, 0, 0, 0.3)); // SEMI-TRANSPARENT RED
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
                .with(new EnemyComponent(player, 1.5, 15, 5, "maggot"))
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
            hitbox.setFill(Color.color(1, 0, 0, 0.3)); // SEMI-TRANSPARENT RED
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
                .with(new EnemyComponent(player, 1.5, 30, 10, "beetle"))
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
                .with(new EnemyComponent(player, 1.5, 120, 20, "giantfly"))
                .collidable()
                .build();
    }

    @Spawns("dragonflyEnemy")
    public Entity newDragonflyEnemy(SpawnData data) {
        Entity player = (Entity) data.get("player");

        Rectangle hitbox = new Rectangle(50, 50);

        if (showHitbox) {
            hitbox.setFill(Color.color(0.5, 0.5, 0.5, 0.3));
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
                .with(new EnemyComponent(player, 1.5, 50, 8, "dragonfly"))
                .collidable()
                .build();
    }

    @Spawns("tankEnemy")
    public Entity newTankEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);
        // HITBOX SIZE
        double width = 60;
        double height = 60;

        Rectangle hitbox = new Rectangle(width, height);

        if (showHitbox) {
            // VISIBLE HITBOX FOR DEBUGGING
            hitbox.setFill(Color.color(0.7, 0.7, 0.7, 0.3));
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
                .with(new EnemyComponent(player, 1.5, 60, 15, "mantis"))
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

    @Spawns("mine")
    public Entity newMine(SpawnData data) {
        var hitbox = new Rectangle(20, 20, Color.DARKORANGE);
        hitbox.setStroke(Color.ORANGE);
        hitbox.setStrokeWidth(1.5);
        return FXGL.entityBuilder(data)
                .type(EntityType.MINE)
                .view(hitbox)
                .bbox(new HitBox(BoundingShape.circle(10)))
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("laser")
    public Entity newLaser(SpawnData data) {
        Point2D direction = data.get("direction");
        double angle = Math.toDegrees(Math.atan2(direction.getY(), direction.getX()));

        return entityBuilder()
                .type(EntityType.LASER)
                .from(data)
                .viewWithBBox(new Rectangle(10, 100, Color.LIGHTBLUE))
                .rotate(angle - 90) // Rotate the laser so the tip faces the mouse
                .with(new LaserComponent())
                .with(new OffscreenCleanComponent())
                .collidable()
                .build();
    }

    @Spawns("slash")
    public Entity newSword(SpawnData data) {
        Point2D dir = data.get("direction");
        Point2D playerCenter = data.get("playerCenter");
        double radius = 50;
        double offsetDistance = radius * 0.5;

        Point2D offsetVector = dir.multiply(offsetDistance);
        Point2D circleCenter = playerCenter.add(offsetVector);

        Circle circle = new Circle();
        circle.setCenterX(radius); // Center the circle in its entity
        circle.setCenterY(radius);
        circle.setRadius(radius);
        circle.setFill(Color.YELLOW);
        circle.setStroke(Color.BLACK);

        Point2D entityPos = circleCenter.subtract(radius, radius);

        Entity slash = entityBuilder()
                .type(EntityType.SLASH)
                .from(data)
                .viewWithBBox(circle)
                .with(new SwordComponent(dir))
                .collidable()
                .at(entityPos)
                .build();

        double angleToMouse = Math.toDegrees(Math.atan2(dir.getY(), dir.getX()));
        slash.setRotation(angleToMouse);

        getGameTimer().runOnceAfter(() -> {
            slash.removeFromWorld();
        }, Duration.millis(100));

        return slash;
    }

    @Spawns("voltChain")
    public Entity newVoltChain(SpawnData data) {
        Point2D direction = data.get("direction");
        int chainCount = data.get("chainCount");

        return FXGL.entityBuilder(data)
                .type(EntityType.VOLT_CHAIN)
                .bbox(new HitBox(BoundingShape.box(10, 10)))
                .viewWithBBox(new Rectangle(10, 10, Color.YELLOW))
                .with(new ProjectileComponent(direction, 1000)) // <---- Here change speed
                .with(new VoltChainComponent(chainCount))
                .collidable()
                .build();
    }

    @Spawns("food")
    public Entity newFood(SpawnData data) {
        // Create a cross shape for the food
        var foodShape = new javafx.scene.shape.Path();
        foodShape.getElements().addAll(
            new javafx.scene.shape.MoveTo(0, 7.5),
            new javafx.scene.shape.LineTo(15, 7.5),
            new javafx.scene.shape.MoveTo(7.5, 0),
            new javafx.scene.shape.LineTo(7.5, 15)
        );
        foodShape.setStroke(Color.RED);
        foodShape.setStrokeWidth(3);

        // Create a background circle
        var background = new javafx.scene.shape.Circle(7.5, 7.5, 7.5);
        background.setFill(Color.RED);
        background.setOpacity(0.3);

        // Group the shapes
        var foodGroup = new javafx.scene.Group(background, foodShape);

        return entityBuilder()
                .type(EntityType.FOOD)
                .from(data)
                .view(foodGroup)
                .bbox(new HitBox(BoundingShape.box(15, 15)))
                .with(new FoodComponent())
                .collidable()
                .build();
    }
}