package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.effect.Glow;
import javafx.util.Duration;
import org.example.core.EntityType;
import org.example.core.SoundManager;

public class DropComponent extends Component {

    private Entity player;
    private final double MAGNET_RANGE = 70.0;
    private final double MOVE_SPEED = 6.5;
    private final int EXP_VALUE = 50;
    private final double GLOW_LEVEL = 0.6; // Base glow intensity (0.0 to 1.0)
    private double elapsedTime = 0.0; // Custom time counter for pulsing

    private Glow glow;

    @Override
    public void onAdded() {
        this.player = FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER)
                .stream()
                .findFirst()
                .orElse(null);

        // Load the memorychip.png sprite
        var memoryChipSprite = FXGL.texture("memorychip.png");

        // Apply a glow effect to the sprite
        glow = new Glow(GLOW_LEVEL);
        memoryChipSprite.setEffect(glow);

        // Add the sprite directly as the entity's view
        entity.getViewComponent().addChild(memoryChipSprite);

        // Despawn after 20 seconds
        FXGL.getGameTimer().runOnceAfter(() -> {
            if (entity != null && entity.isActive()) {
                entity.removeFromWorld();
            }
        }, Duration.seconds(20));
    }

    @Override
    public void onUpdate(double tpf) {
        if (player == null || !player.isActive() || entity == null || !entity.isActive()) {
            return;
        }

        if (entity.isColliding(player)) {
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            playerComponent.addExp(EXP_VALUE);
            SoundManager.getInstance().playSound("pickup"); // Play hit sound
            entity.removeFromWorld();
            return;
        }

        Point2D playerCenter = player.getCenter();
        Point2D dropCenter = entity.getCenter();

        if (playerCenter != null && dropCenter != null &&
                dropCenter.distance(playerCenter) <= MAGNET_RANGE) {
            Point2D direction = playerCenter.subtract(dropCenter).normalize();
            entity.translate(direction.multiply(MOVE_SPEED * tpf * 60));
        }

        // Update elapsed time and animate glow
        elapsedTime += tpf;
        double pulse = 0.4 + 0.2 * Math.sin(elapsedTime * 5);
        glow.setLevel(pulse);
    }
}