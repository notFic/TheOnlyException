package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

public class EnemyComponent extends Component {
    private Entity player;
    private double speed;
    private double variableSpeedFactor = 0.95 + Math.random() * 0.3; // Random speed variation (0.95-1.25)

    public EnemyComponent(Entity player, double baseSpeed) {
        this.player = player;
        // Apply the random speed variation
        this.speed = baseSpeed * variableSpeedFactor;
    }

    @Override
    public void onUpdate(double tpf) {
        if (player == null || !player.isActive()) {
            return;
        }

        // Get direction to player
        Point2D playerPosition = player.getPosition();
        Point2D enemyPosition = entity.getPosition();
        Point2D direction = playerPosition.subtract(enemyPosition).normalize().multiply(speed * tpf * 60);

        // Move toward player
        entity.translate(direction);

        // Remove the enemy if it goes way outside the screen (cleanup)
        double margin = 200; // Extra margin beyond screen
        if (entity.getX() < -margin || entity.getX() > FXGL.getAppWidth() + margin ||
                entity.getY() < -margin || entity.getY() > FXGL.getAppHeight() + margin) {
            // Only remove if we're moving away from the screen (prevents immediate despawn)
            double dx = entity.getX() - FXGL.getAppWidth()/2;
            double dy = entity.getY() - FXGL.getAppHeight()/2;
            Point2D toCenter = new Point2D(dx, dy).normalize();
            Point2D normalizedDir = direction.normalize();

            // If dot product is positive, we're moving away from center
            if (toCenter.dotProduct(normalizedDir) > 0.7) {
                entity.removeFromWorld();
            }
        }
    }
}