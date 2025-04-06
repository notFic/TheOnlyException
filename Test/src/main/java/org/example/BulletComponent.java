package org.example;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

public class BulletComponent extends Component {
    private Point2D direction;
    private final double speed = 5;

    public void setDirection(Point2D direction) {
        this.direction = direction.normalize().multiply(speed);
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translate(direction);

        // Remove bullet if it goes off-screen
        if (entity.getX() < 0 || entity.getX() > FXGL.getAppWidth() ||
                entity.getY() < 0 || entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }
}
