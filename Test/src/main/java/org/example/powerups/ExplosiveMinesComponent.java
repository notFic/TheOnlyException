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
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.example.components.EnemyComponent;
import org.example.core.EntityType;
import org.example.components.PlayerComponent;
import org.example.core.SoundManager;

public class ExplosiveMinesComponent extends Component {
    private static final double SPAWN_INTERVAL = 2.0; // Seconds between mine spawns
    private static final double MINE_LIFETIME = 5.0; // Seconds before explosion
    private static final double EXPLOSION_RADIUS = 100.0; // Base explosion radius
    private static final double EXPLOSION_RADIUS_UPGRADED = 150.0; // With Heap Overflow
    private static final double EXPLOSION_DAMAGE = 40.0; // Damage dealt by explosion
    private static final double EXPLOSION_VISUAL_DURATION = 0.5; // Seconds for visual effect
//    private static final boolean DEBUG_VISUALIZER = false; // Toggle debug radius visualizer

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
        SoundManager.getInstance().playSound("explosion"); // Play gunshot sound

        // Load datawipemine.png and create animated texture
        AnimationChannel mineChannel = new AnimationChannel(
                FXGL.image("datawipemine.png"),
                8, // Number of frames
                24, // Frame width
                24, // Frame height
                Duration.seconds(0.8), // Total animation duration (0.1s per frame)
                0, // Start frame
                7  // End frame
        );
        AnimatedTexture mineTexture = new AnimatedTexture(mineChannel);
        mineTexture.loop(); // Start and loop the animation

        Entity mine = FXGL.entityBuilder()
                .type(EntityType.MINE)
                .at(playerCenter.subtract(24, 24)) // Center 48x48 sprite
                .view(mineTexture)
                .with(new CollidableComponent(true))
                .bbox(new HitBox(BoundingShape.box(48, 48))) // Match scaled sprite
                .scale(2.0, 2.0) // Scale 24x24 sprite to 48x48
                .zIndex(500)
                .buildAndAttach();

        mine.addComponent(new MineBehaviorComponent());
    }

    void cameraShake(){
        Node root = FXGL.getGameScene().getRoot();

        TranslateTransition shake = new TranslateTransition(Duration.seconds(0.1), root);
        shake.setFromY(-3);
        shake.setToY(3);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> root.setTranslateY(0)); // reset just in case
        shake.play();
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
            double scaleFactor = (2 * radius) / 64.0; // Scale 64x64 sprite to diameter 2*radius (200x200 or 300x300)

            // Visual explosion effect (animated sprite)
            AnimationChannel explosionChannel = new AnimationChannel(
                    FXGL.image("datawipeexplosion.png"),
                    10, // Number of frames
                    64, // Frame width
                    64, // Frame height
                    Duration.seconds(EXPLOSION_VISUAL_DURATION), // 0.5 seconds total
                    0, // Start frame
                    9  // End frame
            );
            AnimatedTexture explosionTexture = new AnimatedTexture(explosionChannel);
            explosionTexture.play(); // Play once

            Entity explosionEntity = FXGL.entityBuilder()
                    .at(center.subtract(radius, radius)) // Center scaled sprite (200x200 or 300x300)
                    .view(explosionTexture)
                    .scale(scaleFactor, scaleFactor) // Scale to diameter 2*radius
                    .zIndex(1000)
                    .buildAndAttach();
            FXGL.getGameTimer().runOnceAfter(explosionEntity::removeFromWorld, Duration.seconds(EXPLOSION_VISUAL_DURATION));

//            // Debug radius visualizer (optional)
//            if (DEBUG_VISUALIZER) {
//                Circle debugCircle = new Circle(radius, Color.TRANSPARENT);
//                debugCircle.setStroke(Color.RED);
//                debugCircle.setStrokeWidth(2.0);
//                Entity debugEntity = FXGL.entityBuilder()
//                        .at(center.subtract(radius, radius)) // Center circle
//                        .view(debugCircle)
//                        .zIndex(1001)
//                        .buildAndAttach();
//                FXGL.getGameTimer().runOnceAfter(debugEntity::removeFromWorld, Duration.seconds(EXPLOSION_VISUAL_DURATION));
//            }

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
            cameraShake();
        }
    }
}