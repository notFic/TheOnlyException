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

public class NameInputScene extends FXGLMenu {

    private TextField userField;
    private PasswordField passField;
    private Label errorLabel; // Added for error messages

    public NameInputScene() {
        super(MenuType.MAIN_MENU);

        // Create a semi-transparent overlay for better text visibility
        Rectangle overlay = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight(), Color.color(0, 0, 0, 0.5));
        getContentRoot().getChildren().add(overlay);

        // Create a title label
        Label titleLabel = new Label("Login");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        Label userLabel = new Label("Username");
        userLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        userLabel.setTextFill(Color.WHITE);

        userField = new TextField();
        userField.setMaxWidth(200);
        userField.setPromptText("Enter username");

        Label passLabel = new Label("Password");
        passLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        passLabel.setTextFill(Color.WHITE);

        passField = new PasswordField();
        passField.setMaxWidth(200);
        passField.setPromptText("Enter password");

        // Add error label for feedback
        errorLabel = new Label("");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Create login button
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(120);
        loginButton.setOnAction(e -> handleLogin());

        // Optional: Add a register button
        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(120);
        registerButton.setOnAction(e -> showRegistrationForm());

        // Create horizontal button layout
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(loginButton, registerButton);

        // Create a container for all elements
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.getChildren().addAll(
                titleLabel,
                userLabel, userField,
                passLabel, passField,
                errorLabel,
                buttonBox
        );

        // Make the container center of the screen
        container.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
        container.setTranslateY(FXGL.getAppHeight() / 2.0 - 150);

        getContentRoot().getChildren().add(container);
    }

    private void handleLogin() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Username or password cannot be empty!");
            return;
        }

        // Database connection details
        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            // Use prepared statement to prevent SQL injection
            String query = "SELECT * FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, user);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String dbPassword = resultSet.getString("password");

                if (dbPassword.equals(pass)) {
                    // Login successful
                    FXGL.getWorldProperties().setValue("playerName", user);
                    System.out.println("Login successful for user: " + user);
                    GameApp.startGameWithName(user);
                    fireNewGame();
                } else {
                    // Incorrect password
                    errorLabel.setText("Incorrect password. Please try again.");
                    System.out.println("Login failed: Incorrect password for user: " + user);
                }
            } else {
                // Username not found
                errorLabel.setText("Username not found. Please try again.");
                System.out.println("Login failed: Username not found: " + user);
            }

        } catch (SQLException e) {
            errorLabel.setText("Database error. Please try again later.");
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showRegistrationForm() {
        // Create registration dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Account");

        // Set the button types
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        // Create the registration form
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

        // Request focus on the username field by default
        newUserField.requestFocus();

        // Convert the result to a registration when the register button is clicked
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

                // Register the new user
                if (registerNewUser(newUser, newPass)) {
                    return ButtonType.OK;
                } else {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private boolean registerNewUser(String username, String password) {
        // Database connection details
        String url = "jdbc:mysql://localhost:3306/dbtheonlyexception";
        String dbUser = "root";
        String dbPass = "";

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPass)) {
            // First check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet resultSet = checkStmt.executeQuery();

            if (resultSet.next() && resultSet.getInt(1) > 0) {
                errorLabel.setText("Username already exists. Please choose another.");
                return false;
            }

            // If username doesn't exist, insert the new user
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);

            int rowsAffected = insertStmt.executeUpdate();
            if (rowsAffected > 0) {
                errorLabel.setText("Registration successful! You can now login.");
                return true;
            } else {
                errorLabel.setText("Failed to register user. Please try again.");
                return false;
            }

        } catch (SQLException e) {
            errorLabel.setText("Database error during registration.");
            System.err.println("Database error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
