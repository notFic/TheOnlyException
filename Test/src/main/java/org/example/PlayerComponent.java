package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;

public class PlayerComponent extends Component {

    public void moveLeft() {
        entity.translateX(-2);
    }

    public void moveRight() {
        entity.translateX(2);
    }

    public void moveUp() {
        entity.translateY(-2);
    }

    public void moveDown() {
        entity.translateY(2);
    }

    public void shoot() {
        Point2D mousePosition = FXGL.getInput().getMousePositionWorld();
        Point2D direction = mousePosition.subtract(entity.getCenter());

        Entity bullet = FXGL.spawn("bullet", entity.getCenter());
        bullet.getComponent(BulletComponent.class).setDirection(direction);
    }

    public void shootTripleBurst() {
        // Get the mouse position
        Point2D mousePosition = FXGL.getInput().getMousePositionWorld();
        Point2D direction = mousePosition.subtract(entity.getCenter()).normalize();

        // Spawn 3 bullets with slight angle variations
        spawnBulletWithAngle(direction, 0);         // Center bullet
        spawnBulletWithAngle(direction, -10);       // Left bullet (10 degrees left)
        spawnBulletWithAngle(direction, 10);        // Right bullet (10 degrees right)
    }

    private void spawnBulletWithAngle(Point2D direction, double angleDegrees) {
        Point2D rotatedDirection = rotate(direction, angleDegrees);
        Entity bullet = FXGL.spawn("bullet", entity.getCenter());
        bullet.getComponent(BulletComponent.class).setDirection(rotatedDirection);
    }

    // Helper method to rotate a vector by an angle (in degrees)
    private Point2D rotate(Point2D vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        double newX = vector.getX() * cos - vector.getY() * sin;
        double newY = vector.getX() * sin + vector.getY() * cos;

        return new Point2D(newX, newY);
    }
}
