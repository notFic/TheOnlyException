package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.core.EntityType;

public class FoodComponent extends Component {
    private final Color[] RED_SHADES = {
            Color.RED,
            Color.DARKRED,
            Color.CRIMSON,
            Color.FIREBRICK,
            Color.INDIANRED
    };

    private int currentColorIndex = 0;
    private Rectangle foodVisual;
    private Entity player;
    private final double MAGNET_RANGE = 150.0;
    private final double MOVE_SPEED = 2.5;
    private final int HEAL_AMOUNT = 20;

    @Override
    public void onAdded() {
        this.player = FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER)
                .stream()
                .findFirst()
                .orElse(null);

        if (entity.getViewComponent().getChildren().get(0) instanceof Rectangle) {
            foodVisual = (Rectangle) entity.getViewComponent().getChildren().get(0);

            FXGL.getGameTimer().runAtInterval(() -> {
                if (entity != null && entity.isActive()) {
                    currentColorIndex = (currentColorIndex + 1) % RED_SHADES.length;
                    foodVisual.setFill(RED_SHADES[currentColorIndex]);
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
            playerComponent.damage(-HEAL_AMOUNT); // Negative damage means healing
            entity.removeFromWorld();
            return;
        }

        Point2D playerCenter = player.getCenter();
        Point2D foodCenter = entity.getCenter();

        if (playerCenter != null && foodCenter != null &&
                foodCenter.distance(playerCenter) <= MAGNET_RANGE) {
            Point2D direction = playerCenter.subtract(foodCenter).normalize();
            entity.translate(direction.multiply(MOVE_SPEED * tpf * 60));
        }
    }
} 