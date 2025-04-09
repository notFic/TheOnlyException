package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class EnemyComponent extends Component {
    private Entity player;
    private double speed;
    private double variableSpeedFactor = 0.95 + Math.random() * 0.3; // RANDOM SPEED (0.95-1.25)
    private int health;

    public EnemyComponent(Entity player, double baseSpeed, int baseHealth) {
        this.player = player;
        this.speed = baseSpeed * variableSpeedFactor;
        this.health = baseHealth;
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

    public void damage(double dmg) {
        health -= dmg;

        // FLASHES WHITE WHEN HIT
        var originalView = entity.getViewComponent().getChildren().getFirst();
        var originalEffect = originalView.getEffect();
        originalView.setEffect(new javafx.scene.effect.ColorAdjust(0, -1, 1, 0)); // makes it look white
        FXGL.getGameTimer().runOnceAfter(() -> {
            originalView.setEffect(originalEffect);
        }, javafx.util.Duration.millis(150));

        showDamageText(dmg);

        if (health <= 0) {
            entity.removeFromWorld();
        }
    }


    private void showDamageText(double dmg) {
        var damageText = FXGL.getUIFactoryService().newText(String.valueOf((int) dmg), Color.WHITE, 18);
        var textEntity = FXGL.entityBuilder()
                .at(entity.getPosition().subtract(0, 30))
                .view(damageText)
                .buildAndAttach();

        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(1))
                .translate(textEntity)
                .from(textEntity.getPosition())
                .to(textEntity.getPosition().subtract(0, 30))  // Move text upwards
                .build()
                .start();

        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(1))
                .fadeOut(textEntity)
                .build()
                .start();

        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), javafx.util.Duration.seconds(1));
    }


}