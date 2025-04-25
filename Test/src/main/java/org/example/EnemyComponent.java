package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.util.Duration;

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

        // UPDATE MOVEMENT BASED ON DIRECTION
        if (type.equals("maggot") || type.equals("beetle") || type.equals("mantis")) {
            if (direction.getX() > 0) {
                // MOVING RIGHT
                if (texture.getAnimationChannel() != animWalkRight) {
                    texture.loopAnimationChannel(animWalkRight);
                }
            } else if (direction.getX() < 0) {
                // MOVING LEFT
                if (texture.getAnimationChannel() != animWalkLeft) {
                    texture.loopAnimationChannel(animWalkLeft);
                }
            }
        }


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

        showDamageText(dmg);

        if (health <= 0) {
            if(Math.random() < 0.5){
                FXGL.spawn("drop", entity.getCenter());
            }
            if(Math.random() < 0.3){
                FXGL.spawn("powerUp", entity.getCenter());
            }

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
                .to(textEntity.getPosition().subtract(0, 30))  // MOVE TEXT UPWARDS | STILL NEED FIX
                .build()
                .start();

        FXGL.animationBuilder()
                .duration(javafx.util.Duration.seconds(1))
                .fadeOut(textEntity)
                .build()
                .start();

        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), javafx.util.Duration.seconds(1));
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