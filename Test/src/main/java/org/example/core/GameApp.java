package org.example.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.scene.shape.Rectangle;
import org.example.data.LeaderboardDatabase;
import org.example.scenes.LeaderboardUI;
import org.example.scenes.MainMenuScene;
import org.example.scenes.NameInputScene;
import org.example.model.Player;
import org.example.components.BulletComponent;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;

import static com.almasb.fxgl.dsl.FXGL.*;

// Main game application class managing game world, UI, and physics
public class GameApp extends GameApplication {

    private boolean hasUpdatedExpBar = false; // Flag to ensure updateExpBar runs only once after init
    private Entity player; // Player entity
    private static String storedPlayerName = "Unknown"; // Player's username
    private static int playerId = -1; // Store the player_id from the player table
    private Random random = new Random(); // For enemy spawning
    private boolean isTimerRunning = true; // Controls game timers
    private boolean isLoggedIn = false; // Track login state
    private AudioClip gameMusic;

    // EXP progress bar UI elements
    private Rectangle expBarFill;
    private Text expProgressText;

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbtheonlyexception";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private boolean isLeaderboardOpen = false;

    public void showLeaderboard() {
        if (isLeaderboardOpen) {
            System.out.println("Leaderboard dialog already open - ignoring request");
            return;
        }
        System.out.println("showLeaderboard called");
        LeaderboardUI leaderboardUI = new LeaderboardUI();
        isLeaderboardOpen = true;
        FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
        leaderboardUI.getCloseButton().setOnAction(e -> {
            isLeaderboardOpen = false;
            System.out.println("Leaderboard dialog closed");
        });
    }

