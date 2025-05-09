package org.example.model;

import javafx.scene.paint.Color;

public class UpgradeOption {
    private final String id;
    private final String name;
    private String description;  // Changed from final to allow updating
    private final Color color;
    private final OptionType type;

    public UpgradeOption(String id, String name, String description, Color color, OptionType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.type = type;               // WEAPON OR POWERUP
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    
    // Add a setter for description
    public void setDescription(String description) {
        this.description = description;
    }

    public Color getColor() {
        return color;
    }

    public OptionType getType() {
        return type;
    }
} 