package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.almasb.fxgl.time.TimerAction;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

public class LightningStrike extends Component {

    private Rectangle powerUpVisual;
    private Entity player;
    private final double MAGNET_RANGE = 150.0;
    private final double MOVE_SPEED = 2.5;
    private boolean isActive = false;

    private TimerAction strikeTimer; // interval between next strike

    @Override
    public void onAdded() {
        this.player = FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER)
                .stream()
                .findFirst()
                .orElse(null);

        if (entity.getViewComponent().getChildren().get(0) instanceof Rectangle) {
            powerUpVisual = (Rectangle) entity.getViewComponent().getChildren().get(0);
            powerUpVisual.setFill(Color.PURPLE); // Purple color for power-up
        }

        // Remove after 20 seconds if not collected
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

        // Check for collision with player
        if (entity.isColliding(player)) {
            activatePowerUp();
            entity.removeFromWorld();
            return;
        }

        // Magnet effect like drops
        Point2D playerCenter = player.getCenter();
        Point2D powerUpCenter = entity.getCenter();

        if (playerCenter != null && powerUpCenter != null &&
                powerUpCenter.distance(playerCenter) <= MAGNET_RANGE) {
            Point2D direction = playerCenter.subtract(powerUpCenter).normalize();
            entity.translate(direction.multiply(MOVE_SPEED * tpf * 60));
        }
    }

    private void activatePowerUp() {
        // Get all active enemies on screen
        List<Entity> enemies = FXGL.getGameWorld().getEntitiesByType(EntityType.ENEMY)
                .stream()
                .filter(Entity::isActive)
                .collect(Collectors.toList());

        // Damage 3 random enemies (or all if there are less than 3)
        int enemiesToDamage = Math.min(3, enemies.size());
        for (int i = 0; i < enemiesToDamage; i++) {
            int randomIndex = (int) (Math.random() * enemies.size());
            Entity enemy = enemies.get(randomIndex);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);

            showLightningStrike(enemy.getCenter());
            enemyComponent.damage(50);

            // Remove from list to avoid damaging same enemy twice
            enemies.remove(randomIndex);
        }

    }

    private void showLightningStrike(Point2D position) {
        Image image = FXGL.image("lightning_strike.png");
        int frameWidth = (int) image.getWidth() / 5;
        int frameHeight = (int) image.getHeight();

        AnimationChannel channel = new AnimationChannel(image, 5, frameWidth, frameHeight, Duration.seconds(0.4), 0, 4);
        AnimatedTexture animatedTexture = new AnimatedTexture(channel);
        animatedTexture.play();

        double strikeX = position.getX() - (frameWidth / 2.0); // center horizontally
        double strikeY = 0; // from top of screen

        Entity lightning = FXGL.entityBuilder()
                .at(strikeX, strikeY)
                .view(animatedTexture)
                .zIndex(1000)
                .buildAndAttach();

        double strikeHeight = position.getY();

        lightning.setScaleY(strikeHeight / frameHeight);

        Node root = FXGL.getGameScene().getRoot();

        TranslateTransition shake = new TranslateTransition(Duration.seconds(0.1), root);
        shake.setFromY(-3);
        shake.setToY(3);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> root.setTranslateY(0)); // reset just in case
        shake.play();


        FXGL.getGameTimer().runOnceAfter(() -> {
            lightning.removeFromWorld();
        }, Duration.seconds(0.4));
    }

    public boolean isActivated() {
        return isActive;
    }
}