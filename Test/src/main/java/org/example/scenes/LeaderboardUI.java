package org.example.scenes;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.example.data.LeaderboardDatabase;
import org.example.model.Player;

import java.util.List;

public class LeaderboardUI {

    private TableView<Player> tableView;
    private Button closeButton;
    private StackPane rootPane;
    private Label title;
    private VBox contentBox;
    private String currentPlayerUsername;

    public LeaderboardUI(String currentPlayerUsername) {
        this.currentPlayerUsername = currentPlayerUsername;

        title = new Label("Top 10 Players");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-effect: dropshadow(gaussian, #18b7e7, 15, 0.7, 0, 0);");

        tableView = new TableView<>();
        tableView.setPrefSize(600, 400);
        tableView.setStyle("-fx-background-color: transparent; -fx-border-color: #177bdf; -fx-border-width: 1;");
        tableView.getStylesheets().add(getClass().getResource("/css/leaderboard.css").toExternalForm());

        TableColumn<Player, Number> rankColumn = new TableColumn<>("Rank");
        rankColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getRank()));
        rankColumn.setPrefWidth(90);

        TableColumn<Player, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getUsername()));
        usernameColumn.setPrefWidth(200);

        TableColumn<Player, Number> survivalTimeColumn = new TableColumn<>("Survival Time (s)");
        survivalTimeColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getBestSurvivalTime()));
        survivalTimeColumn.setPrefWidth(150);

        TableColumn<Player, Number> damageColumn = new TableColumn<>("Total Damage");
        damageColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getTotalDamage()));
        damageColumn.setPrefWidth(155);

        tableView.getColumns().addAll(rankColumn, usernameColumn, survivalTimeColumn, damageColumn);

        tableView.setRowFactory(tv -> {
            TableRow<Player> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    String username = newItem.getUsername();
                    if (username != null && username.equals(currentPlayerUsername)) {
                        row.setStyle("-fx-background-color: rgba(0, 255, 0, 0.3); -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial'; -fx-effect: dropshadow(gaussian, #00FF00, 10, 0.5, 0, 0);");
                    } else {
                        row.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial';");
                    }
                }
            });

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    Player player = row.getItem();
                    String username = player.getUsername();
                    if (username != null && username.equals(currentPlayerUsername)) {
                        row.setStyle("-fx-background-color: rgba(0, 255, 0, 0.4); -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial'; -fx-effect: dropshadow(gaussian, #00FF00, 12, 0.6, 0, 0);");
                    } else {
                        row.setStyle("-fx-background-color: rgba(24, 183, 231, 0.3); -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial'; -fx-effect: dropshadow(gaussian, #18b7e7, 10, 0.5, 0, 0);");
                    }
                }
            });

            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    Player player = row.getItem();
                    String username = player.getUsername();
                    if (username != null && username.equals(currentPlayerUsername)) {
                        row.setStyle("-fx-background-color: rgba(0, 255, 0, 0.3); -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial'; -fx-effect: dropshadow(gaussian, #00FF00, 10, 0.5, 0, 0);");
                    } else {
                        row.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-font-family: 'Arial';");
                    }
                }
            });
            return row;
        });

        closeButton = new Button("Close");
        closeButton.setStyle("-fx-font-size: 16; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-border-radius: 5; -fx-effect: dropshadow(gaussian, #18b7e7, 10, 0.5, 0, 0);");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-font-size: 16; -fx-background-color: #18b7e7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-border-radius: 5; -fx-effect: dropshadow(gaussian, #18b7e7, 15, 0.7, 0, 0);"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-font-size: 16; -fx-background-color: #177bdf; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-border-radius: 5; -fx-effect: dropshadow(gaussian, #18b7e7, 10, 0.5, 0, 0);"));

        loadLeaderboardData();

        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(title, tableView, closeButton);

        DropShadow borderGlow = new DropShadow();
        borderGlow.setColor(Color.web("#18b7e7"));
        borderGlow.setRadius(10);
        tableView.setEffect(borderGlow);

        Timeline glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(borderGlow.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(1000), new KeyValue(borderGlow.radiusProperty(), 20)),
                new KeyFrame(Duration.millis(2000), new KeyValue(borderGlow.radiusProperty(), 10))
        );
        glowTimeline.setCycleCount(Timeline.INDEFINITE);
        glowTimeline.play();

        rootPane = new StackPane(contentBox);
        rootPane.setAlignment(Pos.CENTER);
        rootPane.setStyle("-fx-background-color: transparent;");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), contentBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void loadLeaderboardData() {
        List<Player> topPlayers = LeaderboardDatabase.getTopPlayers(10);
        tableView.getItems().clear();
        tableView.getItems().addAll(topPlayers);
    }

    public StackPane getContainer() {
        return rootPane;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public LeaderboardUI() {
        this(null);
    }
}