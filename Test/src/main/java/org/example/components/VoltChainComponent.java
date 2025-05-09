package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;

public class VoltChainComponent extends Component {
    private int chainCount;
    private int frameCounter = 0;

    public VoltChainComponent(int chainCount) {
        this.chainCount = chainCount;
    }

    public void onHitEnemy(Entity enemy) {
        if (chainCount <= 0) return;

        Point2D currentPos = entity.getCenter();
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).stream()
                .filter(e -> e != enemy && e.isActive())
                .sorted(Comparator.comparingDouble(e -> e.getCenter().distance(currentPos)))
                .toList();

        if (!enemies.isEmpty()) {
            Entity nextTarget = enemies.get(0);
            Point2D direction = nextTarget.getCenter().subtract(currentPos).normalize();

            FXGL.spawn("voltChain", new SpawnData(currentPos)
                    .put("direction", direction)
                    .put("chainCount", chainCount - 1));
        }
    }

    @Override
    public void onUpdate(double tpf) {
        frameCounter++;
        if (frameCounter % 1 == 0) { // <--- frameCounter % [n] // lesser in n, closer the trail
            spawnTrailSegment();
        }
    }

    private void spawnTrailSegment() {
        Point2D currentPos = entity.getCenter();
        Point2D velocity = entity.getComponent(ProjectileComponent.class).getDirection().multiply(-10);
        Point2D trailPos = currentPos.add(velocity);
        double distance = trailPos.distance(currentPos);
        double maxDistance = 20.0;
        double opacity = 1.0 - (distance / maxDistance);
        opacity = Math.max(0.2, Math.min(opacity, 1.0));
        Rectangle rect = new Rectangle(6, 6, Color.YELLOW);
        rect.setOpacity(opacity);

        Entity trail = FXGL.entityBuilder()
                .at(trailPos)
                .view(rect)
                .zIndex(5)
                .buildAndAttach();
        FXGL.getGameTimer().runOnceAfter(trail::removeFromWorld, Duration.seconds(0.2));
    }


}