    // Configure game window and main menu
    @Override
    protected void initSettings(GameSettings settings) {
        System.out.println("initSettings called - setting up game settings");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype Game");
        settings.setVersion("0.1.5");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true); // Enable the game menu
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                System.out.println("Creating new NameInputScene as MainMenu");
                return new NameInputScene();
            }
        });
        System.out.println("initSettings completed - main menu and game menu enabled, expecting NameInputScene at startup");
    }

    // Initialize global game variables
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        System.out.println("initGameVars called");
        vars.put("playerName", storedPlayerName);
        vars.put("score", 0);
        vars.put("health", 100);
        vars.put("survivalTime", 0);
        vars.put("level", 1);
        vars.put("exp", 0);
        vars.put("totalDamage", 0);
        vars.put("kills", 0);
        System.out.println("Game vars initialized with playerName: " + storedPlayerName);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (!hasUpdatedExpBar && expBarFill != null) {
            updateExpBar();
            hasUpdatedExpBar = true;
            System.out.println("Initial EXP bar update completed after UI initialization");
        }
    }

    // Store player's username
    public static void startGameWithName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            System.out.println("Static method called with name: " + name);
            storedPlayerName = name;
            // Fetch or create player_id
            playerId = getPlayerId(name);
        }
    }

    private static int getPlayerId(String username) {
        int pId = -1;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Check if player exists
            String selectQuery = "SELECT player_id FROM player WHERE username = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setString(1, username);
                ResultSet resultSet = selectStmt.executeQuery();
                if (resultSet.next()) {
                    pId = resultSet.getInt("player_id");
                    System.out.println("Player found with ID: " + pId);
                    return pId;
                }
            }

            // If player doesn't exist, create a new player
            String insertQuery = "INSERT INTO player (username, created_at) VALUES (?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                insertStmt.executeUpdate();
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    pId = generatedKeys.getInt(1);
                    System.out.println("New player created with ID: " + pId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while fetching/creating player: " + e.getMessage());
            e.printStackTrace();
        }
        return pId;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
        System.out.println("isLoggedIn set to: " + loggedIn);
    }

    // Transition to the MainMenuScene
    public void gotoNewMainMenu() {
        System.out.println("Transitioning to MainMenuScene...");
        // Clear game world entities safely and clear timers
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        FXGL.getGameTimer().clear();
        // Stop game music if playing
        if (gameMusic != null) {
            gameMusic.stop();
            System.out.println("Game music stopped");
            gameMusic = null;
        }
        // Ensure the player remains logged in
        isLoggedIn = true;
        System.out.println("Set isLoggedIn to true for MainMenuScene transition");
        System.out.println("Current isLoggedIn state: " + isLoggedIn);
        // Push the MainMenuScene as a subscene
        FXGL.getSceneService().pushSubScene(new MainMenuScene());
        System.out.println("Transition to MainMenuScene completed");
    }

    // Setup UI elements (name, health, timer, level, EXP)
    @Override
    protected void initUI() {
        Text nameText = getUIFactoryService().newText("", 20);
        nameText.textProperty().bind(getWorldProperties().stringProperty("playerName").concat("'s Game"));
        nameText.setFill(Color.WHITE);
        addUINode(nameText, 20, 20);

        Text healthText = getUIFactoryService().newText("", 24);
        healthText.textProperty().bind(getWorldProperties().intProperty("health").asString("Health: %d"));
        healthText.setFill(Color.RED);
        healthText.setStyle("-fx-font-weight: bold;");
        addUINode(healthText, 20, 50);

        Text timerText = getUIFactoryService().newText("", 24);
        timerText.textProperty().bind(getWorldProperties().intProperty("survivalTime").asString("Time: %d s"));
        timerText.setFill(Color.YELLOW);
        timerText.setStyle("-fx-font-weight: bold;");
        addUINode(timerText, 20, 80);

        Text levelText = getUIFactoryService().newText("", 24);
        levelText.textProperty().bind(getWorldProperties().intProperty("level").asString("Level: %d"));
        levelText.setFill(Color.CYAN);
        levelText.setStyle("-fx-font-weight: bold;");
        addUINode(levelText, 20, 110);

        Text expText = getUIFactoryService().newText("", 24);
        expText.textProperty().bind(getWorldProperties().intProperty("exp").asString("EXP: %d"));
        expText.setFill(Color.YELLOWGREEN);
        expText.setStyle("-fx-font-weight: bold;");
        addUINode(expText, 20, 140);

        // Add EXP progress bar at the bottom of the screen
        Rectangle expBarBackground = new Rectangle(getAppWidth(), 20);
        expBarBackground.setFill(Color.rgb(30, 30, 30, 0.8));
        addUINode(expBarBackground, 0, getAppHeight() - 20);

        expBarFill = new Rectangle(0, 20);
        expBarFill.setFill(Color.YELLOWGREEN);
        addUINode(expBarFill, 0, getAppHeight() - 20);

        // Add EXP text on the progress bar
        expProgressText = getUIFactoryService().newText("", 16);
        expProgressText.setFill(Color.WHITE);
        expProgressText.setStyle("-fx-font-weight: bold;");
        addUINode(expProgressText, getAppWidth() / 2 - 50, getAppHeight() - 5);

        // Initialize the EXP bar once
        updateExpBar();
    }

    // Update experience bar based on player's current exp
    public void updateExpBar() {
        if (expBarFill == null || expProgressText == null) {
            System.out.println("Skipping EXP bar update - UI elements not yet initialized");
            return;
        }
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            int currentExp = playerComponent.getExp();
            int expToNext = playerComponent.getExpToNextLevel();

            // Calculate percentage and update bar width
            double percentage = Math.min(1.0, (double) currentExp / expToNext);
            expBarFill.setWidth(getAppWidth() * percentage);

            // Update text
            expProgressText.setText("EXP: " + currentExp + " / " + expToNext);
        }
    }

    // Bind movement keys (WASD) to player actions
    @Override
    protected void initInput() {
        System.out.println("initInput called");
        onKey(KeyCode.A, () -> {
            if (player != null) {
                player.getComponent(PlayerComponent.class).moveLeft();
            } else {
                System.out.println("Player is null - cannot move left");
            }
        });
        onKey(KeyCode.D, () -> {
            if (player != null) {
                player.getComponent(PlayerComponent.class).moveRight();
            } else {
                System.out.println("Player is null - cannot move right");
            }
        });
        onKey(KeyCode.W, () -> {
            if (player != null) {
                player.getComponent(PlayerComponent.class).moveUp();
            } else {
                System.out.println("Player is null - cannot move up");
            }
        });
        onKey(KeyCode.S, () -> {
            if (player != null) {
                player.getComponent(PlayerComponent.class).moveDown();
            } else {
                System.out.println("Player is null - cannot move down");
            }
        });
        System.out.println("initInput completed");
    }

    // Initialize game world, spawn entities, and setup timers
    @Override
    protected void initGame() {
        System.out.println("initGame called - isLoggedIn: " + isLoggedIn);
        // Only redirect to NameInputScene if the player has never logged in
        if (!isLoggedIn) {
            System.out.println("User not logged in - redirecting to NameInputScene");
            Platform.runLater(() -> FXGL.getGameController().gotoMainMenu());
            return;
        }

        // Clear previous game state
        System.out.println("Clearing existing entities from game world");
        FXGL.getGameWorld().getEntities().forEach(Entity::removeFromWorld);
        FXGL.getGameTimer().clear();

        resetGameState(); // Reset game variables and player state

        FXGL.getGameWorld().addEntityFactory(new GameEntityFactor());

        // Ensure player name is set
        if (!storedPlayerName.equals("Unknown")) {
            FXGL.getWorldProperties().setValue("playerName", storedPlayerName);
            System.out.println("Re-applying stored player name: " + storedPlayerName);
        }

        String playerName = FXGL.getWorldProperties().getString("playerName");
        System.out.println("Player name from world properties: " + playerName);

        // Show welcome notification
        FXGL.runOnce(() -> {
            String currentName = FXGL.getWorldProperties().getString("playerName");
            System.out.println("Showing welcome notification for: " + currentName);
            FXGL.getNotificationService().pushNotification("Welcome, " + currentName + "!");
        }, Duration.seconds(0.2));

        // Set game world dimensions
        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;

        // Spawn background
        SpawnData backgroundData = new SpawnData(0, 0);
        backgroundData.put("worldWidth", worldWidth);
        backgroundData.put("worldHeight", worldHeight);
        spawn("tiledBackground", backgroundData);

        // Spawn player
        SpawnData playerData = new SpawnData(worldWidth / 2.0, worldHeight / 2.0);
        playerData.put("gameApp", this);
        player = spawn("player", playerData);
        System.out.println("Player spawned - player is now: " + (player == null ? "null" : "initialized"));

        // Center camera on player
        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        // Load gameplay music only if not already playing
        if (gameMusic == null) {
            try {
                java.net.URL musicUrl = getClass().getResource("/assets/music/game_music.mp3");
                if (musicUrl == null) {
                    throw new IllegalStateException("Game music file not found at /assets/music/game_music.mp3.");
                }
                gameMusic = new AudioClip(musicUrl.toExternalForm());
                gameMusic.setCycleCount(AudioClip.INDEFINITE);
                gameMusic.setVolume(0.5);
                gameMusic.play();
                System.out.println("Game music loaded and playing successfully");
            } catch (Exception e) {
                System.err.println("Error loading game music: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Game music already loaded, skipping replay");
        }

        // Start survival timer
        isTimerRunning = true;
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getWorldProperties().getInt("survivalTime");
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        // Auto-shoot triple burst every 0.5 seconds
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning && player != null) {
                player.getComponent(PlayerComponent.class).shootTripleBurst();
            }
        }, Duration.seconds(0.2));

        // Spawn enemies at intervals
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("enemy");
        }, Duration.seconds(1));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("fastEnemy");
        }, Duration.seconds(2));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("tankEnemy");
        }, Duration.seconds(3));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("beeEnemy");
        }, Duration.seconds(2.5));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("giantFlyEnemy");
        }, Duration.seconds(6));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("dragonflyEnemy");
        }, Duration.seconds(2));



        resetTimers();
    }

    // Stop all game timers
    public void stopTimer() {
        isTimerRunning = false;
        // Stop the game music before showing the game over screen
        if (gameMusic != null) {
            gameMusic.stop();
            System.out.println("Game music stopped in stopTimer");
            gameMusic = null;
        }
        saveGameSession();
        updateLeaderboard();
        showGameOverScreen();
    }

    private void saveGameSession() {
        if (playerId == -1) {
            System.out.println("Cannot save game session: player_id is invalid");
            return;
        }

        int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");
        int totalDamage = FXGL.getWorldProperties().getInt("totalDamage");
        int kills = FXGL.getWorldProperties().getInt("kills");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String insertQuery = "INSERT INTO game_session (player_id, survival_time, total_dmg_inflicted, kills, session_date) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setInt(1, playerId);
                stmt.setInt(2, survivalTime);
                stmt.setInt(3, totalDamage);
                stmt.setInt(4, kills);
                // Fix timestamp format to match the database column type
                // Use a simpler date format that won't get truncated
                java.util.Date now = new java.util.Date();
                java.sql.Date sqlDate = new java.sql.Date(now.getTime());
                stmt.setDate(5, sqlDate); // Use Date instead of Timestamp
                stmt.executeUpdate();
                System.out.println("Game session saved: Player ID=" + playerId + ", Survival Time=" + survivalTime + ", Total Damage=" + totalDamage + ", Kills=" + kills);
            }
        } catch (SQLException e) {
            System.err.println("Database error while saving game session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLeaderboard() {
        if (playerId == -1) {
            System.out.println("Cannot update leaderboard: player_id is invalid");
            return;
        }

        int survivalTime = FXGL.getWorldProperties().getInt("survivalTime");
        int totalDamage = FXGL.getWorldProperties().getInt("totalDamage");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Check if the player has an entry in the leaderboard
            String selectQuery = "SELECT best_survival_time, total_dmg_inflicted FROM leaderboard WHERE player_id = ?";
            int currentBestSurvivalTime = 0;
            int currentTotalDamage = 0;
            boolean playerExists = false;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, playerId);
                ResultSet resultSet = selectStmt.executeQuery();
                if (resultSet.next()) {
                    playerExists = true;
                    currentBestSurvivalTime = resultSet.getInt("best_survival_time");
                    currentTotalDamage = resultSet.getInt("total_dmg_inflicted");
                }
            }

            // Update or insert leaderboard entry
            if (playerExists) {
                String updateQuery = "UPDATE leaderboard SET best_survival_time = ?, total_dmg_inflicted = ?, last_updated = UNIX_TIMESTAMP() WHERE player_id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    int newBestSurvivalTime = Math.max(currentBestSurvivalTime, survivalTime);
                    int newTotalDamage = currentTotalDamage + totalDamage;
                    updateStmt.setInt(1, newBestSurvivalTime);
                    updateStmt.setInt(2, newTotalDamage);
                    updateStmt.setInt(3, playerId);
                    updateStmt.executeUpdate();
                    System.out.println("Leaderboard updated for Player ID=" + playerId + ": Best Survival Time=" + newBestSurvivalTime + ", Total Damage=" + newTotalDamage);
                }
            } else {
                // Step 1: Calculate the new rank
                int newRank = 1; // Default rank for the first entry
                String rankQuery = "SELECT COALESCE(MAX(rank), 0) + 1 AS new_rank FROM leaderboard";
                try (PreparedStatement rankStmt = connection.prepareStatement(rankQuery)) {
                    ResultSet rankResult = rankStmt.executeQuery();
                    if (rankResult.next()) {
                        newRank = rankResult.getInt("new_rank");
                    }
                }

                // Step 2: Insert the new leaderboard entry with the calculated rank
                String insertQuery = "INSERT INTO leaderboard (player_id, best_survival_time, total_dmg_inflicted, rank, last_updated) VALUES (?, ?, ?, ?, UNIX_TIMESTAMP())";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setInt(1, playerId);
                    insertStmt.setInt(2, survivalTime);
                    insertStmt.setInt(3, totalDamage);
                    insertStmt.setInt(4, newRank);
                    insertStmt.executeUpdate();
                    System.out.println("Leaderboard entry created for Player ID=" + playerId + ": Best Survival Time=" + survivalTime + ", Total Damage=" + totalDamage + ", Rank=" + newRank);
                }
            }

            // Update ranks
            String rankUpdateQuery = "UPDATE leaderboard l SET rank = (SELECT r FROM (SELECT player_id, ROW_NUMBER() OVER (ORDER BY best_survival_time DESC, total_dmg_inflicted DESC) as r FROM leaderboard) ranked WHERE ranked.player_id = l.player_id)";
            try (PreparedStatement rankStmt = connection.prepareStatement(rankUpdateQuery)) {
                rankStmt.executeUpdate();
                System.out.println("Leaderboard ranks updated");
            }
        } catch (SQLException e) {
            System.err.println("Database error while updating leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //KEEP
    private void showGameOverScreen() {
        FXGL.getGameController().pauseEngine();
        Text gameOverText = FXGL.getUIFactoryService().newText("Game Over", Color.RED, 48);
        gameOverText.setStyle("-fx-font-weight: bold;");
        gameOverText.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
        gameOverText.setTranslateY(FXGL.getAppHeight() / 2.0 - 50);

        Text statsText = FXGL.getUIFactoryService().newText(
                "Survival Time: " + FXGL.getWorldProperties().getInt("survivalTime") + " s\n" +
                        "Total Damage: " + FXGL.getWorldProperties().getInt("totalDamage") + "\n" +
                        "Kills: " + FXGL.getWorldProperties().getInt("kills"),
                Color.WHITE, 24
        );
        statsText.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
        statsText.setTranslateY(FXGL.getAppHeight() / 2.0);

        Button backToMenuButton = new Button("Back to Main Menu");
        backToMenuButton.setStyle("-fx-font-size: 20; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        backToMenuButton.setTranslateX(FXGL.getAppWidth() / 2.0 - 80);
        backToMenuButton.setTranslateY(FXGL.getAppHeight() / 2.0 + 100);
        backToMenuButton.setOnAction(e -> {
            getGameScene().clearUINodes();
            FXGL.getGameController().resumeEngine();
            gotoNewMainMenu();
        });

        getGameScene().addUINodes(gameOverText, statsText, backToMenuButton);
    }

    // Start all game timers
    public void startTimer() {
        isTimerRunning = true;
    }

    // Clear and recreate all game timers to prevent speed-up bug
    public void resetTimers() {
        System.out.println("Resetting game timers to prevent speed-up");

        // First stop all timers
        isTimerRunning = false;

        // Clear all existing timers
        FXGL.getGameTimer().clear();

        // Recreate the survival timer
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getWorldProperties().getInt("survivalTime");
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        // Recreate auto-shoot timer
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                player.getComponent(PlayerComponent.class).shootTripleBurst();
            }
        }, Duration.seconds(0.2));

        // Recreate enemy spawn timers
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("enemy");
        }, Duration.seconds(1));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("fastEnemy");
        }, Duration.seconds(2));

        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("tankEnemy");
        }, Duration.seconds(3));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("beeEnemy");
        }, Duration.seconds(2.5));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("giantFlyEnemy");
        }, Duration.seconds(6));
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) spawnEnemyOutsideViewport("dragonflyEnemy");
        }, Duration.seconds(2));


        // Reinitialize player powerup timers
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).reinitializeAfterPause();
        }

        // Restart timers
        isTimerRunning = true;

        System.out.println("All game timers reset successfully");
    }

    // Spawn enemies outside the viewport
    private void spawnEnemyOutsideViewport(String enemyType) {
        // Get viewport bounds
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();

        // Determine spawn position
        double x, y;
        int margin = 50; // Distance outside viewport

        int side = random.nextInt(4);
        switch (side) {
            case 0: // Top
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMinY - margin;
                break;
            case 1: // Right
                x = viewMaxX + margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            case 2: // Bottom
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMaxY + margin;
                break;
            case 3: // Left
                x = viewMinX - margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            default:
                x = viewMinX;
                y = viewMinY;
        }

        // 20% chance to spawn in corners
        if (random.nextDouble() < 0.2) {
            x = viewMinX + (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = viewMinY + (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }

        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    // Define collision physics (bullet-enemy, player-enemy)
    @Override
    protected void initPhysics() {
        // Bullet hits enemy
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            BulletComponent bulletComponent = bullet.getComponent(BulletComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

            int damage = bulletComponent.getDamage();
            enemyComponent.damage(damage, bullet.getPosition());
            FXGL.getWorldProperties().increment("totalDamage", damage); // Track damage
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1); // Track kills
            }
            bullet.removeFromWorld();
        });

        // Enemy damages player
        onCollision(EntityType.PLAYER, EntityType.ENEMY, (player, enemy) -> {
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

            long now = System.nanoTime();
            if (now - enemyComponent.getLastDamageTime() >= 1_000_000_000) {
                int damage = enemyComponent.getDamage();
                playerComponent.damage(damage);
                enemyComponent.setLastDamageTime(now);
            }
        });

        System.out.println("initPhysics completed");
    }

    // Reset game state for a new session
    public void resetGameState() {
        System.out.println("resetGameState called");
        getWorldProperties().setValue("survivalTime", 0);
        getWorldProperties().setValue("health", 100);
        getWorldProperties().setValue("score", 0);
        getWorldProperties().setValue("level", 1);
        getWorldProperties().setValue("exp", 0);
        getWorldProperties().setValue("totalDamage", 0);
        getWorldProperties().setValue("kills", 0);
        isTimerRunning = true;
        player = null;
        hasUpdatedExpBar = false; // Reset the flag for the next game
        System.out.println("Game state reset for new session - player set to null");
    }

    public class MainMenuController {
        private boolean isLeaderboardOpen = false;

        public void showLeaderboard() {
            if (isLeaderboardOpen) {
                System.out.println("Leaderboard dialog already open - ignoring request");
                return;
            }
            System.out.println("showLeaderboard called");
            LeaderboardUI leaderboardUI = new LeaderboardUI();
            isLeaderboardOpen = true;
            FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
            leaderboardUI.getCloseButton().setOnAction(e -> {
                isLeaderboardOpen = false;
                System.out.println("Leaderboard dialog closed");
            });
        }
    }

    // Pause game timers without ending the game
    public void pauseGameTimers() {
        isTimerRunning = false;
        System.out.println("Game timers paused");
    }

    // Resume game timers
    public void resumeGameTimers() {
        isTimerRunning = true;
        System.out.println("Game timers resumed");
        // Reset timers to prevent speed-up bug
        resetTimers();
    }

    public static String getStoredPlayerName() {
        return storedPlayerName;
    }

    // Launch the game
    public static void main(String[] args) {
        launch(args);
    }
}