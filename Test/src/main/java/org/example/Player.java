package org.example;

public class Player {
    private String username;
    private int bestSurvivalTime;
    private int rank;

    public Player(String username, int bestSurvivalTime, int rank) {
        this.username = username;
        this.bestSurvivalTime = bestSurvivalTime;
        this.rank = rank;
    }

    public String getUsername() {
        return username;
    }

    public int getBestSurvivalTime() {
        return bestSurvivalTime;
    }

    public int getRank() {
        return rank;
    }
}
