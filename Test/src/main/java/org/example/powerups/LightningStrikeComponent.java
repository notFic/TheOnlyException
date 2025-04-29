package org.example.powerups;

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
import javafx.util.Duration;
import org.example.EnemyComponent;
import org.example.EntityType;

import java.util.List;
import java.util.stream.Collectors;

public class LightningStrikeComponent extends Component {

    // TODO add a method that will activate this method below every 10 seconds once ma toggle ang iyahang set keybind
    // (KEYBIND IS ONLY FOR DEBUGGING PURPOSES ONLY. we want to pass on this implementation when the player wants
    // to chose this specific powerup. Once implemented correctly, we can apply the same logic for other powerups in the future)

    // ACHIEVED ^^

    public void activatePowerUp() {
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
            enemyComponent.damage(50, enemy.getCenter());

            // Remove from list to avoid damaging same enemy twice
            enemies.remove(randomIndex);
        }

    }

    private void showLightningStrike(Point2D position) {
        Image image = FXGL.image("lightning_strike.png");
        int frameWidth = (int) image.getWidth() / 5; // 5 frames
        int frameHeight = (int) image.getHeight();

        AnimationChannel channel = new AnimationChannel(image, 5, frameWidth, frameHeight, Duration.seconds(0.4), 0, 4);
        AnimatedTexture animatedTexture = new AnimatedTexture(channel);
        animatedTexture.play(); // plays the loaded image

        double strikeX = position.getX() - (frameWidth / 2.0); // center horizontally
        double strikeY = 0; // from top of screen

        Entity lightning = FXGL.entityBuilder()
                .at(strikeX, strikeY)
                .view(animatedTexture)
                .zIndex(1000)
                .buildAndAttach();

        double strikeHeight = position.getY();
        lightning.setScaleY(strikeHeight / frameHeight);
        cameraShake();

        FXGL.getGameTimer().runOnceAfter(lightning::removeFromWorld, Duration.seconds(0.5));
    }


    void cameraShake(){
        Node root = FXGL.getGameScene().getRoot();

        TranslateTransition shake = new TranslateTransition(Duration.seconds(0.1), root);
        shake.setFromY(-3);
        shake.setToY(3);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> root.setTranslateY(0)); // reset just in case
        shake.play();
    }
}