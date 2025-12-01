package pages.mini_player;

import com.Main;
import com.MusicPlayerManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.PomodoroModel; // We use PomodoroController static methods now, but this import is fine
import pages.pomodoro.PomodoroController; // IMPORT ADDED
import org.kordamp.ikonli.javafx.FontIcon;
import pages.player_bar.AmbientPlayerManager;
import pages.player_bar.PlayerBarController;
import javafx.animation.Interpolator;

public class miniPlayerController {

    @FXML private StackPane root;
    @FXML private StackPane uiContainer;
    @FXML private StackPane musicDesign;
    @FXML private Circle visualCircle;
    @FXML private Circle visualCircle2;
    @FXML private Circle visualCircle3;
    @FXML private Label songLabel;
    @FXML private Button playPauseButton;
    @FXML private FontIcon playPauseIcon;
    @FXML private Button nextButton;
    @FXML private Button prevButton;
    @FXML private Button ambientButton;
    @FXML private Circle glowRing;
    @FXML private Circle timerProgressRing;
    @FXML private StackPane ringVisualsStack;

    private double xOffset, yOffset;
    private final MusicPlayerManager musicManager = MusicPlayerManager.getInstance();
    // private final PomodoroModel pomodoroModel = PomodoroModel.getInstance(); // Not needed for the ring anymore
    private final AmbientPlayerManager ambientPlayerManager = PlayerBarController.APM;
    private Timeline glowPulse;
    private AnimationTimer ringAnimationTimer;
    private ParallelTransition rippleEffect;

