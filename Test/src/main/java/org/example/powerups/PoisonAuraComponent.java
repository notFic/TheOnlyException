package org.example.powerups;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.EnemyComponent;
import org.example.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class PoisonAuraComponent extends Component {

    private static final double RADIUS = 100; // Radius of the poison aura
    private static final double DAMAGE_PER_SECOND = 7;
    private static final double DAMAGE_INTERVAL = 0.5; // seconds between damage ticks

    private double damageTimer = 0;

    // Debugging Hitbox
    private Circle debugHitbox;
    private boolean isActive = true;

    public void activatePowerUp() {
        isActive = true;
        showHitbox();
    }

    public void deactivatePowerUp() {
        isActive = false;
        hideHitbox();
    }

    @Override
    public void onAdded() {
        showHitbox(); // Initialize hitbox position when component is added
    }

    @Override
    public void onUpdate(double tpf) {
        if (!isActive) return;

        Point2D playerCenterWorld = entity.getCenter();
        Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

        // Update the timer
        damageTimer += tpf;

        if (damageTimer >= DAMAGE_INTERVAL) {
            damageTimer = 0; // Reset timer after dealing damage

            List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY)
                    .stream()
                    .filter(Entity::isActive)
                    .collect(Collectors.toList());

            for (Entity enemy : enemies) {
                double distance = enemy.getPosition().distance(playerCenterWorld);

                if (distance <= RADIUS) {
                    EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
                    showPoisonAura(enemy.getCenter());
                    enemyComponent.damage(DAMAGE_PER_SECOND); // Deal damage based on interval
                }
            }
        }

        if (debugHitbox != null) {
            debugHitbox.setCenterX(playerCenterScreen.getX());
            debugHitbox.setCenterY(playerCenterScreen.getY());
        }
    }




    private void showPoisonAura(Point2D position) {
        // Show visual effects here (particles, etc.)
    }

    private void showHitbox() {
        if (debugHitbox == null) {
            Point2D playerCenterWorld = entity.getCenter();
            Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

            debugHitbox = new Circle(playerCenterScreen.getX(), playerCenterScreen.getY(), RADIUS);
            debugHitbox.setFill(Color.color(1, 0, 0, 0.1));
            debugHitbox.setStroke(Color.rgb(255, 0, 0, 0.3));
            debugHitbox.setStrokeWidth(2);
            FXGL.getGameScene().addUINode(debugHitbox);
        } else {
            Point2D playerCenterWorld = entity.getCenter();
            Point2D playerCenterScreen = playerCenterWorld.subtract(FXGL.getGameScene().getViewport().getOrigin());

            debugHitbox.setCenterX(playerCenterScreen.getX());
            debugHitbox.setCenterY(playerCenterScreen.getY());
        }
    }



    private void hideHitbox() {
        if (debugHitbox != null) {
            FXGL.getGameScene().removeUINode(debugHitbox);
            debugHitbox = null;
        }
    }
}