package org.example.core;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage sound effects across the game.
 */
public class SoundManager {
    private static SoundManager instance;
    private Map<String, Object> soundEffects; // Can hold either AudioClip or MediaPlayer

    // Flag to check if sound effects are actually loaded
    private boolean soundsLoaded = false;

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

    /**
     * Direct method to play button click sound without requiring the whole sound system
     * This is a quick remedy for sound issues
     */
    public static void playButtonSound() {
        try {
            // Try to use the resource
            java.net.URL soundUrl = SoundManager.class.getResource("/assets/sounds/btn.mp3");
            
            if (soundUrl != null) {
                System.out.println("Sound resource found at: " + soundUrl);

                // Create Media and MediaPlayer directly
                Media media = new Media(soundUrl.toExternalForm());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(1.0); // Full volume for testing
                mediaPlayer.play();

                System.out.println("Playing direct sound from resource");

                // No need to clean up - the MediaPlayer will be garbage collected
            } else {
                System.err.println("Sound resource not found: /assets/sounds/btn.mp3");

                // Try built-in JavaFX alert sound
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            System.err.println("Error playing direct button sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSoundEffects() {
        System.out.println("Loading sound effects...");

        // Load common sound effects
        loadSound("button_click", "/assets/sounds/button_click.wav");
        loadSoundAsMedia("btn_click", "/assets/sounds/btn.mp3"); // Use Media for mp3 files
        //("level_up", "/assets/sounds/level_up.mp3");
        //loadSound("error", "/assets/sounds/error.mp3");
        loadSound("shoot", "/assets/sounds/SHOOT.mp3");
        loadSound("explosion", "/assets/sounds/EXPLOSION.mp3");
        loadSound("hit", "/assets/sounds/HIT.mp3");
        loadSound("lightning", "/assets/sounds/LIGHTNING.mp3");
        loadSound("pickup", "/assets/sounds/PICKUP.mp3");

        // Debug output - show all loaded sounds
        if (!soundEffects.isEmpty()) {
            soundsLoaded = true;
            System.out.println("Successfully loaded " + soundEffects.size() + " sound effects: " + String.join(", ", soundEffects.keySet()));
        } else {
            System.err.println("WARNING: No sound effects were loaded!");
            createDefaultSound("button_click");
            createDefaultSound("btn_click");
        }
    }

    private void loadSound(String name, String path) {
        try {
            java.net.URL soundUrl = getClass().getResource(path);
            if (soundUrl != null) {
                AudioClip clip = new AudioClip(soundUrl.toExternalForm());
                soundEffects.put(name, clip);
                System.out.println("Loaded sound effect: " + name + " from " + path);
            } else {
                System.err.println("Sound effect file not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading sound effect " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSoundAsMedia(String name, String path) {
        try {
            java.net.URL soundUrl = getClass().getResource(path);
            if (soundUrl != null) {
                Media media = new Media(soundUrl.toExternalForm());
                System.out.println("Successfully created Media for: " + name);

                // Store the MediaPlayer in a ready state
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setOnEndOfMedia(() -> {
                    mediaPlayer.stop();
                    mediaPlayer.seek(Duration.ZERO);
                });

                soundEffects.put(name, mediaPlayer);
                System.out.println("Loaded sound effect as Media: " + name + " from " + path);
            } else {
                System.err.println("Sound effect file not found: " + path);
                // Try with alternate approach
                try {
                    // Try the ClassLoader approach as fallback
                    java.net.URL classLoaderUrl = SoundManager.class.getClassLoader().getResource("assets/sounds/btn.mp3");
                    if (classLoaderUrl != null && name.equals("btn_click")) {
                        Media media = new Media(classLoaderUrl.toExternalForm());
                        MediaPlayer mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.setOnEndOfMedia(() -> {
                            mediaPlayer.stop();
                            mediaPlayer.seek(Duration.ZERO);
                        });
                        soundEffects.put(name, mediaPlayer);
                        System.out.println("Loaded sound effect using ClassLoader: " + name);
                    } else {
                        System.err.println("Sound file not found with ClassLoader either: " + (path.startsWith("/") ? path.substring(1) : path));
                    }
                } catch (Exception e2) {
                    System.err.println("Also failed with ClassLoader approach: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading sound effect as Media " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDefaultSound(String name) {
        try {
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
        Object sound = soundEffects.get(name);
        if (sound != null) {
            try {
                // Calculate volume for this sound
                double volume = FXGL.getSettings().getGlobalSoundVolume();
                if (name.contains("click") || name.contains("btn")) {
                    // Make button clicks louder
                    volume = Math.min(1.0, volume * 2.0);
                }

                // Handle different types of sound objects
                if (sound instanceof AudioClip) {
                    AudioClip clip = (AudioClip) sound;
                    clip.setVolume(volume);
                    clip.play();
                    System.out.println("Playing AudioClip: " + name + " at volume " + volume);
                } else if (sound instanceof MediaPlayer) {
                    MediaPlayer mediaPlayer = (MediaPlayer) sound;
                    mediaPlayer.setVolume(volume);
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.play();
                    System.out.println("Playing MediaPlayer: " + name + " at volume " + volume);
                }
            } catch (Exception e) {
                System.err.println("Error playing sound " + name + ": " + e.getMessage());
            }
        } else {
            if (soundsLoaded) {
                System.err.println("Sound effect not found in loaded sounds: " + name);
            } else {
                System.err.println("No sounds were loaded - sound system may not be initialized properly");
            }
        }
    }

    public void stopSound(String name) {
        Object sound = soundEffects.get(name);
        if (sound != null) {
            if (sound instanceof AudioClip) {
                ((AudioClip) sound).stop();
            } else if (sound instanceof MediaPlayer) {
                ((MediaPlayer) sound).stop();
            }
        }
    }

    public void stopAllSounds() {
        for (Object sound : soundEffects.values()) {
            if (sound instanceof AudioClip) {
                ((AudioClip) sound).stop();
            } else if (sound instanceof MediaPlayer) {
                ((MediaPlayer) sound).stop();
            }
        }
    }
}