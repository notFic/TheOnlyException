package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.util.Duration;

public class PlayerComponent extends Component {
    private double speed = 1.5; // PLAYER SPEED

    private AnimatedTexture texture;
    private AnimationChannel animIdle;
    private AnimationChannel animWalk;

    private boolean isMoving = false;
    private Point2D previousPosition;

    public PlayerComponent() {
        animIdle = new AnimationChannel(FXGL.image("pixel_character_pale_blue_original.png"), 5,
                48, 48, Duration.seconds(1), 0, 4);
        animWalk = new AnimationChannel(FXGL.image("pixel_character_pale_blue_original.png"), 8,
                48, 48, Duration.seconds(1), 16, 23);

        texture = new AnimatedTexture(animIdle);
        texture.loop();
    }

    @Override
    public void onAdded() {
        entity.getViewComponent().addChild(texture);
        previousPosition = entity.getPosition();
    }

    @Override
    public void onUpdate(double tpf) {
        // Check if player has moved by comparing current position with previous position
        Point2D currentPosition = entity.getPosition();
        isMoving = !currentPosition.equals(previousPosition);

        // Update animation based on movement
        if (isMoving) {
            if (texture.getAnimationChannel() != animWalk) {
                texture.loopAnimationChannel(animWalk);
            }
        } else {
            if (texture.getAnimationChannel() != animIdle) {
                texture.loopAnimationChannel(animIdle);
            }
        }

        // Store current position for next frame comparison
        previousPosition = currentPosition;
    }

    public void moveLeft() {
        entity.translateX(-speed);
        boundPlayerInWorld();
    }

    public void moveRight() {
        entity.translateX(speed);
        boundPlayerInWorld();
    }

    public void moveUp() {
        entity.translateY(-speed);
        boundPlayerInWorld();
    }

    public void moveDown() {
        entity.translateY(speed);
        boundPlayerInWorld();
    }

    // KEEP FROM GOING OUT OF BOUNDS
    private void boundPlayerInWorld() {
        double worldWidth = FXGL.getAppWidth() * 2;
        double worldHeight = FXGL.getAppHeight() * 2;
        double playerSize = 40; // SIZE OF PLAYER RECTANGLE

        if (entity.getX() < 0) {
            entity.setX(0);
        } else if (entity.getX() > worldWidth - playerSize) {
            entity.setX(worldWidth - playerSize);
        }

        if (entity.getY() < 0) {
            entity.setY(0);
        } else if (entity.getY() > worldHeight - playerSize) {
            entity.setY(worldHeight - playerSize);
        }
    }

    public void shoot() {
        // GET MOUSE POS AND COVERT TO WORLD POSITION
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(
                mouseScreenPos.getX() + viewportX,
                mouseScreenPos.getY() + viewportY
        );

        // GET DIRECTION FROM PLAYER TO MOUSE
        Point2D direction = mouseWorldPos.subtract(entity.getCenter());

        Entity bullet = FXGL.spawn("bullet", entity.getCenter());
        bullet.getComponent(BulletComponent.class).setDirection(direction);
    }

    public void shootTripleBurst() {
        // GET MOUSE POS AND COVERT TO WORLD POSITION
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(
                mouseScreenPos.getX() + viewportX,
                mouseScreenPos.getY() + viewportY
        );

        // GET DIRECTION FROM PLAYER TO MOUSE
        Point2D direction = mouseWorldPos.subtract(entity.getCenter()).normalize();

        // SHOTGUN BANG BUSLOT KALAG
        spawnBulletWithAngle(direction, 0);         // Center bullet
        spawnBulletWithAngle(direction, -10);       // Left bullet (10 degrees left)
        spawnBulletWithAngle(direction, 10);        // Right bullet (10 degrees right)
    }

    // SPAWN BALA
    private void spawnBulletWithAngle(Point2D direction, double angleDegrees) {
        Point2D rotatedDirection = rotate(direction, angleDegrees);
        Entity bullet = FXGL.spawn("bullet", entity.getCenter());
        bullet.getComponent(BulletComponent.class).setDirection(rotatedDirection);
    }

    // ROTATE FOR BURST SHOT
    private Point2D rotate(Point2D vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        double newX = vector.getX() * cos - vector.getY() * sin;
        double newY = vector.getX() * sin + vector.getY() * cos;

        return new Point2D(newX, newY);
    }
}