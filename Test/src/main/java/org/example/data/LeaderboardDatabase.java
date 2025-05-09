package org.example.data;

import org.example.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardDatabase {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbtheonlyexception";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public static List<Player> getTopPlayers(int limit) {
        List<Player> players = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String query = "SELECT l.leaderboard_id, l.player_id, p.username, l.best_survival_time, l.total_dmg_inflicted, l.rank " +
                    "FROM leaderboard l JOIN player p ON l.player_id = p.player_id " +
                    "ORDER BY l.rank ASC LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, limit);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    int bestSurvivalTime = resultSet.getInt("best_survival_time");
                    int totalDamage = resultSet.getInt("total_dmg_inflicted");
                    int rank = resultSet.getInt("rank");
                    // Updated constructor call to include totalDamage
                    Player player = new Player(username, bestSurvivalTime, rank, totalDamage);
                    players.add(player);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error while fetching leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
        return players;
    }
}