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

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameEntityFactor implements EntityFactory {

    @Spawns("background")
    public Entity newBackgroundr(SpawnData data) {
        return entityBuilder()
                .from(data)
                .view(new Rectangle(getAppWidth(), getAppHeight(), Color.SKYBLUE))
                .build();
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        return entityBuilder()
                .type(EntityType.PLAYER)
                .from(data)
                .view(new Rectangle(40, 40, Color.BLUE))
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
                .with(new EnemyComponent(player, 1.5)) // Regular speed enemy
                .collidable()
                .build();
    }

    @Spawns("fastEnemy")
    public Entity newFastEnemy(SpawnData data) {
        Entity player = (Entity) data.getData().getOrDefault("player", null);

        return entityBuilder()
                .type(EntityType.ENEMY) // Still using ENEMY type for collision detection
                .from(data)
                .viewWithBBox(new Rectangle(40, 40, Color.BLACK))
                .with(new EnemyComponent(player, 3.0)) // Fast enemy - double the speed
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
}