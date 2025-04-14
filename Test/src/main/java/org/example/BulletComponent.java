package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

import java.util.Random;

public class BulletComponent extends Component {
    private Point2D direction;
    private final double speed = 5;
    private int damage;

    public BulletComponent() {
        Random rand = new Random();
        this.damage = 22 + rand.nextInt(6);
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
