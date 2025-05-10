package org.example.core;

import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

/**
 * Utility class to add sound effects to buttons.
 */
public class ButtonSoundHelper {
    
    /**
     * Adds a button click sound effect to a button.
     * 
     * @param button The button to add the sound effect to
     */
    public static void addClickSound(Button button) {
        if (button == null) {
            System.err.println("Warning: Attempted to add sound to null button");
            return;
        }

        // Keep track of existing event handlers to avoid duplicates
        boolean hasClickSound = button.getProperties().containsKey("has_click_sound");
        
        if (!hasClickSound) {
            // Add the mouse click event handler
            button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                System.out.println("Button pressed: " + getButtonDescription(button));
                // Use direct playback method for more reliable sound
                SoundManager.playButtonSound();
            });
            
            // Mark the button as having a click sound
            button.getProperties().put("has_click_sound", true);
            System.out.println("Added click sound to button: " + getButtonDescription(button));
        } else {
            System.out.println("Button already has click sound: " + getButtonDescription(button));
        }
    }
    
    /**
     * Gets a friendly description of a button for logging
     */
    private static String getButtonDescription(Button button) {
        if (button.getText() != null && !button.getText().isEmpty()) {
            return "\"" + button.getText() + "\"";
        } else if (button.getId() != null && !button.getId().isEmpty()) {
            return "ID: " + button.getId();
        } else {
            return "unnamed button";
        }
    }
    
    /**
     * Adds button click sound effects to multiple buttons at once.
     * 
     * @param buttons The buttons to add sound effects to
     */
    public static void addClickSoundToAll(Button... buttons) {
        if (buttons == null) {
            System.err.println("Warning: Attempted to add sounds to null button array");
            return;
        }
        
        for (Button button : buttons) {
            addClickSound(button);
        }
        
        System.out.println("Added click sounds to " + buttons.length + " buttons");
    }
} 