package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class PlayerComponent extends Component {
    private double speed = 1.5; // PLAYER SPEED
    int health = 100;

    private Entity healthBar;
    private Rectangle healthBarBackground;
    private Rectangle healthBarFill;
    private final double HEALTH_BAR_WIDTH = 40;
    private final double HEALTH_BAR_HEIGHT = 5;
    private final double HEALTH_BAR_Y_OFFSET = 15; // DISTANCE ABOVE PLAYER

    private AnimatedTexture texture;
    private AnimationChannel animIdleLeft;
    private AnimationChannel animIdleRight;
    private AnimationChannel animWalkLeft;
    private AnimationChannel animWalkRight;
    private GameApp gameApp;

    private boolean isMoving = false;
    private Point2D previousPosition;

    // KURT'S DEBUGGER VARIABLES
    private boolean isAlive = true;

    // PLAYER DIMENSIONS
    private final double PLAYER_WIDTH = 96 * 0.75;
    private final double PLAYER_HEIGHT = 96 * 0.75;
    private final double PLAYER_CENTER_OFFSET_X = PLAYER_WIDTH / 2;
    private final double PLAYER_CENTER_OFFSET_Y = PLAYER_HEIGHT / 2;

    private final double HITBOX_WIDTH = 24;
    private final double HITBOX_HEIGHT = 45;
    private final double HITBOX_CENTER_X = HITBOX_WIDTH / 2;
    private final double HITBOX_CENTER_Y = HITBOX_HEIGHT / 2;

    public PlayerComponent() {
        animIdleLeft = new AnimationChannel(FXGL.image("player-scaled.png"), 5,
                96, 96, Duration.seconds(0.8), 0, 4);
        animIdleRight = new AnimationChannel(FXGL.image("player-scaled.png"), 5,
                96, 96, Duration.seconds(0.8), 5, 9);
        animWalkLeft = new AnimationChannel(FXGL.image("player-scaled.png"), 8,
                96, 96, Duration.seconds(0.6), 16, 23);
        animWalkRight = new AnimationChannel(FXGL.image("player-scaled.png"), 8,
                96, 96, Duration.seconds(0.6), 24, 31);

        texture = new AnimatedTexture(animIdleLeft);
        texture.loop();
    }

    public String getPlayerName() {
        return FXGL.getWorldProperties().getString("playerName");
    }

    public void idleAnimation() {
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double screenWidth = FXGL.getGameScene().getAppWidth();

        if (mouseScreenPos.getX() < screenWidth / 2) {                  // MOUSE IS ON LEFT SIDE OF SCREEN
            if (texture.getAnimationChannel() != animIdleLeft) {
                texture.loopAnimationChannel(animIdleLeft);
            }
        } else {                                                        // MOUSE IS ON RIGHT SIDE OF SCREEN
            if (texture.getAnimationChannel() != animIdleRight) {
                texture.loopAnimationChannel(animIdleRight);
            }
        }
    }

    public void walkAnimation() {
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double screenWidth = FXGL.getGameScene().getAppWidth();

        if (mouseScreenPos.getX() < screenWidth / 2) {                  // MOUSE IS ON LEFT SIDE OF SCREEN
            if (texture.getAnimationChannel() != animWalkLeft) {
                texture.loopAnimationChannel(animWalkLeft);
            }
        } else {                                                        // MOUSE IS ON RIGHT SIDE OF SCREEN
            if (texture.getAnimationChannel() != animWalkRight) {
                texture.loopAnimationChannel(animWalkRight);
            }
        }
    }

    private void createHealthBar() {
        // BLACK BACKGROUND
        healthBarBackground = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarBackground.setFill(Color.BLACK);

        // GREEN FILL
        healthBarFill = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarFill.setFill(Color.GREEN);

        // GROUP TO HOLD BOTH RECTANGLES
        var healthBarGroup = new javafx.scene.Group();
        healthBarGroup.getChildren().addAll(healthBarBackground, healthBarFill);

        // ENTITY TO FOLLOW PLAYER
        healthBar = FXGL.entityBuilder()
                .view(healthBarGroup)
                .build();

        FXGL.getGameWorld().addEntity(healthBar);
        updateHealthBar();
    }

    private void updateHealthBar() {
        if (healthBar != null) {
            // POS ABOVE PLAYER
            double xPos = entity.getX() + (HITBOX_WIDTH / 2) - (HEALTH_BAR_WIDTH / 2);
            double yPos = entity.getY() - HEALTH_BAR_Y_OFFSET;
            healthBar.setPosition(xPos, yPos);

            // CHANGE COLOR BASED ON HEALTH %
            double healthPercentage = Math.max(0, health) / 100.0;
            healthBarFill.setWidth(HEALTH_BAR_WIDTH * healthPercentage);
            if (healthPercentage > 0.6) {
                healthBarFill.setFill(Color.GREEN);
            } else if (healthPercentage > 0.3) {
                healthBarFill.setFill(Color.YELLOW);
            } else {
                healthBarFill.setFill(Color.RED);
            }
        }
    }

    @Override
    public void onAdded() {
        // PLAYER SCALING
        texture.setScaleX(0.75);
        texture.setScaleY(0.75);

        entity.getViewComponent().addChild(texture);

        // ADJUST TO ALIGN WITH HITBOX
        texture.setTranslateX(-35);
        texture.setTranslateY(-27);

        previousPosition = entity.getPosition();
        createHealthBar();
      
        gameApp = entity.getObject("gameApp");
    }

    // UPDATE ANIMATION BASED ON MOVEMENT
    @Override
    public void onUpdate(double tpf) {
        if(!isAlive) return;
        Point2D currentPosition = entity.getPosition();
        isMoving = !currentPosition.equals(previousPosition);

        if (isMoving) {
            walkAnimation();
        } else {
            idleAnimation();
        }

        previousPosition = currentPosition;
        updateHealthBar();
    }

    public void moveLeft() {
        if(!isAlive) return;
        entity.translateX(-speed);
        boundPlayerInWorld();
    }

    public void moveRight() {
        if(!isAlive) return;
        entity.translateX(speed);
        boundPlayerInWorld();
    }

    public void moveUp() {
        if(!isAlive) return;
        entity.translateY(-speed);
        boundPlayerInWorld();
    }
    public void moveDown() {
        if(!isAlive) return;
        entity.translateY(speed);
        boundPlayerInWorld();
    }

    public int getHealth(){
        return health;
    }

    // KEEP FROM GOING OUT OF BOUNDS
    private void boundPlayerInWorld() {
        double worldWidth = FXGL.getAppWidth() * 2;
        double worldHeight = FXGL.getAppHeight() * 2;

        // GET BOUNDARY USING PLAYER SIZE
        if (entity.getX() < 0) {
            entity.setX(0);
        } else if (entity.getX() > worldWidth - PLAYER_WIDTH) {
            entity.setX(worldWidth - PLAYER_WIDTH);
        }

        if (entity.getY() < 0) {
            entity.setY(0);
        } else if (entity.getY() > worldHeight - PLAYER_HEIGHT) {
            entity.setY(worldHeight - PLAYER_HEIGHT);
        }
    }

    public void shoot() {
        if(!isAlive) return;
        // GET MOUSE POS AND CONVERT TO WORLD POSITION
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(
                mouseScreenPos.getX() + viewportX,
                mouseScreenPos.getY() + viewportY
        );

        // SPAWN BULLET AT PLAYER LOCATION
        Point2D bulletSpawnPoint = new Point2D(
                entity.getX(),
                entity.getY()
        );

        // GET DIRECTION FROM PLAYER TO MOUSE
        Point2D direction = mouseWorldPos.subtract(bulletSpawnPoint).normalize();


        Entity bullet = FXGL.spawn("bullet", bulletSpawnPoint);
        bullet.getComponent(BulletComponent.class).setDirection(direction);
    }

    public void shootTripleBurst() {
        if(!isAlive) return;
        // GET MOUSE POS AND CONVERT TO WORLD POSITION
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(
                mouseScreenPos.getX() + viewportX,
                mouseScreenPos.getY() + viewportY
        );


        // SPAWN BULLET AT PLAYER LOCATION
        Point2D bulletSpawnPoint = new Point2D(
                entity.getX(),
                entity.getY()
        );

        // GET DIRECTION FROM PLAYER TO MOUSE
        Point2D direction = mouseWorldPos.subtract(bulletSpawnPoint).normalize();

        // SHOTGUN BANG BUSLOT KALAG
        spawnBulletWithAngle(bulletSpawnPoint, direction, 0);  // CENTER BULLET
        spawnBulletWithAngle(bulletSpawnPoint, direction, -10); // LEFT
        spawnBulletWithAngle(bulletSpawnPoint, direction, 10);  // RIGHT
    }

    // SPAWN BALA
    private void spawnBulletWithAngle(Point2D spawnPoint, Point2D direction, double angleDegrees) {
        Point2D rotatedDirection = rotate(direction, angleDegrees);
        Entity bullet = FXGL.spawn("bullet", spawnPoint);
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

    public void damage(int dmg) {
        if(!isAlive) return;
        health -= dmg;
        if(health < 0) health = 0;
        FXGL.getWorldProperties().setValue("health", health); // Sync with world property

        // para dili mag clutter ang sa console
        if (isAlive) {
            System.out.println("DEBUG: player health = " + health);
        }

        // FLASH RED WHEN HIT
        javafx.scene.effect.ColorAdjust colorAdjust = new javafx.scene.effect.ColorAdjust();
        colorAdjust.setHue(-0.1);
        colorAdjust.setSaturation(0.7);
        colorAdjust.setBrightness(0.3);
        colorAdjust.setContrast(0.2);
        texture.setEffect(colorAdjust);
        FXGL.getGameTimer().runOnceAfter(() -> texture.setEffect(null), Duration.millis(150));

        showDamageText(dmg);
        updateHealthBar();

        if (health <= 0 && isAlive) {
            System.out.println("Player dead");
            isAlive = false;
            if(gameApp != null){
                gameApp.stopTimer();
            }else{
                System.err.println("Warning: gameApp is null, cannot stop timer");
            }

            FXGL.getGameController().pauseEngine();

            int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");
            VBox gameOverMenu = new VBox(10);
            gameOverMenu.setAlignment(javafx.geometry.Pos.CENTER);
            gameOverMenu.setPadding(new javafx.geometry.Insets(20));
            gameOverMenu.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-border-color: white; -fx-border-width: 2;");

            Text gameOverText = new Text("Game Over!");
            gameOverText.setFill(Color.RED);
            gameOverText.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 36));

            Text survivalText = new Text("You survived for " + survivalTime + " seconds");
            survivalText.setFill(Color.WHITE);
            survivalText.setFont(javafx.scene.text.Font.font("Arial", 24));

            Button menuButton = new Button("Back to Main Menu");
            menuButton.setStyle("-fx-font-size: 16; -fx-background-color: #444; -fx-text-fill: white;");
            menuButton.setOnAction(e -> FXGL.getGameController().gotoMainMenu());

            gameOverMenu.getChildren().addAll(gameOverText, survivalText, menuButton);

            FXGL.getDialogService().showBox("Game Over", gameOverMenu, menuButton);

        }
    }


    private void showDamageText(double dmg) {
        var damageText = FXGL.getUIFactoryService().newText(String.valueOf((int) dmg), Color.RED, 18);
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

    @Override
    public void onRemoved() {
        if (healthBar != null) {
            healthBar.removeFromWorld();
        }
    }

}
