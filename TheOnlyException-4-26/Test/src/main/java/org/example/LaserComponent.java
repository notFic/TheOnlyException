package org.example;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

import java.util.Random;

public class LaserComponent extends Component {
    private Point2D direction;
    private final double speed = 5;
    private int damage;

    public LaserComponent() {
        Random rand = new Random();
        this.damage = 25;
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
}
