package org.example;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class NameInputScene extends FXGLMenu {

    private TextField nameField;

    public NameInputScene() {
        super(MenuType.MAIN_MENU);

        // Create a semi-transparent overlay for better text visibility
        Rectangle overlay = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight(), Color.color(0, 0, 0, 0.5));
        getContentRoot().getChildren().add(overlay);

        // Create a title label
        Label titleLabel = new Label("Enter Your Name");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        // Create a text field for name input
        nameField = new TextField();
        nameField.setMaxWidth(200);
        nameField.setPromptText("Your Name");


        // Create a start button
        Button startButton = new Button("Start Game");
        startButton.setPrefWidth(120);
        startButton.setOnAction(e -> {
            if (!nameField.getText().trim().isEmpty()) {
                // Store the name in world properties
                String playerName = nameField.getText().trim();

                // Make sure we're setting it correctly
                FXGL.getWorldProperties().setValue("playerName", playerName);

                // For debugging
                System.out.println("Setting player name to: " + playerName);

                // Call the static method to ensure the name is set before game starts
                GameApp.startGameWithName(playerName);

                // Start the actual game
                fireNewGame();
            }
        });

        // Create a container for all elements
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.getChildren().addAll(titleLabel, nameField, startButton);

        // Make the container center of the screen
        container.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
        container.setTranslateY(FXGL.getAppHeight() / 2.0 - 100);

        getContentRoot().getChildren().add(container);
    }
}
