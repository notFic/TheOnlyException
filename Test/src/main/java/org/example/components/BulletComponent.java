package org.example.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

public class BulletComponent extends Component {
    private Point2D direction;
    private final double speed = 5;
    private int damage;
    private int pierceCount = 0;
    private int enemiesHit = 0;

    public BulletComponent() {
        this.damage = 10; // Base damage at level 1
    }

    public void setDirection(Point2D direction) {
        this.direction = direction.normalize().multiply(speed);
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translate(direction);
    }

    public int getDamage() {
        return damage;
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

    public void setDamage(int damage) {
        this.damage = damage;
    }
}