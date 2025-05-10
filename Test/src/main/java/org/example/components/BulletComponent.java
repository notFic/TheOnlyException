package org.example.components;

import javafx.geometry.Point2D;

public class BulletComponent extends GameComponent {
    private Point2D direction;
    private int pierceCount = 0;
    private int enemiesHit = 0;

    public BulletComponent() {
        this.damage = 10; // Base damage at level 1
        this.speed = 5.0; // Set the speed inherited from GameComponent
    }

    @Override
    protected void initialize() {
        // Component-specific initialization
        // This replaces the need for custom onAdded method
    }

    @Override
    protected void updateComponent(double tpf) {
        // Move in the current direction
        if (direction != null) {
            entity.translate(direction);
        }
    }

    public void setDirection(Point2D direction) {
        this.direction = direction.normalize().multiply(speed);
    }

    public void setPierceCount(int count) {
        this.pierceCount = count;
    }

    public boolean canPierce() {
        return enemiesHit < pierceCount;
    }

    public void incrementEnemiesHit() {
        enemiesHit++;
    }
}