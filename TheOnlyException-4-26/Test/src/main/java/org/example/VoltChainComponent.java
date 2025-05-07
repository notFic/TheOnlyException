package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;

public class VoltChainComponent extends Component {
    private int chainCount;

    public VoltChainComponent(int chainCount) {
        this.chainCount = chainCount;
    }

//    @Override
//    public void onAdded() {
//
//    }

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
}

