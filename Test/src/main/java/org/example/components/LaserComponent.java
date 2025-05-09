package org.example;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

import java.util.Random;

public class LaserComponent extends Component {
    private Point2D direction;
    private final double speed = 20;
    private int enemiesHit = 0;
    private int damage;

    public LaserComponent() {
        Random rand = new Random();
        this.damage = 25;
    }

    public void setEnemiesHit() {
        enemiesHit++;
        if (enemiesHit > 4) {
            enemiesHit = 4;
        }
        setDamage();
    }

    public void setDirection(Point2D direction) {
        this.direction = direction.normalize().multiply(speed);
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translate(direction);
        updateOpacity(); // Call method to update opacity
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage() {
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
        } else if (damage <= 5) { // Changed to <= to include 5 and below
            opacity = 0.20;
        }
        entity.setOpacity(opacity); // Set the opacity of the entity.
    }
}
