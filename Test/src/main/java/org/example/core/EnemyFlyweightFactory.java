package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements the Flyweight pattern for enemy components to reduce memory usage
 * and improve performance with many enemies on screen.
 */
public class EnemyFlyweightFactory {
    private static EnemyFlyweightFactory instance;
    
    // Cache for animation channels (shared between instances)
    private final Map<String, AnimationChannel> animWalkLeftCache = new HashMap<>();
    private final Map<String, AnimationChannel> animWalkRightCache = new HashMap<>();
    
    private EnemyFlyweightFactory() {
        initializeAnimations();
    }
    
    /**
     * Get the singleton instance of the factory
     */
    public static EnemyFlyweightFactory getInstance() {
        if (instance == null) {
            instance = new EnemyFlyweightFactory();
        }
        return instance;
    }
    
    /**
     * Pre-load all animation channels to be shared across enemy instances
     */
    private void initializeAnimations() {
        // Maggot animations
        animWalkLeftCache.put("maggot", new AnimationChannel(FXGL.image("MaggotWalk-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.8), 4, 7));
        animWalkRightCache.put("maggot", new AnimationChannel(FXGL.image("MaggotWalk-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.8), 8, 11));
        
        // Beetle animations
        animWalkLeftCache.put("beetle", new AnimationChannel(FXGL.image("BeetleMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.4), 4, 7));
        animWalkRightCache.put("beetle", new AnimationChannel(FXGL.image("BeetleMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.4), 8, 11));
        
        // Mantis animations
        animWalkLeftCache.put("mantis", new AnimationChannel(FXGL.image("MantisMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.8), 8, 11));
        animWalkRightCache.put("mantis", new AnimationChannel(FXGL.image("MantisMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.8), 4, 7));
        
        // Bee animations
        animWalkLeftCache.put("bee", new AnimationChannel(FXGL.image("BeeMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.4), 4, 7));
        animWalkRightCache.put("bee", new AnimationChannel(FXGL.image("BeeMove-scaled-recolored.png"), 4,
                64, 64, Duration.seconds(0.4), 8, 11));
        
        // Giant Fly animations
        animWalkLeftCache.put("giantfly", new AnimationChannel(FXGL.image("GiantFly-recolored.png"), 4,
                64, 64, Duration.seconds(0.6), 4, 7));
        animWalkRightCache.put("giantfly", new AnimationChannel(FXGL.image("GiantFly-recolored.png"), 4,
                64, 64, Duration.seconds(0.6), 8, 11));
        
        // Dragonfly animations
        animWalkLeftCache.put("dragonfly", new AnimationChannel(FXGL.image("DragonFly-recolored.png"), 4,
                64, 64, Duration.seconds(0.6), 4, 7));
        animWalkRightCache.put("dragonfly", new AnimationChannel(FXGL.image("DragonFly-recolored.png"), 4,
                64, 64, Duration.seconds(0.6), 8, 11));
    }
    
    /**
     * Get the animation channel for walking left for the specified enemy type
     */
    public AnimationChannel getAnimWalkLeft(String enemyType) {
        return animWalkLeftCache.get(enemyType);
    }
    
    /**
     * Get the animation channel for walking right for the specified enemy type
     */
    public AnimationChannel getAnimWalkRight(String enemyType) {
        return animWalkRightCache.get(enemyType);
    }
    
    /**
     * Create a new animated texture for the specified enemy type
     */
    public AnimatedTexture createAnimatedTexture(String enemyType) {
        AnimationChannel initialChannel = animWalkRightCache.get(enemyType);
        if (initialChannel == null) {
            throw new IllegalArgumentException("No animation found for enemy type: " + enemyType);
        }
        
        AnimatedTexture texture = new AnimatedTexture(initialChannel);
        texture.loop();
        
        return texture;
    }
    
    /**
     * Apply position and scale adjustments for the specified enemy type
     */
    public void applyViewAdjustments(String enemyType, AnimatedTexture texture) {
        switch (enemyType) {
            case "maggot":
                texture.setTranslateX(-10);
                texture.setTranslateY(-40);
                break;
            case "beetle":
                texture.setTranslateX(-10);
                texture.setTranslateY(-25);
                break;
            case "mantis":
                texture.setTranslateX(-10);
                texture.setTranslateY(-13);
                break;
            case "bee":
                texture.setScaleX(2.0);
                texture.setScaleY(2.0);
                texture.setTranslateX(-15);
                texture.setTranslateY(-25);
                break;
            case "giantfly":
                texture.setTranslateX(0);
                texture.setTranslateY(5);
                texture.setScaleX(2.0);
                texture.setScaleY(2.0);
                break;
            case "dragonfly":
                texture.setTranslateX(-10);
                texture.setTranslateY(-28);
                texture.setScaleX(1.3);
                texture.setScaleY(1.3);
                break;
        }
    }
} 