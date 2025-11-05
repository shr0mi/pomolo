import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class PlayerBarController {

    @FXML private VBox playerBarPane;
    @FXML private Button prevButton;
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon;
    @FXML private Button nextButton;
    @FXML private Text currentSongText;
    @FXML private Text currentTimeText;
    @FXML private Text totalTimeText;
    @FXML private Slider progressSlider;
    @FXML private Slider volumeSlider;

    private MusicPlayerManager playerManager;
    private boolean isSliderBeingDragged = false;

    @FXML
    private void initialize() {
        playerManager = MusicPlayerManager.getInstance();
        bindControls();

        progressSlider.setOnMousePressed(e -> isSliderBeingDragged = true);
        progressSlider.setOnMouseReleased(e -> {
            if (playerManager.totalDurationProperty().get() != null) {
                Duration total = playerManager.totalDurationProperty().get();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    playerManager.seek(total.multiply(progressSlider.getValue() / 100.0));
                }
            }
            isSliderBeingDragged = false;
        });
    }

    private void bindControls() {
        //Play-pause button change
        playerManager.isPlayingProperty().addListener((obs, wasPlaying, isNowPlaying)->{
            playPauseIcon.setIconLiteral(isNowPlaying ? "fas-pause" : "fas-play");
        });

        currentSongText.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    SongManager.SongInfo song = playerManager.currentSongProperty().get();
                    if (song != null) {
                        String name = song.fileName;
                        if (name.length() > 30) {
                            name = name.substring(0, 27) + "...";
                        }
                        return "Now Playing: " + name;
                    }
                    return "No song selected";
                }, playerManager.currentSongProperty())
        );

        volumeSlider.valueProperty().bindBidirectional(playerManager.volumeProperty());
        //Initial Style
        Platform.runLater(() -> {
            StackPane vol_track = (StackPane) volumeSlider.lookup(".track");
            if (vol_track != null) {
                double val = volumeSlider.getValue();  // current value
                String vol_style = String.format(
                        "-fx-background-color: linear-gradient(to right, red %f%%, white %f%%);",
                        val*100.0, val*100.0
                );
                vol_track.setStyle(vol_style);
            }
        });
        // volume progress color setting
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            StackPane vol_track = (StackPane) volumeSlider.lookup(".track");
            if (vol_track != null) {
                String vol_style = String.format(
                        "-fx-background-color: linear-gradient(to right, red %f%%, white %f%%);",
                        newVal.doubleValue()*100.0, newVal.doubleValue()*100.0
                );
                //System.out.println(newVal.doubleValue());
                vol_track.setStyle(vol_style);
            }
        });

        playerManager.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSliderBeingDragged && playerManager.totalDurationProperty().get() != null) {
                Duration total = playerManager.totalDurationProperty().get();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    double value = newTime.toSeconds() / total.toSeconds() * 100.0;
                    progressSlider.setValue(value);

                    // progress color setting
                    StackPane track = (StackPane) progressSlider.lookup(".track");
                    String style = String.format("-fx-background-color: linear-gradient(to right, red %f%%, white %f%%);",
                            value, value);
                    //System.out.println(value);
                    track.setStyle(style);
                }
            }
            currentTimeText.setText(formatDurationSimple(newTime));
        });

        playerManager.totalDurationProperty().addListener((obs, oldDur, newDur) -> {
            if (newDur != null && newDur.greaterThan(Duration.ZERO)) {
                totalTimeText.setText(formatDurationSimple(newDur));
            } else {
                totalTimeText.setText("00:00");
            }
        });
    }

    @FXML private void handlePlayPause() { playerManager.playPause(); }
    @FXML private void handleNext() { playerManager.next(); }
    @FXML private void handlePrevious() { playerManager.previous(); }

    private String formatDurationSimple(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        long seconds = (long) duration.toSeconds();
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }


}