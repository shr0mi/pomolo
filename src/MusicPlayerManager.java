// Save as: src/MusicPlayerManager.java (OVERWRITE)

import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MusicPlayerManager {

    private static MusicPlayerManager instance;

    private MediaPlayer mediaPlayer;
    private List<SongManager.SongInfo> songQueue = Collections.emptyList();
    private int currentSongIndex = -1;

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
                mediaPlayer.setVolume(newVol.doubleValue());
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
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            File file = new File(song.path);
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                totalDuration.set(mediaPlayer.getMedia().getDuration());
                currentSong.set(song);
                isPlaying.set(true);
                mediaPlayer.play();
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                currentTime.set(newTime);
            });

            mediaPlayer.setOnEndOfMedia(this::next);
            mediaPlayer.setVolume(volume.get());

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
            isPlaying.set(false);
        }
    }

    public void playPause() {
        if (mediaPlayer != null) {
            if (isPlaying.get()) {
                mediaPlayer.pause();
                isPlaying.set(false);
            } else {
                mediaPlayer.play();
                isPlaying.set(true);
            }
        } else if (!songQueue.isEmpty()) {
            playSong(0);
        }
    }

    public void next() {
        if (songQueue.isEmpty()) return;
        currentSongIndex = (currentSongIndex + 1) % songQueue.size(); // Wrap around
        playSong(currentSongIndex);
    }

    public void previous() {
        if (songQueue.isEmpty()) return;
        currentSongIndex = (currentSongIndex - 1 + songQueue.size()) % songQueue.size(); // Wrap around
        playSong(currentSongIndex);
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(duration);
        }
    }

    public void shutdown() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}