    @FXML
    public void initialize() {
        // 1. Layout & Clip Setup
        if (root != null) {
            Rectangle clip = new Rectangle(300, 300);
            clip.setArcWidth(300);
            clip.setArcHeight(300);
            root.setClip(clip);

            // Make root focusable
            root.setFocusTraversable(true);

            // Global Key Handler on Root to intercept Spacebar
            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                switch (event.getCode()) {
                    case SPACE:
                        musicManager.playPause();
                        event.consume(); // Prevent button action
                        break;
                    case LEFT:
                        musicManager.previous();
                        event.consume();
                        break;
                    case RIGHT:
                        musicManager.next();
                        event.consume();
                        break;
                }
            });
        }

        // 2. Setup Layout & Animations
        setupDrag();
        setupHoverAnimations();
        setupGlowEffect();
        setupTimerUpdates();
        setupRippleEffect();

        // Bind ambient player state
        if (ambientPlayerManager != null) {
            // Set initial state
            if (ambientPlayerManager.getIsPlaying()) {
                ambientButton.getStyleClass().add("selected");
            } else {
                ambientButton.getStyleClass().remove("selected");
            }

            // Listen for future changes
            ambientPlayerManager.isPlayingProperty().addListener((obs, wasPlaying, isNowPlaying) -> {
                if (isNowPlaying) {
                    ambientButton.getStyleClass().add("selected");
                } else {
                    ambientButton.getStyleClass().remove("selected");
                }
            });
        }


        // Initial State
        root.setScaleX(0.3);
        root.setScaleY(0.3);
        uiContainer.setOpacity(0);
        musicDesign.setOpacity(1);

        bindMusicPlayer();

        // 3. Ensure Buttons aren't focusable (Extra safety)
        setButtonsNotFocusable();
    }

    private void setupRippleEffect() {
        rippleEffect = new ParallelTransition();
        rippleEffect.setCycleCount(Animation.INDEFINITE);

        // Create animations for each circle with a delay
        rippleEffect.getChildren().addAll(
                createRippleAnimation(visualCircle, Duration.ZERO),
                createRippleAnimation(visualCircle2, Duration.seconds(1)),
                createRippleAnimation(visualCircle3, Duration.seconds(2))
        );
    }

    private Animation createRippleAnimation(Circle circle, Duration delay) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(circle.scaleXProperty(), 0.1),
                        new KeyValue(circle.scaleYProperty(), 0.1),
                        new KeyValue(circle.opacityProperty(), 0.0)
                ),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(circle.opacityProperty(), 0.3)
                ),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(circle.scaleXProperty(), 1.5),
                        new KeyValue(circle.scaleYProperty(), 1.5),
                        new KeyValue(circle.opacityProperty(), 0.0)
                )
        );
        timeline.setDelay(delay);
        return timeline;
    }

    private void setButtonsNotFocusable() {
        playPauseButton.setFocusTraversable(false);
        nextButton.setFocusTraversable(false);
        prevButton.setFocusTraversable(false);
        if (ambientButton != null) ambientButton.setFocusTraversable(false);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, 10) + "..." + text.substring(text.length() - 10);
    }

    private void bindMusicPlayer() {
        var song = musicManager.currentSongProperty().get();
        if (song != null) {
            songLabel.setText(truncate(song.fileName, 20));
        } else {
            songLabel.setText("No song playing");
        }
        musicManager.currentSongProperty().addListener((obs, old, val) -> {
            if (val != null) {
                Platform.runLater(() -> songLabel.setText(truncate(val.fileName, 20)));
            } else {
                Platform.runLater(() -> songLabel.setText("No song playing"));
            }
        });
        updatePlayPauseIcon();
        musicManager.isPlayingProperty().addListener((obs, was, now) -> {
            Platform.runLater(() -> {
                updatePlayPauseIcon();
                updateGlowAnimation(now);
                updateVisualAnimation(now);
            });
        });
        updateGlowAnimation(musicManager.isPlayingProperty().get());
        updateVisualAnimation(musicManager.isPlayingProperty().get());
    }

    private void setupTimerUpdates() {
        if (timerProgressRing == null) return;
        timerProgressRing.setRotate(-90);
        timerProgressRing.setStrokeLineCap(StrokeLineCap.ROUND);

        ringAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // FIXED: Use Static Methods from PomodoroController to get live data
                double total = PomodoroController.getLiveTotalDuration();

                if (total > 0 && PomodoroController.isTimerActive()) {
                    double remaining = PomodoroController.getLiveTimeRemaining();
                    updateTimerProgress(remaining, (long) total);
                } else {
                    ringVisualsStack.setVisible(false);
                }
            }
        };
        ringAnimationTimer.start();
    }

    private void updateTimerProgress(double remainingSeconds, long totalDuration) {
        if (timerProgressRing == null) return;
        if (totalDuration <= 0) {
            ringVisualsStack.setVisible(false);
            return;
        }
        // Force visible if active, regardless of hover (or keep your hover logic if you prefer)
        // Your previous code had: ringVisualsStack.setVisible(root.isHover());
        // If you want it always visible when running:
        ringVisualsStack.setVisible(true);

        double progress = 1.0 - (remainingSeconds / (double) totalDuration);
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        timerProgressRing.getStrokeDashArray().setAll(circumference);
        timerProgressRing.setStrokeDashOffset(progress * circumference);
    }

    private void setupDrag() {
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
            root.requestFocus(); // Request focus on click
        });
        root.setOnMouseDragged(e -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    private void setupHoverAnimations() {
        final double unhoveredScale = 0.3;
        final double hoveredScale = 1.0;
        final Duration animationDuration = Duration.millis(300);

        root.setOnMouseEntered(e -> {
            root.requestFocus();
            animateFade(uiContainer, 1.0);
            animateFade(musicDesign, 0.0);

            // Only show ring if timer is actually active/paused
            if (PomodoroController.isTimerActive()) {
                ringVisualsStack.setVisible(true);
            }

            animateScale(root, hoveredScale, animationDuration);
        });

        root.setOnMouseExited(e -> {
            animateFade(uiContainer, 0.0);
            animateFade(musicDesign, 1.0);
            ringVisualsStack.setVisible(false);
            animateScale(root, unhoveredScale, animationDuration);
        });
    }

    private void animateFade(Node node, double targetOpacity) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setToValue(targetOpacity);
        fade.play();
    }

    private void animateScale(Node node, double targetScale, Duration duration) {
        ScaleTransition st = new ScaleTransition(duration, node);
        st.setToX(targetScale);
        st.setToY(targetScale);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void setupGlowEffect() {
        if (glowRing != null) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#8A6FAD"));
            glow.setRadius(25);
            glow.setSpread(0.3);
            glowRing.setEffect(glow);
            double baseRadius = glowRing.getRadius();
            glowPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(glowRing.opacityProperty(), 1.0),
                            new KeyValue(glowRing.radiusProperty(), baseRadius)
                    ),
                    new KeyFrame(Duration.seconds(1.2),
                            new KeyValue(glowRing.opacityProperty(), 0.4),
                            new KeyValue(glowRing.radiusProperty(), baseRadius + 5)
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
        }
    }

    private void updateVisualAnimation(boolean isPlaying) {
        if (rippleEffect == null) return;
        if (isPlaying) {
            rippleEffect.play();
        } else {
            rippleEffect.stop();
            visualCircle.setScaleX(1.0);
            visualCircle.setScaleY(1.0);
            visualCircle.setOpacity(0.15);
            visualCircle2.setOpacity(0.0);
            visualCircle3.setOpacity(0.0);
        }
    }

    // === UI Controls ===
    // Simplified: No manual focus reset needed! Main.java handles it globally.
    @FXML private void playPause() {
        animateButtonClick(playPauseButton, () -> {
            musicManager.playPause();
            root.requestFocus(); // Explicitly return focus to root
        });
    }

    @FXML private void next() {
        animateButtonClick(nextButton, () -> {
            musicManager.next();
            root.requestFocus(); // Explicitly return focus to root
        });
    }

    @FXML private void previous() {
        animateButtonClick(prevButton, () -> {
            musicManager.previous();
            root.requestFocus(); // Explicitly return focus to root
        });
    }

    @FXML private void restoreMain() {
        // Stop the local timer loop when closing
        if (ringAnimationTimer != null) ringAnimationTimer.stop();

        Stage miniStage = (Stage) root.getScene().getWindow();
        miniStage.close();
        if (Main.getRootController() != null && Main.getRootController().getRootPane() != null) {
            Stage mainStage = (Stage) Main.getRootController().getRootPane().getScene().getWindow();
            mainStage.show();
            mainStage.toFront();
        }
    }

    private void animateButtonClick(Button button, Runnable action) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.9); st.setToY(0.9);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setOnFinished(e -> {
            if (action != null) action.run();
        });
        st.play();
    }

    private void updatePlayPauseIcon() {
        if (playPauseIcon == null) return;
        boolean playing = musicManager.isPlayingProperty().get();
        playPauseIcon.setIconLiteral(playing ? "fas-pause" : "fas-play");
        playPauseButton.getStyleClass().removeAll("mini-player-play-button", "mini-player-pause-button");
        playPauseButton.getStyleClass().add(playing ? "mini-player-pause-button" : "mini-player-play-button");
    }

    @FXML
    private void handleAmbientMini() {
        if (ambientPlayerManager == null) return;
        if (!ambientPlayerManager.getIsPlaying()) {
            ambientPlayerManager.playAmbientMusic();
        } else {
            ambientPlayerManager.stopAmbientMusic();
        }
        root.requestFocus(); // Explicitly return focus to root
    }
}