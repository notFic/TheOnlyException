package org.example.utils;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbtheonlyexception";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static Connection getConnection() throws SQLException {
        System.out.println("Attempting to connect to database: " + DB_URL);
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static boolean userExists(String username) throws SQLException {
        System.out.println("Checking if user exists: " + username);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            boolean exists = resultSet.next();
            System.out.println("User exists check result: " + exists);
            return exists;
        }
    }

    public static boolean validateUser(String username, String password) throws SQLException {
        System.out.println("Validating user: " + username);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT password FROM users WHERE username = ?")) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String dbPassword = resultSet.getString("password");
                boolean valid = dbPassword.equals(password); // Temporarily use plain text match
                System.out.println("User validation result: " + valid);
                return valid;
            }
            System.out.println("User not found in database");
            return false;
        }
    }

    public static boolean registerUser(String username, String password) throws SQLException {
        System.out.println("Registering user: " + username);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, password); // Temporarily use plain text
            int rowsAffected = statement.executeUpdate();
            System.out.println("Registration result: " + (rowsAffected > 0));
            return rowsAffected > 0;
        }
    }

    // Optional: Method to link a user to the player table after registration
    public static int createPlayerRecord(String username) throws SQLException {
        System.out.println("Creating player record for: " + username);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO player (username, created_at) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setDate(2, new Date(System.currentTimeMillis()));
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Return player_id
                }
            }
            return -1;
        }
    }
}