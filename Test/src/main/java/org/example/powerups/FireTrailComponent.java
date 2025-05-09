package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.components.EnemyComponent;
import org.example.core.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FireTrailComponent extends Component {

    private static final double TRAIL_SPAWN_DISTANCE = 20; // Distance between trail segments
    private static final double TRAIL_LIFETIME = 3.0;
    private static final double DAMAGE_INTERVAL = 0.5;
    private static final double DAMAGE_AMOUNT = 5.0;
    private static final double TRAIL_SIZE = 40;
    private static final double BURN_DURATION = 3.0;
    private static final double BURN_DAMAGE = 10;

    private boolean isActive = false;

    private final List<Entity> trailSegments = new ArrayList<>();
    private com.almasb.fxgl.time.TimerAction damageDealer;

    private Map<Entity, Double> burningEnemies = new HashMap<>();
    private Point2D lastTrailSpawnPosition; // Track the last position

    @Override
    public void onAdded() {
        activatePowerUp(); // For testing
    }

    public void activatePowerUp() {
        if (isActive) return;
        isActive = true;

        if (damageDealer != null) damageDealer.expire();

        startDamagingEnemies();
        lastTrailSpawnPosition = entity.getCenter(); // Initialize
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;

        if (damageDealer != null) damageDealer.expire();

        clearTrail();
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        Point2D currentPosition = entity.getCenter();
        double distanceMoved = currentPosition.distance(lastTrailSpawnPosition);

        if (distanceMoved >= TRAIL_SPAWN_DISTANCE) {
            spawnTrailSegment();
            lastTrailSpawnPosition = currentPosition;
        }

        if (!trailSegments.isEmpty()) {
            damageEnemiesTouchingTrail();
        }
    }

    private void startDamagingEnemies() {
        damageDealer = FXGL.getGameTimer().runAtInterval(this::damageEnemiesTouchingTrail, Duration.seconds(DAMAGE_INTERVAL));
    }

    private void spawnTrailSegment() {
        Point2D playerCenter = entity.getCenter();

        Rectangle trailVisual = new Rectangle(TRAIL_SIZE, TRAIL_SIZE, Color.color(1, 0.3, 0, 0.5));
        trailVisual.setStroke(Color.RED);
        trailVisual.setStrokeWidth(1.5);

        Entity trail = FXGL.entityBuilder()
                .type(EntityType.FIRETRAIL)
                .at(playerCenter.subtract(TRAIL_SIZE / 2, TRAIL_SIZE / 2))
                .view(trailVisual)
                .with(new CollidableComponent(true))
                .bbox(new HitBox(BoundingShape.box(TRAIL_SIZE, TRAIL_SIZE)))
                .zIndex(500)
                .buildAndAttach();

        trailSegments.add(trail);

        FXGL.getGameTimer().runOnceAfter(() -> {
            trail.removeFromWorld();
            trailSegments.remove(trail);
        }, Duration.seconds(TRAIL_LIFETIME));
    }

    private void damageEnemiesTouchingTrail() {
        List<Entity> touchingEnemies = FXGL.getGameWorld()
                .getEntitiesByType(EntityType.ENEMY)
                .stream()
                .filter(e -> e.isActive() &&
                        trailSegments.stream().anyMatch(t ->
                                e.getBoundingBoxComponent().isCollidingWith(t.getBoundingBoxComponent())))
                .collect(Collectors.toList());

        touchingEnemies.forEach(enemy -> {
            enemy.getComponentOptional(EnemyComponent.class)
                    .ifPresent(ec -> ec.damage(DAMAGE_AMOUNT, enemy.getCenter()));
            burningEnemies.put(enemy, BURN_DURATION);
        });

        burningEnemies.entrySet().removeIf(entry -> {
            Entity enemy = entry.getKey();
            double remaining = entry.getValue() - DAMAGE_INTERVAL;

            if (remaining <= 0 || !enemy.isActive()) {
                return true;
            }

            if (!touchingEnemies.contains(enemy)) {
                enemy.getComponentOptional(EnemyComponent.class)
                        .ifPresent(ec -> ec.damage(BURN_DAMAGE, enemy.getCenter()));
            }

            burningEnemies.put(enemy, remaining);
            return false;
        });
    }

    private void clearTrail() {
        new ArrayList<>(trailSegments).forEach(Entity::removeFromWorld);
        trailSegments.clear();
    }
}