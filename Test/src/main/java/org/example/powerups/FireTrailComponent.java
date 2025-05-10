package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import org.example.components.EnemyComponent;
import org.example.core.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class FireTrailComponent extends Component {

    private static final double TRAIL_SPAWN_DISTANCE = 20; // Distance between trail segments
    private static final double TRAIL_LIFETIME = 3.0; // Duration each segment lasts
    private static final double TRAIL_SIZE = 40; // Size of each trail segment (hitbox size)

    private static final double TRAIL_OFFSET_X = 25.0; // X offset for trail segments
    private static final double TRAIL_OFFSET_Y = 30.0; // Y offset for trail segments

    private static final double DIRECT_DAMAGE = 4.0; // Damage when in trail
    private static final double DIRECT_DAMAGE_INTERVAL = 0.5; // Damage every 0.5 seconds
    private static final double BURN_DURATION = 3.0; // Duration of burn effect
    private static final double BURN_TICK_INTERVAL = 1.0; // Burn damage every 1 second
    private static final double BURN_TICK_DAMAGE = 2.0; // Damage per burn tick

    private boolean isActive = false;
    private boolean isGamePaused = false; // Track game pause state
    private final List<Entity> trailSegments = new ArrayList<>();
    private Point2D lastTrailSpawnPosition;
    private double directDamageTimer = 0.0; // Timer for direct damage
    private double burnTickTimer = 0.0; // Timer for burn damage ticks

    // Tracks enemy states: inHitbox (true/false), burnTimeRemaining (seconds)
    private final Map<Entity, EnemyState> enemyStates = new HashMap<>();

    private static class EnemyState {
        boolean inHitbox; // Is enemy currently in a trail hitbox?
        double burnTimeRemaining; // Remaining burn duration (0 if not burning)

        EnemyState(boolean inHitbox, double burnTimeRemaining) {
            this.inHitbox = inHitbox;
            this.burnTimeRemaining = burnTimeRemaining;
        }
    }

    @Override
    public void onAdded() {
        activatePowerUp(); // For testing
    }

    public void activatePowerUp() {
        if (isActive) return;
        isActive = true;
        lastTrailSpawnPosition = entity.getCenter();
    }

    public void deactivatePowerUp() {
        if (!isActive) return;
        isActive = false;
        clearTrail();
        enemyStates.clear();
    }

    public void pausePowerUp() {
        if (!isActive || isGamePaused) return;
        isGamePaused = true;
    }

    public void resumePowerUp() {
        if (!isActive || !isGamePaused) return;
        isGamePaused = false;
        lastTrailSpawnPosition = entity.getCenter();
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive || isGamePaused) return;

        // Spawn trail segments
        Point2D currentPosition = entity.getCenter();
        double distanceMoved = currentPosition.distance(lastTrailSpawnPosition);
        if (distanceMoved >= TRAIL_SPAWN_DISTANCE) {
            spawnTrailSegment();
            lastTrailSpawnPosition = currentPosition;
        }

        // Update timers
        directDamageTimer += tpf;
        burnTickTimer += tpf;

        // Update enemy states and apply damage
        updateEnemyStates();
        applyDirectDamage();
        applyBurnDamage();
    }

    private void spawnTrailSegment() {
        Point2D playerCenter = entity.getCenter();

        // Create the animated texture for smolder.png
        AnimationChannel smolderChannel = new AnimationChannel(
                FXGL.image("smolder.png"),
                4, // Number of frames
                16, // Frame width (corrected to 16)
                16, // Frame height (corrected to 16)
                Duration.seconds(0.8), // Total animation duration (0.2s per frame)
                0, // Start frame
                3  // End frame
        );

        AnimatedTexture trailVisual = new AnimatedTexture(smolderChannel);
        trailVisual.loop(); // Start looping animation immediately

        // Scale the sprite to match TRAIL_SIZE (40x40 hitbox)
        double scaleFactor = TRAIL_SIZE / 16.0; // Scale 16x16 sprite to 40x40
        trailVisual.setScaleX(scaleFactor);
        trailVisual.setScaleY(scaleFactor);

        // Apply the X and Y offset to the trail segment position
        Point2D adjustedPosition = playerCenter
                .subtract(TRAIL_SIZE / 2, TRAIL_SIZE / 2) // Center the hitbox
                .add(TRAIL_OFFSET_X, TRAIL_OFFSET_Y); // Apply the offset

        // Adjust the visual position to center the scaled sprite on the hitbox
        double scaledSpriteSize = 16 * scaleFactor; // Should be 40
        trailVisual.setTranslateX(-(scaledSpriteSize / 2));
        trailVisual.setTranslateY(-(scaledSpriteSize / 2));

        Entity trail = FXGL.entityBuilder()
                .type(EntityType.FIRETRAIL)
                .at(adjustedPosition)
                .view(trailVisual)
                .with(new CollidableComponent(true))
                .bbox(new HitBox(BoundingShape.box(TRAIL_SIZE, TRAIL_SIZE)))
                .zIndex(-1)
                .buildAndAttach();

        trailSegments.add(trail);

        System.out.println("FireTrail: Spawned trail segment at " + adjustedPosition +
                " with offset (" + TRAIL_OFFSET_X + ", " + TRAIL_OFFSET_Y + ")" +
                ", Scale factor: " + scaleFactor);

        // Use real-time timer for cleanup
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (trail.isActive()) {
                    FXGL.runOnce(() -> {
                        trailVisual.stop(); // Stop animation on cleanup
                        trail.removeFromWorld();
                        trailSegments.remove(trail);
                    }, Duration.ZERO);
                }
            }
        }, (long) (TRAIL_LIFETIME * 1000));
    }

    private void updateEnemyStates() {
        // Get all active enemies
        List<Entity> activeEnemies = FXGL.getGameWorld()
                .getEntitiesByType(EntityType.ENEMY)
                .stream()
                .filter(Entity::isActive)
                .collect(Collectors.toList());

        // Update hitbox status for each enemy
        for (Entity enemy : activeEnemies) {
            boolean inHitbox = trailSegments.stream()
                    .anyMatch(t -> t.isActive() &&
                            enemy.getBoundingBoxComponent().isCollidingWith(t.getBoundingBoxComponent()));

            EnemyState state = enemyStates.computeIfAbsent(enemy, k -> new EnemyState(false, 0.0));

            if (inHitbox) {
                state.inHitbox = true;
                // Reset burn timer while in hitbox
                state.burnTimeRemaining = 0.0;
            } else {
                // Enemy is not in hitbox
                if (state.inHitbox) {
                    // Enemy just exited hitbox, start burn effect
                    state.burnTimeRemaining = BURN_DURATION;
                    System.out.println("FireTrail: Enemy exited hitbox, starting burn effect for " + BURN_DURATION + " seconds");
                }
                state.inHitbox = false;
            }
        }

        // Remove inactive enemies from tracking
        enemyStates.entrySet().removeIf(entry -> !entry.getKey().isActive());
    }

    private void applyDirectDamage() {
        if (directDamageTimer < DIRECT_DAMAGE_INTERVAL) return;

        directDamageTimer = 0.0; // Reset timer

        for (Map.Entry<Entity, EnemyState> entry : enemyStates.entrySet()) {
            Entity enemy = entry.getKey();
            EnemyState state = entry.getValue();

            if (state.inHitbox) {
                enemy.getComponentOptional(EnemyComponent.class)
                        .ifPresent(ec -> {
                            ec.damage(DIRECT_DAMAGE, enemy.getCenter());
                            System.out.println("FireTrail: Dealt " + DIRECT_DAMAGE + " direct damage to enemy");
                        });
            }
        }
    }

    private void applyBurnDamage() {
        if (burnTickTimer < BURN_TICK_INTERVAL) return;

        burnTickTimer = 0.0; // Reset timer

        for (Map.Entry<Entity, EnemyState> entry : enemyStates.entrySet()) {
            Entity enemy = entry.getKey();
            EnemyState state = entry.getValue();

            if (state.burnTimeRemaining > 0 && !state.inHitbox) {
                enemy.getComponentOptional(EnemyComponent.class)
                        .ifPresent(ec -> {
                            ec.damage(BURN_TICK_DAMAGE, enemy.getCenter());
                            System.out.println("FireTrail: Dealt " + BURN_TICK_DAMAGE + " burn damage to enemy (remaining burn time: " + state.burnTimeRemaining + ")");
                        });
                state.burnTimeRemaining -= BURN_TICK_INTERVAL;
                if (state.burnTimeRemaining <= 0) {
                    state.burnTimeRemaining = 0;
                    System.out.println("FireTrail: Burn effect ended for enemy");
                }
            }
        }
    }

    private void clearTrail() {
        new ArrayList<>(trailSegments).forEach(Entity::removeFromWorld);
        trailSegments.clear();
    }
}