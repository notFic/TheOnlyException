package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.EnemyComponent;
import org.example.EntityType;
import org.example.PlayerComponent;

public class ExplosiveMinesComponent extends Component {
    private static final double SPAWN_INTERVAL = 2.0; // Seconds between mine spawns
    private static final double MINE_LIFETIME = 5.0; // Seconds before explosion
    private static final double EXPLOSION_RADIUS = 100.0; // Base explosion radius, matching PoisonAura
    private static final double EXPLOSION_RADIUS_UPGRADED = 150.0; // With Heap Overflow
    private static final double EXPLOSION_DAMAGE = 20.0; // Damage dealt by explosion
    private static final double BURN_ZONE_DURATION = 1.0; // Burn zone lifetime
    private static final double BURN_ZONE_DAMAGE = 5.0; // Damage per tick
    private static final double BURN_ZONE_TICK_INTERVAL = 0.5; // Seconds between ticks
    private static final double EXPLOSION_VISUAL_DURATION = 0.5; // Seconds for visual effect

    private boolean isActive = false;
    private double spawnTimer = 0.0;
    private int level = 0;

    @Override
    public void onAdded() {
        level = entity.getComponent(PlayerComponent.class).getWeaponLevel("explosive_mines");
        activatePowerUp();
    }

    public void activatePowerUp() {
        if (isActive) return;
        isActive = true;
        spawnTimer = 0.0;
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        spawnTimer += tpf;
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnMine();
            spawnTimer = 0.0;
        }
    }

    private void spawnMine() {
        Point2D playerCenter = entity.getCenter();

        Circle mineVisual = new Circle(10, Color.DARKORANGE);
        mineVisual.setStroke(Color.ORANGE);
        mineVisual.setStrokeWidth(1.5);

        Entity mine = FXGL.entityBuilder()
                .type(EntityType.MINE)
                .at(playerCenter.subtract(10, 10))
                .view(mineVisual)
                .with(new CollidableComponent(true))
                .bbox(new HitBox(BoundingShape.circle(10)))
                .zIndex(500)
                .buildAndAttach();

        mine.addComponent(new MineBehaviorComponent());
    }

    private class MineBehaviorComponent extends Component {
        private double lifetime = 0.0;

        @Override
        public void onUpdate(double tpf) {
            lifetime += tpf;

            if (lifetime >= MINE_LIFETIME) {
                explode();
                entity.removeFromWorld();
            }
        }

        private void explode() {
            Point2D center = entity.getCenter();
            double radius = level >= 2 ? EXPLOSION_RADIUS_UPGRADED : EXPLOSION_RADIUS;

            // Visual explosion effect (centered circle)
            Circle explosionVisual = new Circle(radius, Color.color(1.0, 0.5, 0.0, 0.3));
            explosionVisual.setStroke(Color.ORANGE);
            explosionVisual.setStrokeWidth(2.0);
            Entity explosionEntity = FXGL.entityBuilder()
                    .at(center)
                    .view(explosionVisual)
                    .zIndex(1000)
                    .buildAndAttach();
            FXGL.getGameTimer().runOnceAfter(explosionEntity::removeFromWorld, Duration.seconds(EXPLOSION_VISUAL_DURATION));

            // Particle effect for explosion
            ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(50);
            try {
                emitter.setSourceImage(FXGL.image("orange_particle.png"));
            } catch (Exception e) {
                System.err.println("Warning: orange_particle.png not found, using default particle effect");
            }
            emitter.setSize(2, 5);
            emitter.setNumParticles(20);
            emitter.setEmissionRate(0.5);
            emitter.setExpireFunction(i -> Duration.seconds(0.5));
            Entity particleEntity = FXGL.entityBuilder()
                    .at(center.subtract(25, 25))
                    .with(new ParticleComponent(emitter))
                    .zIndex(1000)
                    .buildAndAttach();
            FXGL.getGameTimer().runOnceAfter(particleEntity::removeFromWorld, Duration.seconds(0.5));

            // Damage enemies in radius
            FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).stream()
                    .filter(e -> e.isActive() && e.getCenter().distance(center) <= radius)
                    .forEach(e -> e.getComponent(EnemyComponent.class).damage(EXPLOSION_DAMAGE, e.getCenter()));

            // Spawn burn zone if level 2 (Heap Overflow)
            if (level >= 2) {
                Rectangle burnVisual = new Rectangle(50, 50, Color.color(1.0, 0.5, 0.0, 0.5));
                burnVisual.setStroke(Color.ORANGE);
                burnVisual.setStrokeWidth(1.5);

                Entity burnZone = FXGL.entityBuilder()
                        .type(EntityType.BURN_ZONE)
                        .at(center.subtract(25, 25))
                        .view(burnVisual)
                        .with(new CollidableComponent(true))
                        .bbox(new HitBox(BoundingShape.box(50, 50)))
                        .zIndex(500)
                        .buildAndAttach();

                burnZone.addComponent(new BurnZoneComponent());
            }
        }
    }

    private class BurnZoneComponent extends Component {
        private double lifetime = 0.0;
        private double damageTimer = 0.0;

        @Override
        public void onUpdate(double tpf) {
            lifetime += tpf;
            damageTimer += tpf;

            if (lifetime >= BURN_ZONE_DURATION) {
                entity.removeFromWorld();
                return;
            }

            if (damageTimer >= BURN_ZONE_TICK_INTERVAL) {
                damageTimer = 0.0;
                FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY).stream()
                        .filter(e -> e.isActive() && entity.getBoundingBoxComponent().isCollidingWith(e.getBoundingBoxComponent()))
                        .forEach(e -> e.getComponent(EnemyComponent.class).damage(BURN_ZONE_DAMAGE, e.getCenter()));
            }
        }
    }
}