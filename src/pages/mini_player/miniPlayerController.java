package pages.mini_player;

import com.Main;
import com.MusicPlayerManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.PomodoroModel;
import org.kordamp.ikonli.javafx.FontIcon;
import pages.player_bar.PlayerBarController;

public class miniPlayerController {

    @FXML private StackPane root;
    @FXML private StackPane uiContainer;
    @FXML private StackPane musicDesign;
    @FXML private Circle visualCircle;
    @FXML private Label songLabel;
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon; // Added fx:id for FontIcon
    @FXML private Button nextButton;
    @FXML private Button prevButton;
    @FXML private Button ambientButton;
    @FXML private Circle glowRing;
    @FXML private Circle timerProgressRing;


    private double xOffset, yOffset;
    private final MusicPlayerManager musicManager = MusicPlayerManager.getInstance();
    private final PomodoroModel pomodoroModel = PomodoroModel.getInstance();
    private Timeline glowPulse, visualPulse;
    private ScaleTransition currentScale;
    private AnimationTimer ringAnimationTimer;

    @FXML
    public void initialize() {
        // Ensure the mini-player starts compact before any animations run
        if (root != null) {
            root.setScaleX(0.75);
            root.setScaleY(0.75);
            // Prevent children from overflowing the mini window by clipping and bounding the root
            root.setMaxWidth(300);
            root.setMaxHeight(300);
            root.setPrefWidth(300);
            root.setPrefHeight(300);
            root.setClip(new javafx.scene.shape.Rectangle(300, 300));
        }
        setupDrag();
        setupHoverAnimations();
        setupGlowEffect();
        setupTimerUpdates();

        // AnimationTimer updates the ring each frame for smooth visuals
        ringAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (timerProgressRing != null && pomodoroModel.getDurationInSeconds() > 0) {
                    updateTimerProgress(pomodoroModel.getRemainingSeconds(), pomodoroModel.getDurationInSeconds());
                }
            }
        };
        ringAnimationTimer.start();

        // Initialize the timer progress ring visuals so it matches Pomodoro page behavior
        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            timerProgressRing.setVisible(false); // initially hidden until Pomodoro started/paused
            // set initial progress from model
            if (pomodoroModel.getDurationInSeconds() > 0) {
                double remaining = pomodoroModel.getRemainingSeconds();
                double fractionElapsed = 1.0 - (remaining / (double) pomodoroModel.getDurationInSeconds());
                if (fractionElapsed < 0) fractionElapsed = 0;
                if (fractionElapsed > 1) fractionElapsed = 1;
                double dashOffset = fractionElapsed * circumference;
                timerProgressRing.setStrokeDashOffset(dashOffset);
            }
        }

        // Initial state: UI hidden, music design shown
        uiContainer.setOpacity(0);
        musicDesign.setOpacity(1);

        // Set up music visual animation (pulsing)
        visualPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(visualCircle.scaleXProperty(), 1.0),
                        new KeyValue(visualCircle.scaleYProperty(), 1.0),
                        new KeyValue(visualCircle.opacityProperty(), 0.15)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(visualCircle.scaleXProperty(), 1.15),
                        new KeyValue(visualCircle.scaleYProperty(), 1.15),
                        new KeyValue(visualCircle.opacityProperty(), 0.25))
        );
        visualPulse.setAutoReverse(true);
        visualPulse.setCycleCount(Animation.INDEFINITE);

        // Sync with MusicPlayerManager
        var song = musicManager.currentSongProperty().get();
        if (song != null) songLabel.setText(song.fileName);
        musicManager.currentSongProperty().addListener((obs, old, val) -> {
            if (val != null) songLabel.setText(val.fileName);
        });

        updatePlayPauseIcon();
        musicManager.isPlayingProperty().addListener((obs, was, now) -> {
            updatePlayPauseIcon();
            updateGlowAnimation(now);
            updateVisualAnimation(now); // Call new method here
        });

        // Initialize ambient button style to reflect current ambient playing state
        if (ambientButton != null) {
            if (PlayerBarController.APM.isPlaying) {
                ambientButton.getStyleClass().remove("mini-player-control-button");
                ambientButton.getStyleClass().add("mini-player-control-button-selected");
            } else {
                ambientButton.getStyleClass().remove("mini-player-control-button-selected");
                if (!ambientButton.getStyleClass().contains("mini-player-control-button"))
                    ambientButton.getStyleClass().add("mini-player-control-button");
            }
        }

        updateGlowAnimation(musicManager.isPlayingProperty().get());
        updateVisualAnimation(musicManager.isPlayingProperty().get()); // Initial call
    }

    private void setupTimerUpdates() {
        // Use the double value so we preserve sub-second resolution and get smooth updates
        pomodoroModel.getRemainingSecondsProperty().addListener((obs, old, newVal) -> {
            updateTimerProgress(newVal.doubleValue(), pomodoroModel.getDurationInSeconds());
            // Make the ring visible when there is a configured duration (it will be updated by the model)
            if (timerProgressRing != null) {
                timerProgressRing.setVisible(pomodoroModel.getDurationInSeconds() > 0);
            }
        });
    }

    // Accept double to keep sub-second precision (matches PomodoroController timeline updates)
    private void updateTimerProgress(double remainingSeconds, long totalDuration) {
        if (timerProgressRing == null) return;
        if (totalDuration <= 0) {
            timerProgressRing.setStrokeDashOffset(0);
            return;
        }

        double fractionElapsed = 1.0 - ((double) remainingSeconds / (double) totalDuration);
        if (fractionElapsed < 0) fractionElapsed = 0;
        if (fractionElapsed > 1.0) fractionElapsed = 1.0;

        double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        timerProgressRing.setStrokeDashOffset(fractionElapsed * circumference);
    }

    private void setupDrag() {
        root.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        root.setOnMouseDragged(e -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    private void setupHoverAnimations() {
        root.setOnMouseEntered(e -> {
            animateFade(uiContainer, 1.0);
            animateFade(musicDesign, 0.0);
            animateScale(root, 1.0);
        });

        root.setOnMouseExited(e -> {
            animateFade(uiContainer, 0.0);
            animateFade(musicDesign, 1.0);
            animateScale(root, 0.75);
        });

        // start at compact scale (already set at top of initialize)
        // no-op here
    }

    private void animateFade(Node node, double targetOpacity) {
        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setToValue(targetOpacity);
        fade.play();
    }

    private void animateScale(Node node, double targetScale) {
        if (currentScale != null) currentScale.stop();
        currentScale = new ScaleTransition(Duration.millis(250), node);
        currentScale.setToX(targetScale);
        currentScale.setToY(targetScale);
        currentScale.setInterpolator(Interpolator.EASE_BOTH);
        currentScale.play();
    }

    private void setupGlowEffect() {
        if (glowRing != null) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#8A6FAD")); // Aesthetic purple
            glow.setRadius(25);
            glow.setSpread(0.3);
            glowRing.setEffect(glow);

            double baseRadius = glowRing.getRadius();

            glowPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(glowRing.opacityProperty(), 1.0),
                            new KeyValue(glowRing.radiusProperty(), baseRadius),
                            new KeyValue(glow.colorProperty(), Color.web("#8A6FAD")) // Aesthetic purple
                    ),
                    new KeyFrame(Duration.seconds(1.2),
                            new KeyValue(glowRing.opacityProperty(), 0.4),
                            new KeyValue(glowRing.radiusProperty(), baseRadius + 6),
                            new KeyValue(glow.colorProperty(), Color.web("#A080C0")) // Slightly lighter aesthetic purple
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
            if (glow != null) glow.setColor(Color.web("#8A6FAD")); // Aesthetic purple
        }
    }

    // New method to control visual animation
    private void updateVisualAnimation(boolean isPlaying) {
        if (visualPulse == null) return;
        if (isPlaying) {
            visualPulse.play();
        } else {
            visualPulse.stop();
            // Reset visualCircle to its initial state when paused
            visualCircle.setScaleX(1.0);
            visualCircle.setScaleY(1.0);
            visualCircle.setOpacity(0.15);
        }
    }

    // === UI Controls ===
    @FXML private void playPause() {
        animateButtonClick(playPauseButton, () -> {
            musicManager.playPause();
            updatePlayPauseIcon();
        });
    }

    @FXML private void next() {
        animateButtonClick(nextButton, () -> musicManager.next());
    }

    @FXML private void previous() {
        animateButtonClick(prevButton, () -> musicManager.previous());
    }

    @FXML private void restoreMain() {
        Stage miniStage = (Stage) root.getScene().getWindow();
        miniStage.close();

        Stage mainStage = (Stage) Main.getRootController().getRootPane().getScene().getWindow();
        mainStage.show();
    }

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

    private void updatePlayPauseIcon() {
        playPauseButton.getStyleClass().removeAll("mini-player-play-button", "mini-player-pause-button");
        if (musicManager.isPlayingProperty().get()) {
            playPauseButton.getStyleClass().add("mini-player-pause-button");
            playPauseIcon.setIconLiteral("fas-pause"); // Update FontIcon directly
        } else {
            playPauseButton.getStyleClass().add("mini-player-play-button");
            playPauseIcon.setIconLiteral("fas-play"); // Update FontIcon directly
        }
    }

    @FXML
    private void handleAmbientMini() {
        if (PlayerBarController.APM == null) return;
        if (!PlayerBarController.APM.isPlaying) {
            ambientButton.getStyleClass().remove("mini-player-control-button");
            ambientButton.getStyleClass().add("mini-player-control-button-selected");
            PlayerBarController.APM.playAmbientMusic();
        } else {
            ambientButton.getStyleClass().remove("mini-player-control-button-selected");
            if (!ambientButton.getStyleClass().contains("mini-player-control-button"))
                ambientButton.getStyleClass().add("mini-player-control-button");
            PlayerBarController.APM.stopAmbientMusic();
        }
    }
}
