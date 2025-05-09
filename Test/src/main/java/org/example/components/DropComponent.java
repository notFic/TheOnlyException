package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.core.EntityType;

public class DropComponent extends Component {

    private final Color[] YELLOW_SHADES = {
            Color.GOLD,
            Color.YELLOW,
            Color.LIGHTYELLOW,
            Color.KHAKI,
            Color.GOLDENROD
    };

    private int currentColorIndex = 0;
    private Rectangle dropVisual;
    private Entity player;
    private final double MAGNET_RANGE = 150.0;
    private final double MOVE_SPEED = 2.5;
    private final int EXP_VALUE = 50;

    @Override
    public void onAdded() {
        this.player = FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER)
                .stream()
                .findFirst()
                .orElse(null);

        if (entity.getViewComponent().getChildren().get(0) instanceof Rectangle) {
            dropVisual = (Rectangle) entity.getViewComponent().getChildren().get(0);

            FXGL.getGameTimer().runAtInterval(() -> {
                if (entity != null && entity.isActive()) {
                    currentColorIndex = (currentColorIndex + 1) % YELLOW_SHADES.length;
                    dropVisual.setFill(YELLOW_SHADES[currentColorIndex]);
                }
            }, Duration.seconds(0.2));
        }

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
    }
}