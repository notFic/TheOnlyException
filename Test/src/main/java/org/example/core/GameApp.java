package org.example.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.time.TimerAction;
import com.almasb.fxgl.ui.FontType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.text.FontWeight;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.controllers.GameOverController;
import org.example.scenes.LoginScene;
import org.example.scenes.RegisterScene;
import org.example.scenes.MainMenuScene;
import org.example.components.LaserComponent;
import org.example.components.SwordComponent;
import org.example.components.VoltChainComponent;
import org.example.scenes.LeaderboardUI;
import org.example.components.BulletComponent;
import org.example.components.EnemyComponent;
import org.example.components.PlayerComponent;

import java.sql.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;
import static javafx.application.Application.launch;

public class GameApp extends GameApplication {

    private boolean hasUpdatedExpBar = false;
    private Entity player;
    private static String storedPlayerName = "Unknown";
    private static int playerId = -1;
    private Random random = new Random();
    private boolean isTimerRunning = true;
    private boolean isLoggedIn = false;
    private MediaPlayer gameMusicPlayer;
    private String userType = "Gun";

    private TimerAction survivalTimerAction;
    private Rectangle expBarFill;
    private Text expProgressText;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbtheonlyexception";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private boolean isLeaderboardOpen = false;

    private WaveManager waveManager;

    public void showLeaderboard() {
        if (isLeaderboardOpen) {
            System.out.println("Leaderboard dialog already open - ignoring request");
            return;
        }
        System.out.println("showLeaderboard called");
        LeaderboardUI leaderboardUI = new LeaderboardUI(storedPlayerName);
        isLeaderboardOpen = true;
        FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
        leaderboardUI.getCloseButton().setOnAction(e -> {
            isLeaderboardOpen = false;
            System.out.println("Leaderboard dialog closed");
        });
    }

