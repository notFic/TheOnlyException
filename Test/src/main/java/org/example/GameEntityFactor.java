package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.OffscreenCleanComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameEntityFactor implements EntityFactory {

    /*
    dinhi i-define ang mga entities (player, bullet, enemy, etc) and ang sa pag
    create nila in a centralized way. if need mo mag create ug new na entity, refer to this file
    but when it comes to actually spawning them in game, refer to GameApp.java
     */

    private static final boolean showHitbox = false; // SWITCH TO TRUE FOR DEBUGGING PURPOSES

    @Spawns("background")
    public Entity newBackgroundr(SpawnData data) {
        return entityBuilder()
                .from(data)
                .view(new Rectangle(getAppWidth(), getAppHeight(), Color.SKYBLUE))
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
            hitbox.setFill(Color.color(0, 1, 0, 0.3)); // Semi-transparent green
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

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(new Rectangle(40, 40, Color.RED))
                .with(new EnemyComponent(player, 1.5, 50, 5)) // NORMAL
                .collidable()
                .build();
    }

    @Spawns("fastEnemy")
    public Entity newFastEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(new Rectangle(40, 40, Color.BLACK))
                .with(new EnemyComponent(player, 3.0, 30, 10)) // FAST nis
                .collidable()
                .build();
    }

    @Spawns("tankEnemy")
    public Entity newTankEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);

        return entityBuilder()
                .type(EntityType.ENEMY)
                .from(data)
                .viewWithBBox(new Rectangle(40, 40, Color.VIOLET))
                .with(new EnemyComponent(player, 1.0, 300, 15)) // SLOW nis
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