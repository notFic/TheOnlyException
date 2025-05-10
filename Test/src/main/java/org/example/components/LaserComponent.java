package org.example.components;

import javafx.geometry.Point2D;

import java.util.Random;

public class LaserComponent extends GameComponent {
    private Point2D direction;
    private int enemiesHit = 0;

    public LaserComponent() {
        Random rand = new Random();
        this.damage = 25;
        this.speed = 20.0;
    }

    @Override
    protected void initialize() {
        // Component-specific initialization
    }

    @Override
    protected void updateComponent(double tpf) {
        if (direction != null) {
            entity.translate(direction);
            updateOpacity(); // Call method to update opacity
        }
    }

    public void setEnemiesHit() {
        enemiesHit++;
        if (enemiesHit > 4) {
            enemiesHit = 4;
        }
        updateDamageByHits();
    }

    public void setDirection(Point2D direction) {
        this.direction = direction.normalize().multiply(speed);
    }

    public void updateDamageByHits() {
        this.damage -= 5 * enemiesHit;
        if (damage < 5) {
            damage = 5;
        }
    }

    private void updateOpacity() {
        double opacity = 0.75; // Default
        if (damage == 20) {
            opacity = 0.75;
        } else if (damage == 15) {
            opacity = 0.50;
        } else if (damage == 13) {
            opacity = 0.35;
        } else if (damage <= 5) {
            opacity = 0.20;
        }
        entity.setOpacity(opacity); // Set the opacity of the entity.
    }
}
