package org.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.scene.SubScene;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.powerups.AutoHealComponent;
import org.example.powerups.ExplosiveMinesComponent;
import org.example.core.GameApp;
import org.example.scenes.LevelUpMenu;
import org.example.powerups.FireTrailComponent;
import org.example.powerups.LightningStrikeComponent;
import org.example.powerups.PoisonAuraComponent;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

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

    // Weapon and powerup tracking
    private Map<String, Integer> weaponLevels = new HashMap<>();
    private LightningStrikeComponent lightningstrike;
    private PoisonAuraComponent poisonaura;
    private FireTrailComponent firetrail;
    private ExplosiveMinesComponent explosiveMines;
    private AutoHealComponent autoHeal;

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

        // Only initialize weapon levels map if it doesn't exist
        if (weaponLevels == null) {
            weaponLevels = new HashMap<>();
        }

        // Initialize available powerups based on acquired weapons
        initializeAcquiredPowerups();
    }

    // Initialize powerups based on acquired weapons
    private void initializeAcquiredPowerups() {
        // Initialize lightning strike if acquired
        if (getWeaponLevel("lightning") > 0) {
            initializeLightningStrike();
        }
        if (getWeaponLevel("poison") > 0) {
            initializePoisonAura();
        }
        if (getWeaponLevel("fire_trail") > 0) {
            initializeFireTrail();
        }
        if (getWeaponLevel("explosive_mines") > 0) {
            initializeExplosiveMines();
        }
        if (getWeaponLevel("auto_heal") > 0) {
            initializeAutoHeal();
        }
    }

    // Recreate all powerup timers to prevent stacking after pauses
    public void reinitializePowerupTimers() {
        // First clear any existing powerup timers
        // We must recreate them instead of just activating them to avoid stacking

        // Create a new lightning strike timer if weapon is acquired
        if (getWeaponLevel("lightning") > 0) {
            System.out.println("Creating lightning strike timer");

            // Activate lightning strike every 5 seconds
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isAlive && getWeaponLevel("lightning") > 0 && lightningstrike != null) {
                    System.out.println("Lightning strike activated");
                    lightningstrike.activatePowerUp();
                }
            }, Duration.seconds(5));
        }

        // Create a new explosive mines timer if weapon is acquired
        if (getWeaponLevel("explosive_mines") > 0) {
            System.out.println("Creating explosive mines timer");

            // Activate explosive mines every 2 seconds
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isAlive && getWeaponLevel("explosive_mines") > 0 && explosiveMines != null) {
                    System.out.println("Explosive mines activated");
                    explosiveMines.activatePowerUp();
                }
            }, Duration.seconds(2));
        }

        // Create a new auto heal timer if powerup is acquired
        if (getWeaponLevel("auto_heal") > 0) {
            System.out.println("Creating auto heal timer");

            // Activate auto heal every 5 seconds
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isAlive && getWeaponLevel("auto_heal") > 0 && autoHeal != null) {
                    System.out.println("Auto heal activated");
                    autoHeal.activatePowerUp();
                }
            }, Duration.seconds(5));
        }
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
        if (!isAlive || (autoHeal != null && autoHeal.isInvulnerable())) return;

        // Check for Safe Mode invulnerability
        if (autoHeal != null && autoHeal.canTriggerInvulnerability() && (health - dmg) <= (maxHealth * 0.1)) {
            autoHeal.triggerInvulnerability();
            return;
        }

        health -= dmg;
        FXGL.getWorldProperties().setValue("health", health);

        // Check if player died
        if (health <= 0) {
            health = 0;
            isAlive = false;
            FXGL.getWorldProperties().setValue("health", health);
            System.out.println("Player dead");

            // Stop game timers
            if (gameApp != null) {
                gameApp.stopTimer();
            } else {
                System.err.println("Warning: gameApp is null, cannot stop timer");
            }
            int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");

            saveProgress();
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

        // Clear weapon levels
        weaponLevels.clear();

        FXGL.getWorldProperties().setValue("health", health);
        FXGL.getWorldProperties().setValue("level", level);
        FXGL.getWorldProperties().setValue("exp", exp);
        System.out.println("Player state reset: health=" + health + ", isAlive=" + isAlive + ", level=" + level);
        updateHealthBar();
    }

    // Handle powerup timers after pause
    public void reinitializeAfterPause() {
        // Recreate all powerup timers
        reinitializePowerupTimers();
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
            String selectPlayerQuery = "SELECT player_id FROM player WHERE username = ?";
            int playerId;
            try (var pstmt = connection.prepareStatement(selectPlayerQuery)) {
                pstmt.setString(1, currentPlayer);
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    playerId = rs.getInt("player_id");
                } else {
                    System.err.println("Player not found: " + currentPlayer);
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

        // Store original exp value (will be negative after subtracting expToNextLevel)
        int originalExp = exp - expToNextLevel;

        // Temporarily set exp to full for UI display purposes
        exp = expToNextLevel;
        FXGL.getWorldProperties().setValue("exp", exp);

        // Update game world properties
        FXGL.getWorldProperties().setValue("level", level);
        FXGL.getWorldProperties().setValue("health", health);

        // Force UI update if game app is available
        if (gameApp != null) {
            gameApp.updateExpBar();
        }

        // Show level up menu with weapon choices
        showLevelUpMenu();

        // After menu is shown, reset exp to correct value
        exp = originalExp;
        expToNextLevel = (int) (expToNextLevel * 1.5);
        FXGL.getWorldProperties().setValue("exp", exp);

        updateHealthBar();
    }

    // Show the level up menu
    private void showLevelUpMenu() {
        LevelUpMenu menu = new LevelUpMenu(this);
        menu.show();
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

    // Get the level of a weapon or powerup
    public int getWeaponLevel(String weaponId) {
        return weaponLevels.getOrDefault(weaponId, 0);
    }

    // Get the reference to the game app
    public GameApp getGameApp() {
        return gameApp;
    }

    // Handle weapon selection from level-up menu (without resuming the engine)
    public void onWeaponSelectedNoResume(String weaponId, int newLevel) {
        // Update the weapon/powerup level
        weaponLevels.put(weaponId, newLevel);
        System.out.println("Selected weapon/powerup: " + weaponId + " at level " + newLevel);

        // Initialize specific powerups if selected for the first time
        if (newLevel == 1) {
            if ("lightning".equals(weaponId)) {
                System.out.println("Setting up Lightning Strike for first time");
                initializeLightningStrike();
                FXGL.getNotificationService().pushNotification("Acquired Lightning Strike!");
            }
            if ("poison".equals(weaponId)) {
                initializePoisonAura();
                poisonaura.activatePowerUp();
                FXGL.getNotificationService().pushNotification("Acquired Poison Aura!");
            }
            if ("fire_trail".equals(weaponId)) {
                initializeFireTrail();
                firetrail.activatePowerUp();
                FXGL.getNotificationService().pushNotification("Acquired Fire Trail!");
            }
            if ("explosive_mines".equals(weaponId)) {
                initializeExplosiveMines();
                explosiveMines.activatePowerUp();
                FXGL.getNotificationService().pushNotification("Acquired Data Wipe!");
            }
            if ("auto_heal".equals(weaponId)) {
                initializeAutoHeal();
                autoHeal.activatePowerUp();
                FXGL.getNotificationService().pushNotification("Acquired System Restore!");
            }
        }
    }
    
    // Handle weapon selection from level-up menu
    public void onWeaponSelected(String weaponId, int newLevel) {
        // Update the weapon/powerup level using the non-resuming method
        onWeaponSelectedNoResume(weaponId, newLevel);

        // Resume fire trail damage after level-up menu closes
        if (firetrail != null && getWeaponLevel("fire_trail") > 0) {
            firetrail.resumePowerUp();
        }

        // Resume the game
        if (gameApp != null) {
            gameApp.resetTimers(); // Reset timers to prevent speed-up bug
        }
        FXGL.getGameController().resumeEngine();
    }

    // Clean up health bar on removal
    @Override
    public void onRemoved() {
        if (healthBar != null) {
            healthBar.removeFromWorld();
        }
    }

    // Initialize the lightning strike weapon
    private void initializeLightningStrike() {
        System.out.println("Initializing lightning strike component");
        // Always create a new component to avoid stale references
        lightningstrike = new LightningStrikeComponent();
        entity.addComponent(lightningstrike);

        // Always recreate the timer to avoid stacking
        reinitializePowerupTimers();
    }

    private void initializePoisonAura(){
        if(poisonaura == null){
            poisonaura = new PoisonAuraComponent();
            entity.addComponent(poisonaura);
            poisonaura.activatePowerUp();  // Activate the aura
        }
    }

    private void initializeFireTrail() {
        if (firetrail == null) {
            firetrail = new FireTrailComponent();
            entity.addComponent(firetrail);
            firetrail.activatePowerUp();
        }
    }

    private void initializeExplosiveMines() {
        if (explosiveMines == null) {
            explosiveMines = new ExplosiveMinesComponent();
            entity.addComponent(explosiveMines);
            explosiveMines.activatePowerUp();
        }
    }

    private void initializeAutoHeal() {
        if (autoHeal == null) {
            autoHeal = new AutoHealComponent();
            entity.addComponent(autoHeal);
            autoHeal.activatePowerUp();
        }
    }

    public boolean isMoving() {
        return isMoving;
    }
}