    @Override
    protected void initSettings(GameSettings settings) {
        System.out.println("initSettings called - setting up game settings");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Prototype Game");
        settings.setVersion("0.1.5");
        // Load Pixelify Sans fonts
        Font.loadFont(getClass().getResourceAsStream("/fonts/PixelifySans_Bold.ttf"), 52);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PixelifySans_SemiBold.ttf"), 24);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PixelifySans_Medium.ttf"), 18);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PixelifySans_Regular.ttf"), 16);
        System.out.println("Pixelify Sans fonts loaded: Bold, SemiBold, Medium, Regular");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(false);
        
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                System.out.println("Creating LoginScene as initial MainMenu");
                LoginScene loginScene = new LoginScene();
                loginScene.getLoginController().setLoginSuccessCallback(() -> {
                    System.out.println("Login successful - transitioning to MainMenuScene");
                    setLoggedIn(true);
                    gotoNewMainMenu();
                });
                loginScene.getLoginController().setSwitchToRegisterCallback(() -> {
                    System.out.println("Switching to RegisterScene...");
                    RegisterScene registerScene = new RegisterScene();
                    registerScene.getRegisterController().setSwitchToMainMenuCallback(() -> {
                        System.out.println("Registration successful - transitioning to MainMenuScene");
                        setLoggedIn(true);
                        gotoNewMainMenu();
                    });
                    registerScene.getRegisterController().setSwitchToLoginCallback(() -> {
                        System.out.println("Switching back to LoginScene...");
                        FXGL.getSceneService().popSubScene();
                    });
                    FXGL.getSceneService().pushSubScene(registerScene);
                });
                return loginScene;
            }
        });
        System.out.println("initSettings completed");
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        System.out.println("initGameVars called");
        vars.put("playerName", storedPlayerName);
        vars.put("score", 0);
        vars.put("health", 100);
        vars.put("survivalTime", 0);
        vars.put("level", 1);
        vars.put("rawExp", 0); // Raw EXP value
        vars.put("expPercentage", 0); // Percentage for UI
        vars.put("totalDamage", 0);
        vars.put("kills", 0);
        vars.put("wave", 1);
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

    public static void startGameWithName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            System.out.println("Static method called with name: " + name);
            storedPlayerName = name;
            playerId = getPlayerId(name);
        }
    }

    private static int getPlayerId(String username) {
        int pId = -1;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
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


    public void gotoNewMainMenu() {
        System.out.println("Transitioning to MainMenuScene...");
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        FXGL.getGameTimer().clear();
        if (gameMusicPlayer != null) {
            gameMusicPlayer.stop();
            gameMusicPlayer.dispose();
            gameMusicPlayer = null;
        }
        isLoggedIn = true;
        System.out.println("Current isLoggedIn state: " + isLoggedIn);
        FXGL.getSceneService().pushSubScene(new MainMenuScene());
        System.out.println("Transition to MainMenuScene completed via pushSubScene");
    }

    @Override
    protected void initUI() {
        // Create a semi-transparent panel for the HUD
        VBox hudPanel = new VBox(10);
        hudPanel.setPadding(new Insets(10));
        hudPanel.setStyle("-fx-background-color: rgba(25, 25, 112, 0.7); -fx-border-color: #1E90FF; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        hudPanel.setTranslateX(20);
        hudPanel.setTranslateY(20);

        // Neon glow effect for text
        DropShadow neonGlow = new DropShadow();
        neonGlow.setColor(Color.web("#00B7EB"));
        neonGlow.setRadius(10);
        neonGlow.setSpread(0.5);

        // Wave Text
        Text waveText = getUIFactoryService().newText("", Color.web("#87CEEB"), FontType.GAME, 24);
        waveText.setEffect(neonGlow);
        waveText.textProperty().bind(FXGL.getWorldProperties().intProperty("wave").asString("Wave: %d"));
        hudPanel.getChildren().add(waveText);

        // Player Name Text
        Text nameText = getUIFactoryService().newText("", Color.WHITE, FontType.GAME, 20);
        nameText.setEffect(neonGlow);
        nameText.textProperty().bind(getWorldProperties().stringProperty("playerName").concat("'s Game"));
        hudPanel.getChildren().add(nameText);

        // Health Text
        Text healthText = getUIFactoryService().newText("", Color.web("#1E90FF"), FontType.GAME, 24);
        healthText.setEffect(neonGlow);
        healthText.textProperty().bind(getWorldProperties().intProperty("health").asString("Health: %d"));
        hudPanel.getChildren().add(healthText);

        // Timer Text
        Text timerText = getUIFactoryService().newText("", Color.web("#87CEEB"), FontType.GAME, 24);
        timerText.setEffect(neonGlow);
        timerText.textProperty().bind(getWorldProperties().intProperty("survivalTime").asString("Time: %d s"));
        hudPanel.getChildren().add(timerText);

        // Level Text
        Text levelText = getUIFactoryService().newText("", Color.web("#00B7EB"), FontType.GAME, 24);
        levelText.setEffect(neonGlow);
        levelText.textProperty().bind(getWorldProperties().intProperty("level").asString("Level: %d"));
        hudPanel.getChildren().add(levelText);

        // Add HUD panel to scene
        addUINode(hudPanel);

        // EXP Bar Background
        Rectangle expBarBackground = new Rectangle(getAppWidth() - 40, 30);
        expBarBackground.setFill(Color.rgb(25, 25, 112, 0.8));
        expBarBackground.setStroke(Color.web("#1E90FF"));
        expBarBackground.setStrokeWidth(2);
        expBarBackground.setArcWidth(10);
        expBarBackground.setArcHeight(10);
        expBarBackground.setEffect(new DropShadow(10, Color.web("#00B7EB")));
        addUINode(expBarBackground, 20, getAppHeight() - 50);

        // EXP Bar Fill
        expBarFill = new Rectangle(0, 30);
        expBarFill.setFill(Color.web("#87CEEB"));
        expBarFill.setArcWidth(10);
        expBarFill.setArcHeight(10);
        expBarFill.setEffect(new DropShadow(8, Color.web("#00B7EB")));
        addUINode(expBarFill, 20, getAppHeight() - 50);
        expBarFill.toFront();

        // EXP Progress Text
        expProgressText = getUIFactoryService().newText("", Color.WHITE, FontType.GAME, 18);
        expProgressText.setEffect(neonGlow);
        expProgressText.textProperty().bind(getWorldProperties().intProperty("expPercentage").asString("EXP: %d%%"));
        addUINode(expProgressText, getAppWidth() / 2 - 50, getAppHeight() - 35);

        updateExpBar();
    }

    public void updateExpBar() {
        if (expBarFill == null || expProgressText == null) {
            System.out.println("Skipping EXP bar update - UI elements not yet initialized");
            return;
        }

        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            System.out.println("Skipping EXP bar update - player or PlayerComponent not initialized");
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        int currentExp = playerComponent.getExp();
        int expToNext = playerComponent.getExpToNextLevel();

        // Calculate the percentage (capped at 100%)
        double percentage = expToNext > 0 ? Math.min(1.0, (double) currentExp / expToNext) : 0.0;

        // Calculate the width based on the EXP bar background width
        double maxBarWidth = getAppWidth() - 40;
        double fillWidth = maxBarWidth * percentage;

        // Ensure bar is at least 1px wide if there's any EXP
        if (currentExp > 0 && fillWidth < 1) {
            fillWidth = 1;
        }

        // Update the fill width
        expBarFill.setWidth(fillWidth);

        // Update the world property for EXP percentage
        int expPercentage = (int) (percentage * 100);
        getWorldProperties().setValue("expPercentage", expPercentage);

        // Debug output
        System.out.println("EXP Update: currentExp=" + currentExp + ", expToNext=" + expToNext +
                ", percentage=" + expPercentage + "%, fillWidth=" + fillWidth);
    }

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
        
        // Add ESC key for pausing the game
        onKeyDown(KeyCode.ESCAPE, () -> {
            // Only show pause menu if we're in an active game (player exists)
            if (player != null && isTimerRunning) {
                try {
                    System.out.println("Opening pause menu");
                    FXGL.getSceneService().pushSubScene(new org.example.scenes.PauseScene(this));
                } catch (Exception e) {
                    System.err.println("Error showing pause menu: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        
        System.out.println("initInput completed");
    }

    @Override
    protected void initGame() {
        System.out.println("initGame called - isLoggedIn: " + isLoggedIn);
        if (!isLoggedIn) {
            System.out.println("User not logged in - redirecting to LoginScene");
            Platform.runLater(() -> FXGL.getGameController().gotoMainMenu());
            return;
        }

        // Stop any existing game music to ensure a fresh start
        if (gameMusicPlayer != null) {
            gameMusicPlayer.stop();
            gameMusicPlayer.dispose();
            gameMusicPlayer = null;
        }
        
        FXGL.getGameWorld().getEntities().forEach(Entity::removeFromWorld);
        FXGL.getGameTimer().clear();
        resetGameState();

        FXGL.getGameWorld().addEntityFactory(new GameEntityFactory());

        if (!storedPlayerName.equals("Unknown")) {
            FXGL.getWorldProperties().setValue("playerName", storedPlayerName);
            System.out.println("Re-applying stored player name: " + storedPlayerName);
        }

        String playerName = FXGL.getWorldProperties().getString("playerName");
        System.out.println("Player name from world properties: " + playerName);

        FXGL.runOnce(() -> {
            String currentName = FXGL.getWorldProperties().getString("playerName");
            System.out.println("Showing welcome notification for: " + currentName);
            FXGL.getNotificationService().pushNotification("Welcome, " + currentName + "!");
        }, Duration.seconds(0.2));

        int worldWidth = getAppWidth() * 2;
        int worldHeight = getAppHeight() * 2;

        SpawnData backgroundData = new SpawnData(0, 0);
        backgroundData.put("worldWidth", worldWidth);
        backgroundData.put("worldHeight", worldHeight);
        spawn("tiledBackground", backgroundData);

        SpawnData playerData = new SpawnData(worldWidth / 2.0, worldHeight / 2.0);
        playerData.put("gameApp", this);
        player = spawn("player", playerData);
        System.out.println("Player spawned - player is now: " + (player == null ? "null" : "initialized"));

        getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2, getAppHeight() / 2);
        getGameScene().getViewport().setBounds(0, 0, worldWidth, worldHeight);

        // Load game music with the current volume settings
        loadGameMusic();

        isTimerRunning = true;

        // Initialize the wave manager and start it
        waveManager = WaveManager.getInstance();
        waveManager.start(player);
        System.out.println("WaveManager initialized and started in initGame");

        // Add listener for EXP updates
        getWorldProperties().intProperty("exp").addListener((obs, oldValue, newValue) -> {
            updateExpBar();
            System.out.println("EXP property changed: old=" + oldValue + ", new=" + newValue);
        });

        resetTimers();
    }

    // Separate method for loading game music to improve code organization
    private void loadGameMusic() {
        try {
            java.net.URL musicUrl = getClass().getResource("/assets/music/game_music.mp3");
            if (musicUrl == null) {
                throw new IllegalStateException("Game music file not found at /assets/music/game_music.mp3.");
            }
            
            Media musicMedia = new Media(musicUrl.toExternalForm());
            gameMusicPlayer = new MediaPlayer(musicMedia);
            gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            // Use the current global music volume
            double volume = FXGL.getSettings().getGlobalMusicVolume();
            gameMusicPlayer.setVolume(volume);
            gameMusicPlayer.play();
            
            // Listen for changes to the global music volume
            FXGL.getSettings().globalMusicVolumeProperty().addListener((obs, oldVal, newVal) -> {
                if (gameMusicPlayer != null) {
                    gameMusicPlayer.setVolume(newVal.doubleValue());
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error loading game music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopTimer() {
        isTimerRunning = false;
        if (waveManager != null) {
            waveManager.stop();
            System.out.println("WaveManager stopped in stopTimer");
        }
        // Do not stop music here; it stops when Back to Main Menu is clicked
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
                java.util.Date now = new java.util.Date();
                java.sql.Date sqlDate = new java.sql.Date(now.getTime());
                stmt.setDate(5, sqlDate);
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
                int newRank = 1;
                String rankQuery = "SELECT COALESCE(MAX(rank), 0) + 1 AS new_rank FROM leaderboard";
                try (PreparedStatement rankStmt = connection.prepareStatement(rankQuery)) {
                    ResultSet rankResult = rankStmt.executeQuery();
                    if (rankResult.next()) {
                        newRank = rankResult.getInt("new_rank");
                    }
                }
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

    private void showGameOverScreen() {
        FXGL.getGameController().pauseEngine();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GameOverScene.fxml"));
            GameOverController controller = new GameOverController();
            controller.setGameApp(this);
            loader.setController(controller);
            javafx.scene.Parent root = loader.load();

            // Pass game stats to controller
            controller.setStats(
                    FXGL.getWorldProperties().getInt("survivalTime"),
                    FXGL.getWorldProperties().getInt("totalDamage"),
                    FXGL.getWorldProperties().getInt("kills")
            );

            // Set callback for back-to-menu action
            controller.setBackToMenuCallback(() -> {
                FXGL.getGameScene().clearUINodes();
                FXGL.getGameController().resumeEngine();
                if (gameMusicPlayer != null) {
                    gameMusicPlayer.stop();
                    gameMusicPlayer.dispose();
                    gameMusicPlayer = null;
                }
                gotoNewMainMenu();
            });

            // Add root to game scene
            FXGL.getGameScene().addUINode(root);
        } catch (Exception e) {
            System.err.println("Error loading GameOverScene.fxml: " + e.getMessage());
            e.printStackTrace();
            // Fallback to basic text if FXML fails
            Text gameOverText = FXGL.getUIFactoryService().newText("Game Over - Error Loading UI", Color.RED, 24);
            gameOverText.setFont(Font.font("Pixelify Sans", javafx.scene.text.FontWeight.BOLD, 24));
            gameOverText.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
            gameOverText.setTranslateY(FXGL.getAppHeight() / 2.0);
            getGameScene().addUINode(gameOverText);
        }
    }

    public void resetTimers() {
        System.out.println("Resetting game timers to prevent speed-up");
        isTimerRunning = false;

        // Recreate survival time timer
        FXGL.getGameTimer().runAtInterval(() -> {
            if (isTimerRunning) {
                int currentTime = getWorldProperties().getInt("survivalTime");
                getWorldProperties().setValue("survivalTime", currentTime + 1);
            }
        }, Duration.seconds(1));

        // Recreate weapon timers
        if (userType.equals("Gun")) {
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            int weaponLevel = playerComponent.getWeaponLevel("gun");
            double cooldown = 0.75; // Base cooldown at level 1
            if (weaponLevel >= 4) cooldown = 0.50; // Level 4: Reduced cooldown
            if (weaponLevel >= 7) cooldown = 0.30; // Level 7: Further reduced cooldown
            
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    playerComponent.shootTripleBurst();
                }
            }, Duration.seconds(cooldown));
        } else if (userType.equals("Sword")) {
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    player.getComponent(PlayerComponent.class).swordSlash();
                }
            }, Duration.seconds(0.5));
        } else if (userType.equals("Laser")) {
            FXGL.runOnce(() -> {
                player.getComponent(PlayerComponent.class).shootLaser();
            }, Duration.seconds(0.2));

            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    player.getComponent(PlayerComponent.class).shootLaser();
                }
            }, Duration.seconds(.5));
        } else {
            FXGL.getGameTimer().runAtInterval(() -> {
                if (isTimerRunning) {
                    player.getComponent(PlayerComponent.class).shootVoltChain();
                }
            }, Duration.seconds(.5));
        }

        // CHANGED: Ensure WaveManager timers are preserved and restarted
        if (waveManager == null) {
            waveManager = WaveManager.getInstance();
            System.out.println("WaveManager initialized in resetTimers");
        }

        if (!waveManager.isActive() && player != null) {
            waveManager.start(player);
            System.out.println("WaveManager restarted in resetTimers");
        } else {
            System.out.println("WaveManager already active or player null, skipping restart");
        }

        // Reinitialize player powerup timers
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).reinitializeAfterPause();
        }
        isTimerRunning = true;
        System.out.println("All game timers reset successfully");
    }

    private void spawnEnemyOutsideViewport(String enemyType) {
        double viewMinX = getGameScene().getViewport().getX();
        double viewMinY = getGameScene().getViewport().getY();
        double viewMaxX = viewMinX + getAppWidth();
        double viewMaxY = viewMinY + getAppHeight();
        double x, y;
        int margin = 50;
        int side = random.nextInt(4);
        switch (side) {
            case 0:
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMinY - margin;
                break;
            case 1:
                x = viewMaxX + margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
            case 2:
                x = viewMinX + random.nextDouble() * getAppWidth();
                y = viewMaxY + margin;
                break;
            case 3:
            default:
                x = viewMinX - margin;
                y = viewMinY + random.nextDouble() * getAppHeight();
                break;
        }
        if (random.nextDouble() < 0.2) {
            x = viewMinX + (random.nextBoolean() ? -margin : getAppWidth() + margin);
            y = viewMinY + (random.nextBoolean() ? -margin : getAppHeight() + margin);
        }
        SpawnData data = new SpawnData(x, y);
        data.put("player", player);
        FXGL.getGameWorld().spawn(enemyType, data);
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            BulletComponent bulletComponent = bullet.getComponent(BulletComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            int damage = bulletComponent.getDamage();
            enemyComponent.damage(damage, bullet.getPosition());
            FXGL.getWorldProperties().increment("totalDamage", damage);
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1);
                playerComponent.addExp(10); // Add 10 EXP per kill
            }
            
            // Handle piercing
            if (bulletComponent.canPierce()) {
                bulletComponent.incrementEnemiesHit();
            } else {
                bullet.removeFromWorld();
            }
        });

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

        onCollisionBegin(EntityType.SLASH, EntityType.ENEMY, (slash, enemy) -> {
            SwordComponent swordComponent = slash.getComponent(SwordComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            int damage = swordComponent.getDamage();
            enemyComponent.damage(damage, slash.getPosition());
            FXGL.getWorldProperties().increment("totalDamage", damage);
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1);
                player.getComponent(PlayerComponent.class).addExp(10); // Add 10 EXP per kill
            }
        });

        onCollisionBegin(EntityType.LASER, EntityType.ENEMY, (laser, enemy) -> {
            LaserComponent laserComponent = laser.getComponent(LaserComponent.class);
            EnemyComponent enemyComponent = enemy.getComponent(EnemyComponent.class);
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            int damage = laserComponent.getDamage();
            laserComponent.setEnemiesHit();
            enemyComponent.damage(damage, laser.getPosition());
            FXGL.getWorldProperties().increment("totalDamage", damage);
            if (enemyComponent.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1);
                playerComponent.addExp(10); // Add 10 EXP per kill
            }
        });

        onCollisionBegin(EntityType.VOLT_CHAIN, EntityType.ENEMY, (chain, enemy) -> {
            VoltChainComponent chainComp = chain.getComponent(VoltChainComponent.class);
            EnemyComponent enemyComp = enemy.getComponent(EnemyComponent.class);
            enemyComp.damage(10, chain.getPosition());
            chainComp.onHitEnemy(enemy);
            if (enemyComp.getHealth() <= 0) {
                FXGL.getWorldProperties().increment("kills", 1);
                player.getComponent(PlayerComponent.class).addExp(10); // Add 10 EXP per kill
            }
            chain.removeFromWorld();
        });

        System.out.println("initPhysics completed");
    }

    public void resetGameState() {
        System.out.println("resetGameState called");
        getWorldProperties().setValue("survivalTime", 0);
        getWorldProperties().setValue("health", 100);
        getWorldProperties().setValue("score", 0);
        getWorldProperties().setValue("level", 1);
        getWorldProperties().setValue("exp", 0);
        getWorldProperties().setValue("totalDamage", 0);
        getWorldProperties().setValue("kills", 0);
        getWorldProperties().setValue("wave", 1); // Reset wave property
        isTimerRunning = true;
        player = null;
        hasUpdatedExpBar = false;
        System.out.println("Game state reset for new session - player set to null");
        if (waveManager != null) {
            waveManager.reset();
            System.out.println("WaveManager reset in resetGameState");
        }
        System.out.println("Game state reset complete");
    }

    public class MainMenuController {
        private boolean isLeaderboardOpen = false;

        public void showLeaderboard() {
            if (isLeaderboardOpen) {
                System.out.println("Leaderboard dialog already open - ignoring request");
                return;
            }
            System.out.println("showLeaderboard called");
            LeaderboardUI leaderboardUI = new LeaderboardUI(storedPlayerName);
            isLeaderboardOpen = true;
            FXGL.getDialogService().showBox("Leaderboard", leaderboardUI.getContainer(), leaderboardUI.getCloseButton());
            leaderboardUI.getCloseButton().setOnAction(e -> {
                isLeaderboardOpen = false;
                System.out.println("Leaderboard dialog closed");
            });
        }
    }

    public void pauseGameTimers() {
        isTimerRunning = false;
        if (waveManager != null) {
            waveManager.stop();
            System.out.println("WaveManager paused in pauseGameTimers");
        }
    }

    public void resumeGameTimers() {
        isTimerRunning = true;
        System.out.println("Game timers resumed in resumeGameTimers");
        resetTimers();
        if (waveManager != null && player != null) {
            waveManager.start(player);
            System.out.println("WaveManager resumed in resumeGameTimers");
        }
    }

    public static String getStoredPlayerName() {
        return storedPlayerName;
    }

    public static void main(String[] args) {
        launch(args);
    }
}