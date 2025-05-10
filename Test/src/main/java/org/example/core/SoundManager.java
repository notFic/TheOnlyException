package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.media.AudioClip;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage sound effects across the game.
 */
public class SoundManager {
    private static SoundManager instance;
    private Map<String, AudioClip> soundEffects;

    private SoundManager() {
        soundEffects = new HashMap<>();
        loadSoundEffects();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void loadSoundEffects() {
        System.out.println("Loading sound effects...");

        // Load common sound effects
        loadSound("button_click", "/assets/sounds/button_click.wav");
        loadSound("level_up", "/assets/sounds/level_up.mp3");
        loadSound("error", "/assets/sounds/error.mp3");
        loadSound("shoot", "/assets/sounds/SHOOT.mp3"); // Added gunshot sound
        loadSound("explosion", "/assets/sounds/EXPLOSION.mp3"); // Added gunshot sound

        // If sound files don't exist yet, create a default one
        if (soundEffects.isEmpty()) {
            createDefaultSound("button_click");
        }
    }

    private void loadSound(String name, String path) {
        try {
            java.net.URL soundUrl = getClass().getResource(path);
            if (soundUrl != null) {
                AudioClip clip = new AudioClip(soundUrl.toExternalForm());
                soundEffects.put(name, clip);
                System.out.println("Loaded sound effect: " + name);
            } else {
                System.err.println("Sound effect not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading sound effect " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultSound(String name) {
        // Create a default sound if no sounds are available
        // This is a temporary solution until proper sound files are added
        try {
            // Try to use any available sound from the game resources
            java.net.URL musicUrl = getClass().getResource("/assets/music/music2.mp3");
            if (musicUrl != null) {
                AudioClip clip = new AudioClip(musicUrl.toExternalForm());
                soundEffects.put(name, clip);
                System.out.println("Created default sound for: " + name);
            } else {
                System.err.println("Could not create default sound, no resources available");
            }
        } catch (Exception e) {
            System.err.println("Error creating default sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void playSound(String name) {
        AudioClip clip = soundEffects.get(name);
        if (clip != null) {
            // Use the global sound volume setting
            clip.setVolume(FXGL.getSettings().getGlobalSoundVolume());
            clip.play();
            System.out.println("Playing sound effect: " + name);
        } else {
            System.err.println("Sound effect not found: " + name);
        }
    }

    public void stopSound(String name) {
        AudioClip clip = soundEffects.get(name);
        if (clip != null) {
            clip.stop();
        }
    }

    public void stopAllSounds() {
        for (AudioClip clip : soundEffects.values()) {
            clip.stop();
        }
    }
}