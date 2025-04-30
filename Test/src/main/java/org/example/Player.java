package org.example;

public class Player {
        private String username;
        private int bestSurvivalTime;
        private int rank;
        private int totalDamage; // Added field to store total damage

        public Player(String username, int bestSurvivalTime, int rank, int totalDamage) {
                this.username = username;
                this.bestSurvivalTime = bestSurvivalTime;
                this.rank = rank;
                this.totalDamage = totalDamage; // Initialize totalDamage in constructor
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

        public int getTotalDamage() {
                return totalDamage; // Return the total damage value
        }

        // Setter for totalDamage, useful when populating from database
        public void setTotalDamage(int totalDamage) {
                this.totalDamage = totalDamage;
        }
}