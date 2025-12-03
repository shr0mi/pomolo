package com;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

public class MusicPlayerManager {

    private static MusicPlayerManager instance;

    private MediaPlayer mediaPlayer;
    private List<SongManager.SongInfo> songQueue = Collections.emptyList();
    private int currentSongIndex = -1;

    // Store playback state for recovery
    private double savedPosition = 0.0;
    private boolean wasPlayingBeforeError = false;

    // --- JavaFX Properties for UI Binding ---
    private final ReadOnlyObjectWrapper<SongManager.SongInfo> currentSong = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper isPlaying = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<Duration> currentTime = new ReadOnlyObjectWrapper<>(Duration.ZERO);
    private final ReadOnlyObjectWrapper<Duration> totalDuration = new ReadOnlyObjectWrapper<>(Duration.ZERO);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.8); // Default 80% volume

    // Private constructor for Singleton
    private MusicPlayerManager() {
        volume.addListener((obs, oldVol, newVol) -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setVolume(newVol.doubleValue());
                } catch (Exception e) {
                    System.err.println("Error setting volume: " + e.getMessage());
                }
            }
        });
    }

    // Public method to get the single instance
    public static synchronized MusicPlayerManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerManager();
        }
        return instance;
    }

    // --- Public Getters for Properties (for UI binding) ---
    public ReadOnlyObjectProperty<SongManager.SongInfo> currentSongProperty() {
        return currentSong.getReadOnlyProperty();
    }
    public ReadOnlyBooleanProperty isPlayingProperty() {
        return isPlaying.getReadOnlyProperty();
    }
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }
    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDuration.getReadOnlyProperty();
    }
    public DoubleProperty volumeProperty() {
        return volume;
    }

    // --- Playback Controls ---

    public void setQueue(List<SongManager.SongInfo> songs) {
        this.songQueue = songs;
    }

    public List<SongManager.SongInfo> getQueue() {
        return this.songQueue;
    }

    public void playSong(int index) {
        if (index < 0 || index >= songQueue.size()) return;

        currentSongIndex = index;
        SongManager.SongInfo song = songQueue.get(currentSongIndex);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                System.err.println("Error disposing old media player: " + e.getMessage());
            }
            mediaPlayer = null;
        }

        try {
            File file = new File(song.path);
            if(!file.exists()){
                throw new FileNotFoundException();
            }
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // Add error handler to catch sleep/wake issues
            mediaPlayer.setOnError(() -> {
                MediaException error = mediaPlayer.getError();
                System.err.println("MediaPlayer Error: " + (error != null ? error.getMessage() : "Unknown"));
                handleMediaPlayerError();
            });

            // Handle stalled playback (common after system sleep)
            mediaPlayer.setOnStalled(() -> {
                System.out.println("Playback stalled, attempting recovery...");
                handleStalledPlayback();
            });

            mediaPlayer.setOnReady(() -> {
                totalDuration.set(mediaPlayer.getMedia().getDuration());
                currentSong.set(song);
                isPlaying.set(true);

                // Restore position if recovering from error
                if (savedPosition > 0) {
                    mediaPlayer.seek(Duration.seconds(savedPosition));
                    savedPosition = 0.0;
                }

                try {
                    mediaPlayer.play();
                } catch (Exception e) {
                    System.err.println("Error starting playback: " + e.getMessage());
                    handleMediaPlayerError();
                }
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                currentTime.set(newTime);
            });

            mediaPlayer.setOnEndOfMedia(this::next);
            mediaPlayer.setVolume(volume.get());

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
            isPlaying.set(false);
            SqliteDBManager.deleteSong(song.path);
        }
    }

    private void handleMediaPlayerError() {
        if (mediaPlayer == null) return;

        try {
            // Save current state
            savedPosition = mediaPlayer.getCurrentTime().toSeconds();
            wasPlayingBeforeError = isPlaying.get();

            Platform.runLater(() -> {
                if (wasPlayingBeforeError && currentSongIndex >= 0) {
                    System.out.println("Attempting to recover playback from position: " + savedPosition);
                    // Recreate the media player with the same song
                    playSong(currentSongIndex);
                } else {
                    isPlaying.set(false);
                }
            });
        } catch (Exception e) {
            System.err.println("Error during recovery: " + e.getMessage());
            isPlaying.set(false);
        }
    }

    private void handleStalledPlayback() {
        if (mediaPlayer == null) return;

        try {
            savedPosition = mediaPlayer.getCurrentTime().toSeconds();

            Platform.runLater(() -> {
                try {
                    // Try simple recovery first
                    mediaPlayer.pause();
                    Thread.sleep(100);
                    mediaPlayer.seek(Duration.seconds(savedPosition));
                    mediaPlayer.play();
                } catch (Exception e) {
                    System.err.println("Simple recovery failed, recreating player: " + e.getMessage());
                    // Fall back to full recreation
                    wasPlayingBeforeError = true;
                    handleMediaPlayerError();
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling stalled playback: " + e.getMessage());
        }
    }

    public void playPause() {
        if (mediaPlayer != null) {
            try {
                if (isPlaying.get()) {
                    mediaPlayer.pause();
                    isPlaying.set(false);
                } else {
                    mediaPlayer.play();
                    isPlaying.set(true);
                }
            } catch (Exception e) {
                System.err.println("Error in playPause: " + e.getMessage());
                handleMediaPlayerError();
            }
        } else if (!songQueue.isEmpty()) {
            playSong(0);
        }
    }

    public void next() {
        if (songQueue.isEmpty()) return;
        savedPosition = 0.0; // Reset saved position for new song
        currentSongIndex = (currentSongIndex + 1) % songQueue.size(); // Wrap around
        playSong(currentSongIndex);
    }

    public void previous() {
        if (songQueue.isEmpty()) return;
        savedPosition = 0.0; // Reset saved position for new song
        currentSongIndex = (currentSongIndex - 1 + songQueue.size()) % songQueue.size(); // Wrap around
        playSong(currentSongIndex);
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seek(duration);
            } catch (Exception e) {
                System.err.println("Error seeking: " + e.getMessage());
                handleMediaPlayerError();
            }
        }
    }

    public void shutdown() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
            mediaPlayer = null;
        }
    }
}