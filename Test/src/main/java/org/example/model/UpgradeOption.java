package org.example.model;

import javafx.scene.paint.Color;

public class UpgradeOption {
    private final String id;
    private final String name;
    private final String description;
    private final Color color;
    private final OptionType type;
    private final String imagePath; // New field for image path

    public UpgradeOption(String id, String name, String description, Color color, OptionType type, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.type = type;
        this.imagePath = imagePath;
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

    public Color getColor() {
        return color;
    }

    public OptionType getType() {
        return type;
    }

    public String getImagePath() {
        return imagePath;
    }
}