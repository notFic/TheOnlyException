package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/dbtheonlyexception";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Method to get the top N players from the game_session table
    public static List<Player> getTopPlayers(int limit) {
        List<Player> players = new ArrayList<>();
        String query = "SELECT u.username, MAX(gs.survival_time) as best_survival_time, " +
                "RANK() OVER (ORDER BY MAX(gs.survival_time) DESC) as rank " +
                "FROM users u " +
                "JOIN game_session gs ON u.id = gs.player_id " +
                "GROUP BY u.id, u.username " +
                "ORDER BY best_survival_time DESC " +
                "LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                int bestSurvivalTime = rs.getInt("best_survival_time");
                int rank = rs.getInt("rank");
                players.add(new Player(username, bestSurvivalTime, rank));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    // Method to insert a new session into the game_session table
    public static void insertGameSession(int playerId, int survivalTime, int totalDamage, int kills) {
        String query = "INSERT INTO game_session (player_id, survival_time, total_dmg_inflicted, kills, session_date) " +
                "VALUES (?, ?, ?, ?, NOW())";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, survivalTime);
            stmt.setInt(3, totalDamage);
            stmt.setInt(4, kills);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}