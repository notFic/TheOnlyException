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
import java.sql.*;

/*                         !!    REGARDING POWER-UP IMPLEMENTATION    !!
    note for future kurt: ang pag activate sa power-ups kay ma triggered within the onAdded sa dinhi nga file,
    WALA SA ONUPDATE GIPLACE ANG ACTIVATION, NAA SA ONADDED!!
    you have to set the variables first, then an interval timer, with the activation method inside the timer.
    this is so that kada given na timer, it will activate that power up and the process happens naturally since
    naa man sa onAdded nakabutang; this means na ma "initialize" siya once, and it will keep running until the
    end of the game.
 */

// Component controlling player movement, animations, health, and game progress
public class PlayerComponent extends Component {
    private double speed = 1.5; // Player movement speed
    private int health = 100; // Current health
    private int maxHealth = 100; // Maximum health, increases on level-up
    private int level = 1; // Current level
    private int exp = 0; // Current experience points
    private int expToNextLevel = 100; // EXP needed for next level

    private Entity healthBar; // Health bar entity
    private Rectangle healthBarBackground; // Health bar background
    private Rectangle healthBarFill; // Health bar fill
    private final double HEALTH_BAR_WIDTH = 40; // Health bar width
    private final double HEALTH_BAR_HEIGHT = 5; // Health bar height
    private final double HEALTH_BAR_Y_OFFSET = 15; // Distance above player

    private AnimatedTexture texture; // Player sprite texture
    private AnimationChannel animIdleLeft; // Idle animation (left-facing)
    private AnimationChannel animIdleRight; // Idle animation (right-facing)
    private AnimationChannel animWalkLeft; // Walk animation (left-facing)
    private AnimationChannel animWalkRight; // Walk animation (right-facing)
    private GameApp gameApp; // Reference to main game app

    private boolean isMoving = false; // Tracks if player is moving
    private Point2D previousPosition; // Previous position for movement detection
    private boolean isAlive = true; // Tracks if player is alive

    // Player and hitbox dimensions
    private final double PLAYER_WIDTH = 96 * 0.75;
    private final double PLAYER_HEIGHT = 96 * 0.75;
    private final double HITBOX_WIDTH = 24;
    private final double HITBOX_HEIGHT = 45;

    // kurt's shit
    private LightningStrike lightningstrike;

    // Initialize player animations
    public PlayerComponent() {
        animIdleLeft = new AnimationChannel(FXGL.image("player-scaled.png"), 5, 96, 96, Duration.seconds(0.8), 0, 4);
        animIdleRight = new AnimationChannel(FXGL.image("player-scaled.png"), 5, 96, 96, Duration.seconds(0.8), 5, 9);
        animWalkLeft = new AnimationChannel(FXGL.image("player-scaled.png"), 8, 96, 96, Duration.seconds(0.6), 16, 23);
        animWalkRight = new AnimationChannel(FXGL.image("player-scaled.png"), 8, 96, 96, Duration.seconds(0.6), 24, 31);

        texture = new AnimatedTexture(animIdleLeft);
        texture.loop();
    }

    // Get player's username
    public String getPlayerName() {
        return FXGL.getWorldProperties().getString("playerName");
    }

    // Play idle animation based on mouse position
    public void idleAnimation() {
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double screenWidth = FXGL.getGameScene().getAppWidth();
        if (mouseScreenPos.getX() < screenWidth / 2) {
            if (texture.getAnimationChannel() != animIdleLeft) {
                texture.loopAnimationChannel(animIdleLeft);
            }
        } else {
            if (texture.getAnimationChannel() != animIdleRight) {
                texture.loopAnimationChannel(animIdleRight);
            }
        }
    }

    // Play walk animation based on mouse position
    public void walkAnimation() {
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double screenWidth = FXGL.getGameScene().getAppWidth();
        if (mouseScreenPos.getX() < screenWidth / 2) {
            if (texture.getAnimationChannel() != animWalkLeft) {
                texture.loopAnimationChannel(animWalkLeft);
            }
        } else {
            if (texture.getAnimationChannel() != animWalkRight) {
                texture.loopAnimationChannel(animWalkRight);
            }
        }
    }

