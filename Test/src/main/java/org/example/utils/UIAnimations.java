package org.example.utils;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class UIAnimations {

    public static void fadeIn(Node node, int durationMs) {
        if (node == null) {
            System.out.println("Warning: Attempted to fade in a null node");
            return;
        }
        FadeTransition fadeIn = new FadeTransition(Duration.millis(durationMs), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public static void shakeNode(Node node) {
        if (node == null) {
            System.out.println("Warning: Attempted to shake a null node");
            return;
        }
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(5);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    public static void pulseNode(Node node) {
        if (node == null) {
            System.out.println("Warning: Attempted to pulse a null node");
            return;
        }
        FadeTransition pulse = new FadeTransition(Duration.millis(1000), node);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.8);
        pulse.setCycleCount(TranslateTransition.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }
}