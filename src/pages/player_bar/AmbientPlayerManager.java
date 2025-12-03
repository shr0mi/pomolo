package pages.player_bar;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class AmbientPlayerManager {
    private final BooleanProperty isPlaying = new SimpleBooleanProperty(false);

    // Media objects (reusable)
    private Media rain;
    private Media fireplace;
    private Media wind;

    // MediaPlayer objects (may need recreation)
    private MediaPlayer rainPlayer;
    private MediaPlayer firePlayer;
    private MediaPlayer windPlayer;

    // Volume settings (persist across recreations)
    private double rainVolume = 0.5;
    private double fireplaceVolume = 0.5;
    private double windVolume = 0.5;

    private boolean shouldBePlayingRain = false;
    private boolean shouldBePlayingFire = false;
    private boolean shouldBePlayingWind = false;

    public AmbientPlayerManager() {
        initializeMedia();
        createPlayers();
    }

    private void initializeMedia() {
        try {
            rain = new Media(getClass().getResource("/ambient_sounds/rain.wav").toExternalForm());
            fireplace = new Media(getClass().getResource("/ambient_sounds/fireplace.mp3").toExternalForm());
            wind = new Media(getClass().getResource("/ambient_sounds/wind.mp3").toExternalForm());
            System.out.println("Ambient media initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing ambient media: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPlayers() {
        try {
            // Clean up old players if they exist
            disposePlayer(rainPlayer);
            disposePlayer(firePlayer);
            disposePlayer(windPlayer);

            // Create new players
            rainPlayer = new MediaPlayer(rain);
            firePlayer = new MediaPlayer(fireplace);
            windPlayer = new MediaPlayer(wind);

            // Configure players
            setupPlayer(rainPlayer, "Rain");
            setupPlayer(firePlayer, "Fire");
            setupPlayer(windPlayer, "Wind");

            // Restore volumes
            rainPlayer.setVolume(rainVolume);
            firePlayer.setVolume(fireplaceVolume);
            windPlayer.setVolume(windVolume);

            System.out.println("Ambient players created successfully");
        } catch (Exception e) {
            System.err.println("Error creating ambient players: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupPlayer(MediaPlayer player, String name) {
        player.setCycleCount(MediaPlayer.INDEFINITE);

        player.setOnError(() -> {
            System.err.println(name + " player error: " +
                    (player.getError() != null ? player.getError().getMessage() : "Unknown"));
            handlePlayerError();
        });

        player.setOnStalled(() -> {
            System.out.println(name + " player stalled, attempting recovery...");
            handlePlayerStall();
        });

        player.setOnReady(() -> {
            System.out.println(name + " player ready");
        });
    }

    private void handlePlayerError() {
        System.out.println("Handling ambient player error, attempting recovery...");

        Platform.runLater(() -> {
            try {
                // Save current playing state
                boolean wasPlaying = isPlaying.get();

                // Stop current players
                stopAllPlayersSafely();

                // Small delay before recreation
                Thread.sleep(500);

                // Recreate players
                createPlayers();

                // Restart if was playing
                if (wasPlaying) {
                    System.out.println("Restarting ambient sounds after recovery");
                    restartPlayback();
                }
            } catch (Exception e) {
                System.err.println("Error during ambient player recovery: " + e.getMessage());
                e.printStackTrace();
                isPlaying.set(false);
            }
        });
    }

    private void handlePlayerStall() {
        Platform.runLater(() -> {
            try {
                System.out.println("Attempting to recover stalled ambient players...");

                // Try simple recovery first
                if (shouldBePlayingRain && rainPlayer != null) {
                    rainPlayer.pause();
                    Thread.sleep(50);
                    rainPlayer.play();
                }
                if (shouldBePlayingFire && firePlayer != null) {
                    firePlayer.pause();
                    Thread.sleep(50);
                    firePlayer.play();
                }
                if (shouldBePlayingWind && windPlayer != null) {
                    windPlayer.pause();
                    Thread.sleep(50);
                    windPlayer.play();
                }
            } catch (Exception e) {
                System.err.println("Stall recovery failed, recreating players: " + e.getMessage());
                handlePlayerError();
            }
        });
    }

    private void stopAllPlayersSafely() {
        try {
            if (rainPlayer != null) rainPlayer.stop();
        } catch (Exception e) {
            System.err.println("Error stopping rain player: " + e.getMessage());
        }

        try {
            if (firePlayer != null) firePlayer.stop();
        } catch (Exception e) {
            System.err.println("Error stopping fire player: " + e.getMessage());
        }

        try {
            if (windPlayer != null) windPlayer.stop();
        } catch (Exception e) {
            System.err.println("Error stopping wind player: " + e.getMessage());
        }
    }

    private void disposePlayer(MediaPlayer player) {
        if (player != null) {
            try {
                player.stop();
                player.dispose();
            } catch (Exception e) {
                System.err.println("Error disposing player: " + e.getMessage());
            }
        }
    }

    private void restartPlayback() {
        if (shouldBePlayingRain || shouldBePlayingFire || shouldBePlayingWind) {
            try {
                if (shouldBePlayingRain) rainPlayer.play();
                if (shouldBePlayingFire) firePlayer.play();
                if (shouldBePlayingWind) windPlayer.play();
                isPlaying.set(true);
            } catch (Exception e) {
                System.err.println("Error restarting playback: " + e.getMessage());
                isPlaying.set(false);
            }
        }
    }

    public BooleanProperty isPlayingProperty() {
        return isPlaying;
    }

    public final boolean getIsPlaying() {
        return isPlaying.get();
    }

    public void playAmbientMusic() {
        try {
            shouldBePlayingRain = true;
            shouldBePlayingFire = true;
            shouldBePlayingWind = true;

            windPlayer.play();
            rainPlayer.play();
            firePlayer.play();
            isPlaying.set(true);
            System.out.println("Ambient music started");
        } catch (Exception e) {
            System.err.println("Error playing ambient music: " + e.getMessage());
            e.printStackTrace();
            handlePlayerError();
        }
    }

    public void stopAmbientMusic() {
        try {
            shouldBePlayingRain = false;
            shouldBePlayingFire = false;
            shouldBePlayingWind = false;

            stopAllPlayersSafely();
            isPlaying.set(false);
            System.out.println("Ambient music stopped");
        } catch (Exception e) {
            System.err.println("Error stopping ambient music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Rain sound control
    public void setRainVolume(double volume) {
        try {
            rainVolume = volume;
            if (rainPlayer != null) {
                rainPlayer.setVolume(volume);
            }
        } catch (Exception e) {
            System.err.println("Error setting rain volume: " + e.getMessage());
        }
    }

    public double getRainVolume() {
        try {
            return rainPlayer != null ? rainPlayer.getVolume() : rainVolume;
        } catch (Exception e) {
            System.err.println("Error getting rain volume: " + e.getMessage());
            return rainVolume;
        }
    }

    // Fire sound control
    public void setFireplaceVolume(double volume) {
        try {
            fireplaceVolume = volume;
            if (firePlayer != null) {
                firePlayer.setVolume(volume);
            }
        } catch (Exception e) {
            System.err.println("Error setting fireplace volume: " + e.getMessage());
        }
    }

    public double getFireplaceVolume() {
        try {
            return firePlayer != null ? firePlayer.getVolume() : fireplaceVolume;
        } catch (Exception e) {
            System.err.println("Error getting fireplace volume: " + e.getMessage());
            return fireplaceVolume;
        }
    }

    // Wind sound control
    public void setWindVolume(double volume) {
        try {
            windVolume = volume;
            if (windPlayer != null) {
                windPlayer.setVolume(volume);
            }
        } catch (Exception e) {
            System.err.println("Error setting wind volume: " + e.getMessage());
        }
    }

    public double getWindVolume() {
        try {
            return windPlayer != null ? windPlayer.getVolume() : windVolume;
        } catch (Exception e) {
            System.err.println("Error getting wind volume: " + e.getMessage());
            return windVolume;
        }
    }

    // Clean up method (call this when shutting down the app)
    public void shutdown() {
        System.out.println("Shutting down ambient player...");
        stopAmbientMusic();
        disposePlayer(rainPlayer);
        disposePlayer(firePlayer);
        disposePlayer(windPlayer);
    }
}