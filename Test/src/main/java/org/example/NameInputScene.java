package org.example;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.sql.*;

// Main menu scene for user login and registration
public class NameInputScene extends FXGLMenu {

    private TextField userField; // Username input field
    private PasswordField passField; // Password input field
    private Label errorLabel; // Displays error messages

    // Initialize the login menu UI
    public NameInputScene() {
        super(MenuType.MAIN_MENU);

        // Add semi-transparent background overlay
        Rectangle overlay = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight(), Color.color(0, 0, 0, 0.5));
        getContentRoot().getChildren().add(overlay);

        // Create title
        Label titleLabel = new Label("Login");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // Username input
        Label userLabel = new Label("Username");
        userLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        userLabel.setTextFill(Color.WHITE);
        userField = new TextField();
        userField.setMaxWidth(200);
        userField.setPromptText("Enter username");

        // Password input
        Label passLabel = new Label("Password");
        passLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        passLabel.setTextFill(Color.WHITE);
        passField = new PasswordField();
        passField.setMaxWidth(200);
        passField.setPromptText("Enter password");

        // Error message display
        errorLabel = new Label("");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Login and register buttons
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(120);
        loginButton.setOnAction(e -> handleLogin());
        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(120);
        registerButton.setOnAction(e -> showRegistrationForm());

        // Button container
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(loginButton, registerButton);

        // Main UI container
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.getChildren().addAll(titleLabel, userLabel, userField, passLabel, passField, errorLabel, buttonBox);
        container.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
        container.setTranslateY(FXGL.getAppHeight() / 2.0 - 150);

        getContentRoot().getChildren().add(container);
    }

    // Handle user login with database validation
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
            System.out.println("Database connection established successfully");

            String query = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, user);
                System.out.println("Executing query: " + query + " with username: " + user);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String dbPassword = resultSet.getString("password");
                    if (dbPassword.equals(pass)) {
                        FXGL.getWorldProperties().setValue("playerName", user);
                        System.out.println("Login successful for user: " + user);
                        GameApp.startGameWithName(user);
                        fireNewGame(); // Start the game
                    } else {
                        errorLabel.setText("Incorrect password. Please try again.");
                        System.out.println("Login failed: Incorrect password for user: " + user);
                    }
                } else {
                    errorLabel.setText("Username not found. Please try again.");
                    System.out.println("Login failed: Username not found: " + user);
                    listAllUsers(connection); // Debug: list users
                }
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error. Please try again later.");
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Debug: List all usernames in the database
    private void listAllUsers(Connection connection) {
        try {
            String query = "SELECT username FROM users";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                System.out.println("--- All users in database ---");
                boolean anyUsers = false;
                while (resultSet.next()) {
                    anyUsers = true;
                    System.out.println("User: " + resultSet.getString("username"));
                }
                if (!anyUsers) {
                    System.out.println("No users found in database!");
                }
                System.out.println("----------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error listing users: " + e.getMessage());
        }
    }

    // Show registration form for new users
    private void showRegistrationForm() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Account");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        VBox registerForm = new VBox(10);
        registerForm.setAlignment(Pos.CENTER);

        TextField newUserField = new TextField();
        newUserField.setPromptText("Username");
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Password");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm Password");
        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.RED);

        registerForm.getChildren().addAll(
                new Label("Username:"), newUserField,
                new Label("Password:"), newPassField,
                new Label("Confirm Password:"), confirmPassField,
                statusLabel
        );

        dialog.getDialogPane().setContent(registerForm);
        newUserField.requestFocus();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                String newUser = newUserField.getText().trim();
                String newPass = newPassField.getText().trim();
                String confirmPass = confirmPassField.getText().trim();

                if (newUser.isEmpty() || newPass.isEmpty()) {
                    statusLabel.setText("Username and password cannot be empty");
                    return null;
                }

                if (!newPass.equals(confirmPass)) {
                    statusLabel.setText("Passwords do not match");
                    return null;
                }

                Integer userId = registerNewUser(newUser, newPass);
                if (userId != null) {
                    userField.setText(newUser);
                    passField.setText(newPass);
                    errorLabel.setText("Registration successful! Logging in...");
                    handleLogin();
                    return ButtonType.OK;
                } else {
                    statusLabel.setText(errorLabel.getText());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // Register a new user in the database
    private Integer registerNewUser(String username, String password) {
        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            // Check if username exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                ResultSet resultSet = checkStmt.executeQuery();
                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    errorLabel.setText("Username already exists. Please choose another.");
                    return null;
                }
            }

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected > 0) {
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        errorLabel.setText("Registration successful! You can now login.");
                        return generatedKeys.getInt(1);
                    }
                }
                errorLabel.setText("Failed to register user. Please try again.");
                return null;
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error during registration.");
            System.err.println("Database error during registration: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}