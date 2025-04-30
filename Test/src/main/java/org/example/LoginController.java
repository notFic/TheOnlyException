package org.example;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.sql.*;

public class LoginController {

    private Runnable loginSuccessCallback;

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private MediaView backgroundMediaView;

    private MediaPlayer mediaPlayer;

    @FXML
    private void initialize() {
        try {
            System.out.println("Attempting to load video from path: /assets/images/placeHolderVidBg.mp4");
            java.net.URL videoUrl = getClass().getResource("/assets/images/placeHolderVidBg.mp4");
            if (videoUrl == null) {
                throw new IllegalStateException("Video file not found at /assets/images/placeHolderVidBg.mp4. Ensure the file exists in src/main/resources/assets/videos/");
            }

            String videoPath = videoUrl.toExternalForm();
            System.out.println("Video path resolved to: " + videoPath);

            Media media = new Media(videoPath);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setMute(true);
            mediaPlayer.play();

            // Center the UI elements
            double paneWidth = 1280.0;
            double paneHeight = 720.0;
            double fieldWidth = 200.0;
            double fieldHeight = 30.0;
            double buttonWidth = 100.0;
            double spacing = 20.0;

            // Calculate the total height of the UI elements (4 elements with 3 gaps)
            double totalHeight = 4 * fieldHeight + 3 * spacing;
            double startY = (paneHeight - totalHeight) / 2;

            // Center errorLabel
            errorLabel.setLayoutX((paneWidth - fieldWidth) / 2); // 540.0
            errorLabel.setLayoutY(startY); // ~270.0

            // Center userField
            userField.setLayoutX((paneWidth - fieldWidth) / 2);
            userField.setLayoutY(startY + fieldHeight + spacing);

            // Center passField
            passField.setLayoutX((paneWidth - fieldWidth) / 2);
            passField.setLayoutY(userField.getLayoutY() + fieldHeight + spacing);

            // Center buttons (side by side)
            double buttonsTotalWidth = 2 * buttonWidth + 10; // 10 is spacing between buttons
            double buttonsStartX = (paneWidth - buttonsTotalWidth) / 2; // 535.0
            double buttonsY = passField.getLayoutY() + fieldHeight + spacing;

            loginButton.setLayoutX(buttonsStartX);
            loginButton.setLayoutY(buttonsY);

            registerButton.setLayoutX(buttonsStartX + buttonWidth + 10);
            registerButton.setLayoutY(buttonsY);

            mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("MediaPlayer status: " + newValue);
                if (newValue == MediaPlayer.Status.HALTED) {
                    System.out.println("MediaPlayer error: " + mediaPlayer.getError());
                }
            });

            System.out.println("Background video loaded and playing successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load background video: " + e.getMessage());
            backgroundMediaView.getParent().setStyle("-fx-background-color: black;");
        }
    }

    public void setLoginSuccessCallback(Runnable callback) {
        this.loginSuccessCallback = callback;
    }

    @FXML
    private void handleLogin() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Username or password cannot be empty!");
            return;
        }

        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            String query = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, user);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String dbPassword = resultSet.getString("password");
                    if (dbPassword.equals(pass)) {
                        FXGL.getWorldProperties().setValue("playerName", user);
                        GameApp.startGameWithName(user);
                        // Set login state
                        GameApp gameApp = (GameApp) FXGL.getAppCast();
                        gameApp.setLoggedIn(true);
                        System.out.println("Login successful for user: " + user + ". Transitioning to MainMenuScene.");
                        // Stop the background video
                        if (mediaPlayer != null) {
                            mediaPlayer.stop();
                            System.out.println("LoginController background video stopped");
                        }
                        // Transition to the new MainMenuScene
                        gameApp.gotoNewMainMenu();
                        if (loginSuccessCallback != null) {
                            loginSuccessCallback.run();
                        }
                    } else {
                        errorLabel.setText("Incorrect password. Please try again.");
                    }
                } else {
                    errorLabel.setText("Username not found. Please try again.");
                }
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Username or password cannot be empty!");
            return;
        }

        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            String checkQuery = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, user);
                ResultSet resultSet = checkStmt.executeQuery();
                if (resultSet.next()) {
                    errorLabel.setText("Username already exists!");
                    return;
                }
            }

            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                insertStmt.setString(1, user);
                insertStmt.setString(2, pass);
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected > 0) {
                    errorLabel.setText("Registration successful! Please log in.");
                } else {
                    errorLabel.setText("Registration failed. Try again.");
                }
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}