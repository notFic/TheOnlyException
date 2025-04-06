package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.RandomMoveComponent;
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
                .from(data)
                .view(new Rectangle(40, 40, Color.BLUE))
                .with(new PlayerComponent())
                .build();
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        return entityBuilder()
                .from(data)
                .view(new Rectangle(40, 40, Color.RED))
                .with(new RandomMoveComponent(
                        new Rectangle2D(0, 0, getAppWidth(), getAppHeight()), 150))
                .build();
    }

    @Spawns("bullet")
    public Entity newBullet(SpawnData data) {
        return entityBuilder()
                .from(data)
                .type(EntityType.BULLET)
                .viewWithBBox(new Rectangle(10, 10, Color.BLACK))
                .with(new BulletComponent())
                .build();
    }
}