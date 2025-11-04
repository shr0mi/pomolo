import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class miniPlayerController {

    @FXML
    private StackPane root;

    @FXML
    private Label songLabel;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button prevButton;

    @FXML
    private Circle glowRing;

    private double xOffset = 0;
    private double yOffset = 0;

    private final MusicPlayerManager musicManager = MusicPlayerManager.getInstance();

    private Timeline glowPulse;
    private ScaleTransition currentScale;

    @FXML
    public void initialize() {
        // === DRAG ANYWHERE ===
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // === HOVER SCALE ANIMATION (only affects size, not glow/beat) ===
        root.setOnMouseEntered(e -> animateScale(root, 1.0));   // expand
        root.setOnMouseExited(e -> animateScale(root, 0.75));   // shrink more
        root.setScaleX(0.75);
        root.setScaleY(0.75);

        // === SONG TITLE SYNC ===
        var song = musicManager.currentSongProperty().get();
        if (song != null) songLabel.setText(song.fileName);

        musicManager.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) songLabel.setText(newSong.fileName);
        });

        // === ICON SYNC ===
        updatePlayPauseIcon();

        musicManager.isPlayingProperty().addListener((obs, wasPlaying, isNowPlaying) -> {
            updatePlayPauseIcon();
            updateGlowAnimation(isNowPlaying);
        });

        // === INITIALIZE EFFECTS ===
        setupGlowEffect();
        updateGlowAnimation(musicManager.isPlayingProperty().get());
    }

    // --- Helper: Smooth scale animation without stopping glow ---
    private void animateScale(javafx.scene.Node node, double targetScale) {
        if (currentScale != null) currentScale.stop();
        currentScale = new ScaleTransition(Duration.millis(250), node);
        currentScale.setToX(targetScale);
        currentScale.setToY(targetScale);
        currentScale.setInterpolator(Interpolator.EASE_BOTH);
        currentScale.play();
    }

    // --- Button animation (unchanged) ---
    private void animateButtonClick(Button button, Runnable actionAfterShrink) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(120), button);
        scaleDown.setToX(0.8);
        scaleDown.setToY(0.8);

        FadeTransition fadeDown = new FadeTransition(Duration.millis(120), button);
        fadeDown.setToValue(0.4);

        ParallelTransition shrink = new ParallelTransition(scaleDown, fadeDown);
        shrink.setOnFinished(e -> {
            if (actionAfterShrink != null) actionAfterShrink.run();

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(120), button);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);

            FadeTransition fadeUp = new FadeTransition(Duration.millis(120), button);
            fadeUp.setToValue(1.0);

            new ParallelTransition(scaleUp, fadeUp).play();
        });
        shrink.play();
    }

    // --- Play/Pause icon styling ---
    private void updatePlayPauseIcon() {
        if (musicManager.isPlayingProperty().get()) {
            playPauseButton.setText("⏸");
            playPauseButton.setStyle(
                    "-fx-font-size: 22px; -fx-background-color: #E53935; -fx-text-fill: white; "
                            + "-fx-background-radius: 50%; -fx-pref-width: 50; -fx-pref-height: 50;"
            );
        } else {
            playPauseButton.setText("▶");
            playPauseButton.setStyle(
                    "-fx-font-size: 22px; -fx-background-color: #4CAF50; -fx-text-fill: white; "
                            + "-fx-background-radius: 50%; -fx-pref-width: 50; -fx-pref-height: 50;"
            );
        }
    }

    // === Glowing effect ===
    private void setupGlowEffect() {
        if (glowRing != null) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#4CAF50"));
            glow.setRadius(25);
            glow.setSpread(0.3);
            glowRing.setEffect(glow);

            double baseRadius = glowRing.getRadius();

            glowPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(glowRing.opacityProperty(), 1.0),
                            new KeyValue(glowRing.radiusProperty(), baseRadius),
                            new KeyValue(glow.colorProperty(), Color.web("#4CAF50"))
                    ),
                    new KeyFrame(Duration.seconds(1.2),
                            new KeyValue(glowRing.opacityProperty(), 0.4),
                            new KeyValue(glowRing.radiusProperty(), baseRadius + 6),
                            new KeyValue(glow.colorProperty(), Color.web("#80FFF9"))
                    )
            );
            glowPulse.setCycleCount(Animation.INDEFINITE);
            glowPulse.setAutoReverse(true);
        }
    }

    private void updateGlowAnimation(boolean isPlaying) {
        if (glowPulse == null) return;
        if (isPlaying) {
            glowPulse.play();
        } else {
            glowPulse.stop();
            glowRing.setOpacity(0.3);
            glowRing.setRadius(135);
            DropShadow glow = (DropShadow) glowRing.getEffect();
            if (glow != null) glow.setColor(Color.web("#4CAF50"));
        }
    }

    // === Button Actions ===
    @FXML
    private void playPause() {
        animateButtonClick(playPauseButton, () -> {
            musicManager.playPause();
            updatePlayPauseIcon();
        });
    }

    @FXML
    private void next() {
        animateButtonClick(nextButton, () -> musicManager.next());
    }

    @FXML
    private void previous() {
        animateButtonClick(prevButton, () -> musicManager.previous());
    }

    @FXML
    private void restoreMain() {
        Stage miniStage = (Stage) root.getScene().getWindow();
        miniStage.close();

        Stage mainStage = (Stage) Main.getRootController().getRootPane().getScene().getWindow();
        mainStage.show();
    }
}
