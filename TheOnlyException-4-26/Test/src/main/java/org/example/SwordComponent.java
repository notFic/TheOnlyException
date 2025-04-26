package org.example;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;

import java.util.Random;

public class SwordComponent extends Component {
    private Point2D direction;
    private int damage;

    public SwordComponent(Point2D direction) {
        this.direction = direction.normalize();
        Random rand = new Random();
        this.damage = 22 + rand.nextInt(6);
    }

    public int getDamage() {
        damage = 100;
        return damage;
    }
}