    // Create health bar above player
    private void createHealthBar() {
        healthBarBackground = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT, Color.BLACK);
        healthBarFill = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT, Color.GREEN);
        var healthBarGroup = new javafx.scene.Group(healthBarBackground, healthBarFill);
        healthBar = FXGL.entityBuilder().view(healthBarGroup).build();
        FXGL.getGameWorld().addEntity(healthBar);
        updateHealthBar();
    }

    // Update health bar position and appearance
    private void updateHealthBar() {
        if (healthBar != null) {
            double xPos = entity.getX() + (HITBOX_WIDTH / 2) - (HEALTH_BAR_WIDTH / 2);
            double yPos = entity.getY() - HEALTH_BAR_Y_OFFSET;
            healthBar.setPosition(xPos, yPos);

            double healthPercentage = Math.max(0, health) / (double) maxHealth;
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

    // Initialize player on addition to game world
    @Override
    public void onAdded() {
        texture.setScaleX(0.75);
        texture.setScaleY(0.75);
        entity.getViewComponent().addChild(texture);
        texture.setTranslateX(-35); // Align with hitbox
        texture.setTranslateY(-27);
        previousPosition = entity.getPosition();
        createHealthBar();
        gameApp = entity.getObject("gameApp");

        // kurt's shit
        // dinhi siguro iactivate ang tanan powerups once ang player maka unlock nila

        lightningstrike = new LightningStrike();

        FXGL.getGameTimer().runAtInterval(() -> {
            lightningstrike.activatePowerUp();
        }, Duration.seconds(5));
    }

    // Update player state each frame
    @Override
    public void onUpdate(double tpf) {
        if (!isAlive) return;

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

    // Move player left
    public void moveLeft() {
        if (!isAlive) return;
        entity.translateX(-speed);
        boundPlayerInWorld();
    }

    // Move player right
    public void moveRight() {
        if (!isAlive) return;
        entity.translateX(speed);
        boundPlayerInWorld();
    }

    // Move player up
    public void moveUp() {
        if (!isAlive) return;
        entity.translateY(-speed);
        boundPlayerInWorld();
    }

    // Move player down
    public void moveDown() {
        if (!isAlive) return;
        entity.translateY(speed);
        boundPlayerInWorld();
    }

    // Get current health
    public int getHealth() {
        return health;
    }

    // Keep player within world bounds
    private void boundPlayerInWorld() {
        double worldWidth = FXGL.getAppWidth() * 2;
        double worldHeight = FXGL.getAppHeight() * 2;
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

    // Shoot a single bullet toward mouse
    public void shoot() {
        if (!isAlive) return;
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(mouseScreenPos.getX() + viewportX, mouseScreenPos.getY() + viewportY);
        Point2D bulletSpawnPoint = new Point2D(entity.getX(), entity.getY());
        Point2D direction = mouseWorldPos.subtract(bulletSpawnPoint).normalize();
        Entity bullet = FXGL.spawn("bullet", bulletSpawnPoint);
        bullet.getComponent(BulletComponent.class).setDirection(direction);
    }

    // Shoot three bullets in a spread
    public void shootTripleBurst() {
        if (!isAlive) return;
        Point2D mouseScreenPos = FXGL.getInput().getMousePositionUI();
        double viewportX = FXGL.getGameScene().getViewport().getX();
        double viewportY = FXGL.getGameScene().getViewport().getY();
        Point2D mouseWorldPos = new Point2D(mouseScreenPos.getX() + viewportX, mouseScreenPos.getY() + viewportY);
        Point2D bulletSpawnPoint = new Point2D(entity.getX(), entity.getY());
        Point2D direction = mouseWorldPos.subtract(bulletSpawnPoint).normalize();
        spawnBulletWithAngle(bulletSpawnPoint, direction, 0); // Center
        spawnBulletWithAngle(bulletSpawnPoint, direction, -10); // Left
        spawnBulletWithAngle(bulletSpawnPoint, direction, 10); // Right
    }

    // Spawn a bullet with an angle offset
    private void spawnBulletWithAngle(Point2D spawnPoint, Point2D direction, double angleDegrees) {
        Point2D rotatedDirection = rotate(direction, angleDegrees);
        Entity bullet = FXGL.spawn("bullet", spawnPoint);
        bullet.getComponent(BulletComponent.class).setDirection(rotatedDirection);
    }

    // Rotate a vector by an angle
    private Point2D rotate(Point2D vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        double newX = vector.getX() * cos - vector.getY() * sin;
        double newY = vector.getX() * sin + vector.getY() * cos;
        return new Point2D(newX, newY);
    }

    // Apply damage to player
    public void damage(int dmg) {
        if (!isAlive) return;
        health -= dmg;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
        FXGL.getWorldProperties().setValue("health", health);
        if (isAlive) {
            System.out.println("DEBUG: player health = " + health);
        }

        // Apply damage effect
        javafx.scene.effect.ColorAdjust colorAdjust = new javafx.scene.effect.ColorAdjust();
        colorAdjust.setHue(-0.1);
        colorAdjust.setSaturation(0.7);
        colorAdjust.setBrightness(0.3);
        colorAdjust.setContrast(0.2);
        texture.setEffect(colorAdjust);
        FXGL.getGameTimer().runOnceAfter(() -> texture.setEffect(null), Duration.millis(150));

        showDamageText(dmg);
        updateHealthBar();

        // Handle player death
        if (!isAlive) {
            System.out.println("Player dead");
            if (gameApp != null) {
                gameApp.stopTimer();
            } else {
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
            Text levelText = new Text("Reached Level: " + level);
            levelText.setFill(Color.WHITE);
            levelText.setFont(javafx.scene.text.Font.font("Arial", 24));

            Button menuButton = new Button("Back to Main Menu");
            menuButton.setStyle("-fx-font-size: 16; -fx-background-color: #444; -fx-text-fill: white;");
            menuButton.setOnAction(e -> {
                resetPlayerState();
                gameApp.resetGameState();
                FXGL.getGameController().gotoMainMenu();
                FXGL.getGameController().resumeEngine();
            });

            gameOverMenu.getChildren().addAll(gameOverText, survivalText, levelText, menuButton);
            FXGL.getDialogService().showBox("Game Over", gameOverMenu, menuButton);

            saveProgress();
        }
    }

    // Reset player state for a new game
    public void resetPlayerState() {
        health = 100;
        maxHealth = 100;
        level = 1;
        exp = 0;
        expToNextLevel = 100;
        speed = 1.5;
        isAlive = true;
        FXGL.getWorldProperties().setValue("health", health);
        FXGL.getWorldProperties().setValue("level", level);
        FXGL.getWorldProperties().setValue("exp", exp);
        System.out.println("Player state reset: health=" + health + ", isAlive=" + isAlive + ", level=" + level);
        updateHealthBar();
    }

    // Save game progress to database
    private void saveProgress() {
        String currentPlayer = getPlayerName();
        int survivedTime = FXGL.getWorldProperties().getInt("survivalTime");
        int totalDmgInflicted = FXGL.getWorldProperties().getInt("totalDamage");
        int kills = FXGL.getWorldProperties().getInt("kills");

        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            String selectPlayerQuery = "SELECT id FROM users WHERE username = ?";
            int playerId;
            try (var pstmt = connection.prepareStatement(selectPlayerQuery)) {
                pstmt.setString(1, currentPlayer);
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    playerId = rs.getInt("id");
                } else {
                    System.err.println("User not found: " + currentPlayer);
                    return;
                }
            }

            String insertSessionQuery = "INSERT INTO game_session (player_id, survival_time, total_dmg_inflicted, kills, session_date) VALUES (?, ?, ?, ?, ?)";
            try (var pstmt = connection.prepareStatement(insertSessionQuery)) {
                pstmt.setInt(1, playerId);
                pstmt.setInt(2, survivedTime);
                pstmt.setInt(3, totalDmgInflicted);
                pstmt.setInt(4, kills);
                pstmt.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                pstmt.executeUpdate();
                System.out.println("Game session saved for player: " + currentPlayer);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Show floating damage text
    private void showDamageText(double dmg) {
        var damageText = FXGL.getUIFactoryService().newText(String.valueOf((int) dmg), Color.RED, 18);
        var textEntity = FXGL.entityBuilder().at(entity.getPosition().subtract(0, 30)).view(damageText).buildAndAttach();
        FXGL.animationBuilder().duration(Duration.seconds(1)).translate(textEntity).from(textEntity.getPosition()).to(textEntity.getPosition().subtract(0, 30)).build().start();
        FXGL.animationBuilder().duration(Duration.seconds(1)).fadeOut(textEntity).build().start();
        FXGL.getGameTimer().runOnceAfter(() -> textEntity.removeFromWorld(), Duration.seconds(1));
    }

    // Add experience points and check for level-up
    public void addExp(int expGained) {
        if (!isAlive) return;
        exp += expGained;
        FXGL.getWorldProperties().setValue("exp", exp);
        System.out.println("DEBUG: Player gained " + expGained + " EXP, total EXP = " + exp);
        while (exp >= expToNextLevel) {
            levelUp();
        }
    }

    // Level up player and apply stat boosts
    private void levelUp() {
        level++;
        exp -= expToNextLevel;
        expToNextLevel = (int) (expToNextLevel * 1.5);
        FXGL.getWorldProperties().setValue("level", level);
        FXGL.getWorldProperties().setValue("exp", exp);
        maxHealth += 20;
        health = maxHealth;
        speed += 0.2;
        FXGL.getWorldProperties().setValue("health", health);
        FXGL.getNotificationService().pushNotification("Level Up! Reached Level " + level);
        System.out.println("DEBUG: Player leveled up to Level " + level + ", Max Health = " + maxHealth + ", Speed = " + speed);
        updateHealthBar();
    }

    // Get player level
    public int getLevel() {
        return level;
    }

    // Get current EXP
    public int getExp() {
        return exp;
    }

    // Get EXP needed for next level
    public int getExpToNextLevel() {
        return expToNextLevel;
    }

    // Clean up health bar on removal
    @Override
    public void onRemoved() {
        if (healthBar != null) {
            healthBar.removeFromWorld();
        }
    }
}