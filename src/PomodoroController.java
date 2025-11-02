import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.Objects;

public class PomodoroController {
    @FXML private Label timerLabel;
    @FXML private Button startButton;
    @FXML private StackPane ringVisualsStack;
    @FXML private Circle timerProgressRing;
    @FXML private HBox increaseButtonHBox;
    @FXML private HBox decreaseButtonHBox;

    @FXML private Button increaseHourButton;
    @FXML private Button decreaseHourButton;
    @FXML private Button increaseMinuteButton;
    @FXML private Button decreaseMinuteButton;
    @FXML private Button increaseSecondButton;
    @FXML private Button decreaseSecondButton;

    private static final int POMODORO_DEFAULT_MINUTES = 1;
    private static final int SHORT_BREAK_SECONDS = 5 * 60;

    private Timeline timeline;
    private boolean isRunning = false;
    private MediaPlayer mediaPlayer;

    private int editableHours = 0;
    private int editableMinutes = POMODORO_DEFAULT_MINUTES;
    private int editableSeconds = 0;

    private boolean isPomodoroSession = true;

    private double timeRemaining;
    private long startTimeMillis;
    private double timeRemainingAtStartOfRun;
    private int sessionDurationSeconds;

    // --- Initialization ---
    @FXML
    public void initialize() {
        syncEditableTime();
        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            setRingVisible(false);
        }

        // Setup media player
        String RINGTONE_FILE = "1_second_tone.mp3";
        try {
            String audioPath = Objects.requireNonNull(getClass().getResource(RINGTONE_FILE)).toExternalForm();
            Media sound = new Media(audioPath);
            mediaPlayer = new MediaPlayer(sound);
        } catch (Exception e) {
            System.err.println("Could not load ringtone file: " + RINGTONE_FILE);
            mediaPlayer = null;
        }
    }
    private void syncEditableTime() {
        int totalEditableSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;

        if (totalEditableSeconds < 1) {
            editableMinutes = 1;
            editableHours = 0;
            editableSeconds = 0;
            totalEditableSeconds = 60;
        }

        timeRemaining = totalEditableSeconds;
        sessionDurationSeconds = totalEditableSeconds;
        updateTimerLabel(totalEditableSeconds);
    }
    // --- Main Timer Control ---
    @FXML
    private void handleStartStop(ActionEvent event) {
        if (isRunning) {
            // **STOPPING / PAUSING**
            stopTimer();
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            timeRemaining = timeRemainingAtStartOfRun - ((double) elapsedMillis / 1000.0);
            if (timeRemaining < 0) timeRemaining = 0;

            startButton.setText(isPomodoroSession ? "Start Pomodoro" : "Start Break");
            setEditingControlsVisible(true);
            setRingVisible(false);
        } else {
            // **STARTING / RESUMING**
            if (timeRemaining < 0.1) {
                if (isPomodoroSession) {
                    syncEditableTime();
                } else {
                    timeRemaining = SHORT_BREAK_SECONDS;
                    sessionDurationSeconds = SHORT_BREAK_SECONDS;
                }
                if (timeRemaining < 1) return;
            }

            timeRemainingAtStartOfRun = timeRemaining;
            startTimer();
            startButton.setText("Stop");
            setEditingControlsVisible(false);
            setRingVisible(true);
        }
        isRunning = !isRunning;
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();
        startTimeMillis = System.currentTimeMillis();
        timeline = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                    double elapsedSeconds = (double) elapsedMillis / 1000.0;
                    timeRemaining = timeRemainingAtStartOfRun - elapsedSeconds;

                    if (timeRemaining <= 0) {
                        timeRemaining = 0;
                        timerFinished();
                    }

                    updateTimerLabel((int) Math.ceil(timeRemaining));
                    updateTimerRingProgress();
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    private void stopTimer() {
        if (timeline != null) timeline.stop();
    }
    private void timerFinished() {
        stopTimer();
        isRunning = false;
        if (isPomodoroSession) {
            isPomodoroSession = false;
            timeRemaining = SHORT_BREAK_SECONDS;
            sessionDurationSeconds = SHORT_BREAK_SECONDS;
            startButton.setText("Start Break");
        } else {
            isPomodoroSession = true;
            syncEditableTime();
            startButton.setText("Start Pomodoro");
        }

        setEditingControlsVisible(true);
        updateTimerLabel((int) timeRemaining);
        setRingVisible(false);
    }
    private void playRingtone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.play();
        }
    }
    // --- Unified Time Adjustment ---
    @FXML
    private void handleTimeAdjustment(ActionEvent event) {
        if (isRunning) return;
        Button source = (Button) event.getSource();
        String id = source.getId();
        int delta;
        if (id.contains("increase")) {
            delta = 1;
        } else if (id.contains("decrease")) {
            delta = -1;
        } else {
            return;
        }
        if (id.contains("Hour")) {
            editableHours = (editableHours + delta + 24) % 24;
        } else if (id.contains("Minute")) {
            editableMinutes = (editableMinutes + delta + 60) % 60;
        } else if (id.contains("Second")) {
            editableSeconds = (editableSeconds + delta + 60) % 60;
        }
        isPomodoroSession = true;
        startButton.setText("Start Pomodoro");
        // This is the essential call that updates the UI:
        syncEditableTime();
    }
    // --- Utility Methods ---
    private void updateTimerLabel(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        String timeText = String.format("%02d:%02d:%02d", h, m, s);

        if (timerLabel != null) {
            timerLabel.setText(timeText);
        }
    }
    private void updateTimerRingProgress() {
        if (timerProgressRing == null || sessionDurationSeconds == 0) return;

        final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        double fractionElapsed = 1.0 - (timeRemaining / sessionDurationSeconds);

        if (fractionElapsed < 0) fractionElapsed = 0;
        if (fractionElapsed > 1.0) fractionElapsed = 1.0;

        double dashOffset = fractionElapsed * circumference;
        timerProgressRing.setStrokeDashOffset(dashOffset);
    }
    private void setEditingControlsVisible(boolean visible) {
        boolean disabled = !visible;
        if (increaseButtonHBox != null) increaseButtonHBox.setVisible(visible);
        if (decreaseButtonHBox != null) decreaseButtonHBox.setVisible(visible);
        if (increaseMinuteButton != null) increaseMinuteButton.setDisable(disabled);
        if (decreaseMinuteButton != null) decreaseMinuteButton.setDisable(disabled);
        if (increaseHourButton != null) increaseHourButton.setDisable(disabled);
        if (decreaseHourButton != null) decreaseHourButton.setDisable(disabled);
        if (increaseSecondButton != null) increaseSecondButton.setDisable(disabled);
        if (decreaseSecondButton != null) decreaseSecondButton.setDisable(disabled);
    }
    private void setRingVisible(boolean visible) {
        if (ringVisualsStack != null) {
            ringVisualsStack.setVisible(visible);
        }
    }
    @FXML
    public void goToHome(ActionEvent e) throws Exception{
        stopTimer();
        Parent home = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("home.fxml")));
        Main.getRootController().setPage(home);
    }
}
