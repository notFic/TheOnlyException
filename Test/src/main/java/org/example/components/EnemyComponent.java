package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.example.core.EntityType;

import java.util.List;

public class EnemyComponent extends Component {
    private Entity player;
    private double speed;
    private double variableSpeedFactor = 0.95 + Math.random() * 0.3; // RANDOM SPEED (0.95-1.25)
    private int health;
    private int damage;

    private long lastDamageTime = 0;
    private final long damageCooldown = 500_000_000; // 0.5 SEC INTERNAL COOLDOWN

    private AnimatedTexture texture;
    private AnimationChannel animWalkLeft;
    private AnimationChannel animWalkRight;
    private String type;

    // Constants for collision avoidance
    private final double SEPARATION_DISTANCE = 35.0; // Minimum distance to maintain between enemies
    private final double SEPARATION_FORCE = 0.7; // Strength of the separation force

    // Animation smoothing variables
    private boolean isMovingRight = true;
    private double animationChangeThreshold = 0.2; // Minimum X velocity needed to change direction
    private double totalDirectionX = 0;
    private static final double DIRECTION_MEMORY_FACTOR = 0.8; // How much to remember previous directions

    public EnemyComponent(Entity player, double baseSpeed, int baseHealth, int damage, String type) {
        this.player = player;
        this.speed = baseSpeed * variableSpeedFactor;
        this.health = baseHealth;
        this.damage = damage;
        this.type = type;

        if (type.equals("maggot")) {
            animWalkLeft = new AnimationChannel(FXGL.image("MaggotWalk-scaled.png"), 4,
                    64, 64, Duration.seconds(0.8), 4, 7);
            animWalkRight = new AnimationChannel(FXGL.image("MaggotWalk-scaled.png"), 4,
                    64, 64, Duration.seconds(0.8), 8, 11);

            texture = new AnimatedTexture(animWalkRight);
            texture.loop();
        }
        if (type.equals("beetle")) {
            animWalkLeft = new AnimationChannel(FXGL.image("BeetleMove-scaled.png"), 4,
                    64, 64, Duration.seconds(0.4), 4, 7);
            animWalkRight = new AnimationChannel(FXGL.image("BeetleMove-scaled.png"), 4,
                    64, 64, Duration.seconds(0.4), 8, 11);

            texture = new AnimatedTexture(animWalkRight);
            texture.loop();
        }
        if (type.equals("mantis")) {
            animWalkRight = new AnimationChannel(FXGL.image("MantisMove-scaled.png"), 4,
                    64, 64, Duration.seconds(0.8), 4, 7);
            animWalkLeft = new AnimationChannel(FXGL.image("MantisMove-scaled.png"), 4,
                    64, 64, Duration.seconds(0.8), 8, 11);

            texture = new AnimatedTexture(animWalkRight);
            texture.loop();
        }
    }

    @Override
    public void onAdded() {
        if (type.equals("maggot")) {
            entity.getViewComponent().addChild(texture);

            // ADJUST TO ALIGN WITH HITBOX
            texture.setTranslateX(-10);
            texture.setTranslateY(-40);
        }
        if (type.equals("beetle")) {
            entity.getViewComponent().addChild(texture);

            // ADJUST TO ALIGN WITH HITBOX
            texture.setTranslateX(-10);
            texture.setTranslateY(-25);
        }
        if (type.equals("mantis")) {
            entity.getViewComponent().addChild(texture);

            // ADJUST TO ALIGN WITH HITBOX
            texture.setTranslateX(-10);
            texture.setTranslateY(-13);
        }
    }

    @Override
    public void onUpdate(double tpf) {
        if (player == null || !player.isActive()) {
            return;
        }

        // GET PLAYER DIRECTION AND MOVE TOWARDS IT
        Point2D playerPosition = player.getPosition();
        Point2D enemyPosition = entity.getPosition();
        Point2D direction = playerPosition.subtract(enemyPosition).normalize().multiply(speed * tpf * 60);

        // Apply separation force to avoid other enemies
        Point2D separationForce = calculateSeparationForce(tpf);
        direction = direction.add(separationForce);

        // Update animation direction with smoothing to prevent jittering
        updateAnimation(direction);

        entity.translate(direction);

        // GET VIEWPORT BOUNDS
        double viewMinX = FXGL.getGameScene().getViewport().getX();
        double viewMinY = FXGL.getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + FXGL.getAppWidth();
        double viewMaxY = viewMinY + FXGL.getAppHeight();

        // REMOVE ENEMY IF OUTSIDE VIEWPORT
        double margin = 500; // EXTRA MARGIN OUTSIDE VIEWPORT
        if (entity.getX() < viewMinX - margin || entity.getX() > viewMaxX + margin ||
                entity.getY() < viewMinY - margin || entity.getY() > viewMaxY + margin) {

            double viewCenterX = viewMinX + FXGL.getAppWidth() / 2;
            double viewCenterY = viewMinY + FXGL.getAppHeight() / 2;

            double dx = entity.getX() - viewCenterX;
            double dy = entity.getY() - viewCenterY;
            Point2D toCenter = new Point2D(dx, dy).normalize();
            Point2D normalizedDir = direction.normalize();

            if (toCenter.dotProduct(normalizedDir) > 0.7) {
                entity.removeFromWorld();
            }
        }
    }

    /**
     * Update animation with smoothing to prevent rapid changes
     * when small forces affect movement direction
     */
    private void updateAnimation(Point2D direction) {
        if (type.equals("maggot") || type.equals("beetle") || type.equals("mantis")) {
            // Use running average to smooth direction changes
            totalDirectionX = (totalDirectionX * DIRECTION_MEMORY_FACTOR) + (direction.getX() * (1 - DIRECTION_MEMORY_FACTOR));

            // Only change animation if we exceed the threshold in either direction
            if (totalDirectionX > animationChangeThreshold && !isMovingRight) {
                isMovingRight = true;
                texture.loopAnimationChannel(animWalkRight);
            } else if (totalDirectionX < -animationChangeThreshold && isMovingRight) {
                isMovingRight = false;
                texture.loopAnimationChannel(animWalkLeft);
            }
        }
    }

    /**
     * Calculate a separation force to avoid crowding with other enemies
     */
    private Point2D calculateSeparationForce(double tpf) {
        Point2D currentPosition = entity.getPosition();
        Point2D separationForce = new Point2D(0, 0);
        int neighborCount = 0;

        // Get all enemies in the game world
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY);

        for (Entity otherEnemy : enemies) {
            // Skip self
            if (otherEnemy == entity) {
                continue;
            }

            // Calculate distance to the other enemy
            Point2D otherPosition = otherEnemy.getPosition();
            double distance = currentPosition.distance(otherPosition);

            // If the other enemy is too close, add a separation force
            if (distance < SEPARATION_DISTANCE && distance > 0) {
                // Calculate direction away from the other enemy
                Point2D awayDirection = currentPosition.subtract(otherPosition).normalize();

                // The separation force is stronger when enemies are closer
                double forceMagnitude = SEPARATION_FORCE * (SEPARATION_DISTANCE - distance) / SEPARATION_DISTANCE;

                // Add the weighted separation force
                separationForce = separationForce.add(awayDirection.multiply(forceMagnitude));
                neighborCount++;
            }
        }

        // If there are neighbors, normalize the force
        if (neighborCount > 0) {
            separationForce = separationForce.normalize().multiply(SEPARATION_FORCE * speed * tpf * 60);
        }

        return separationForce;
    }

    public void damage(double dmg, Point2D hitPosition) {
        health -= dmg;

        // FLASHES WHITE WHEN HIT
        if (type.equals("maggot") || type.equals("beetle") || type.equals("mantis")) {
            texture.setEffect(new javafx.scene.effect.ColorAdjust(0, -1, 1, 0)); // WHITE
            FXGL.getGameTimer().runOnceAfter(() -> {
                texture.setEffect(null);
            }, javafx.util.Duration.millis(25));
        } else {
            var originalView = entity.getViewComponent().getChildren().get(0);
            var originalEffect = originalView.getEffect();
            originalView.setEffect(new javafx.scene.effect.ColorAdjust(0, -1, 1, 0)); // WHITE
            FXGL.getGameTimer().runOnceAfter(() -> {
                originalView.setEffect(originalEffect);
            }, javafx.util.Duration.millis(25));
        }

        showDamageText(dmg, hitPosition);

        if (health <= 0) {
            if(Math.random() < 0.5){
                FXGL.spawn("drop", entity.getCenter());
            }

            entity.removeFromWorld();
        }
    }

    private void showDamageText(double dmg, Point2D hitPosition) {
        // Create the damage text with original styling
        var damageText = FXGL.getUIFactoryService().newText(String.valueOf((int) dmg), Color.WHITE, 22);
        
        // Add directly to game world at the hit position
        var textEntity = FXGL.entityBuilder()
                .at(hitPosition)
                .view(damageText)
                .zIndex(100)
                .buildAndAttach();
        
        // Determine jump direction based on enemy position relative to player
        boolean jumpRight = true; // Default to right
        
        if (player != null) {
            double enemyX = entity.getX();
            double playerX = player.getX();
            
            // If enemy is to the left of player, jump left
            // If enemy is to the right of player or at same position, jump right
            jumpRight = enemyX >= playerX;
        }
        
        // Distance and height for the jump
        int xDistance = 30;
        int yPeak = 25;
        
        // Set the direction based on enemy position
        if (!jumpRight) {
            xDistance = -xDistance;
        }
        
        // Create a path for the arc movement
        javafx.scene.shape.Path path = new javafx.scene.shape.Path();
        path.getElements().add(new javafx.scene.shape.MoveTo(0, 0));
        path.getElements().add(new javafx.scene.shape.QuadCurveTo(
                xDistance / 2.0, -yPeak,  // Control point
                xDistance, 0             // End point
        ));
        
        // Create a compound animation that combines path and fade
        javafx.animation.PathTransition pathTransition = new javafx.animation.PathTransition(
                javafx.util.Duration.seconds(0.6), path, damageText);
        pathTransition.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        // Create the fade transition
        javafx.animation.FadeTransition fadeTransition = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(0.25), damageText);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        
        // Create a timeline for managing the timing of both animations
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, e -> pathTransition.play()),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.4), e -> fadeTransition.play())
        );
        
        // Remove entity when animations are done
        fadeTransition.setOnFinished(e -> textEntity.removeFromWorld());
        
        // Start the timeline
        timeline.play();
    }

    public int getDamage() {
        return damage;
    }

    public int getHealth(){
        return health;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public void setLastDamageTime(long time) {
        lastDamageTime = time;
    }
}