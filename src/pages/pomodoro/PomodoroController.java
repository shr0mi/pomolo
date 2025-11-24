package pages.pomodoro;

import com.Main;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.PomodoroModel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.File;

public class PomodoroController {
    // FXML Component Bindings
    @FXML private Label timerLabel;
    @FXML private Button playButton, pauseButton, stopButton;
    @FXML private StackPane ringVisualsStack;
    @FXML private Circle timerProgressRing;
    @FXML private HBox increaseButtonHBox, decreaseButtonHBox;
    @FXML private Button increaseHourButton, decreaseHourButton, increaseMinuteButton, decreaseMinuteButton, increaseSecondButton, decreaseSecondButton;

    private PomodoroModel pomodoroModel;
    private Timeline timeline;
    private boolean isRunning = false, isPaused = false, isPomodoroSession = true;
    private int editableHours = 0, editableMinutes = 0, editableSeconds = 0;
    private long startTimeMillis;
    private AudioClip ringtone;
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String DURATION_KEY = "pomodoro_duration_seconds";

    @FXML
    public void initialize() {
        pomodoroModel = PomodoroModel.getInstance();
        boolean wasRunning = isRunning;
        if (editableMinutes == 0 && !isRunning && !isPaused) {
            int savedDurationSeconds = manageDurationPersistence(null);
            editableHours = savedDurationSeconds / 3600;
            editableMinutes = (savedDurationSeconds % 3600) / 60;
            editableSeconds = savedDurationSeconds % 60;
        }
        if (!isRunning && !isPaused) {
            syncEditableTime();
        } else {
            updateTimerLabel((int) Math.ceil(pomodoroModel.getRemainingSeconds()));
        }
        if (ringtone == null) {
            try {
                File audioFile = new File("1_second_tone.mp3");
                String soundPath = audioFile.toURI().toURL().toExternalForm();
                ringtone = new AudioClip(soundPath);
            } catch (Exception e) {
                System.err.println("Alarm will be silent.");
            }
        }
        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            setRingVisible(isRunning || isPaused);
        }
        updateTimerRingProgress();
        updateButtonStates();
        if (wasRunning) {
            startTimer(pomodoroModel.getRemainingSeconds());
        }
    }

    private void syncEditableTime() {
        int totalEditableSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        pomodoroModel.setDurationInSeconds(totalEditableSeconds);
        pomodoroModel.setRemainingSeconds(totalEditableSeconds);
        updateTimerLabel(totalEditableSeconds);
        updateButtonStates();
    }

    @FXML
    private void handlePlay(ActionEvent event) {
        if (!isRunning) {
            if (pomodoroModel.getRemainingSeconds() < 0.1) {
                if (isPomodoroSession) {
                    syncEditableTime();
                } else {
                    pomodoroModel.setRemainingSeconds(pomodoroModel.getDurationInSeconds());
                }
                if (pomodoroModel.getRemainingSeconds() < 1) return;
            }

            startTimer(pomodoroModel.getRemainingSeconds());
            isRunning = true;
            isPaused = false;
            setRingVisible(true);
            updateButtonStates();
        }
    }

    @FXML
    private void handlePause(ActionEvent event) {
        if (isRunning) {
            stopTimer();
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            pomodoroModel.setRemainingSeconds(pomodoroModel.getRemainingSeconds() - ((double) elapsedMillis / 1000.0));
            if (pomodoroModel.getRemainingSeconds() < 0) pomodoroModel.setRemainingSeconds(0);
            isRunning = false;
            isPaused = true;
            setRingVisible(true);
            updateButtonStates();
        }
    }

    @FXML
    private void handleStop(ActionEvent event) {
        stopTimer();
        isRunning = false;
        isPaused = false;
        if (isPomodoroSession) {
            syncEditableTime();
        } else {
            pomodoroModel.setRemainingSeconds(pomodoroModel.getDurationInSeconds());
            updateTimerLabel((int) pomodoroModel.getRemainingSeconds());
        }
        setRingVisible(false);
        updateButtonStates();
    }

    @FXML
    private void handleTimeAdjustment(ActionEvent event) {
        if (isRunning || isPaused) return;
        Button source = (Button) event.getSource();
        String id = source.getId();
        int delta = id.contains("increase") ? 1 : -1;

        if (id.contains("Hour")) editableHours = (editableHours + delta + 24) % 24;
        else if (id.contains("Minute")) editableMinutes = (editableMinutes + delta + 60) % 60;
        else if (id.contains("Second")) editableSeconds = (editableSeconds + delta + 60) % 60;

        isPomodoroSession = true;
        syncEditableTime();
        updateButtonStates();
        int newTotalSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        manageDurationPersistence(newTotalSeconds);
    }

    @FXML
    public void goToHome(ActionEvent e) throws Exception {
        Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
        Main.getRootController().setPage(home);
        Stage mainStage = (Stage) Main.getRootController().getRootPane().getScene().getWindow();
        mainStage.show();
    }

    @FXML
    private void showMiniPlayer() {
        try {
            Parent miniPlayerRoot = FXMLLoader.load(Main.class.getResource("/pages/mini_player/miniPlayer.fxml"));
            // Open mini player in its own stage (don't rely on Main.showMiniPlayer here)
            Stage miniStage = new Stage();
            // Use a fixed scene size that matches the mini player FXML pref size to avoid overflow when scaling
            Scene miniScene = new Scene(miniPlayerRoot, 300, 300);
             try {
                 var css = Main.class.getResource("/css/dark-theme.css");
                 if (css != null) miniScene.getStylesheets().add(css.toExternalForm());
             } catch (Exception ignored) {
             }
             miniStage.initStyle(StageStyle.UNDECORATED);
             miniStage.setResizable(false);
             miniStage.setAlwaysOnTop(true);
             miniStage.setScene(miniScene);
             miniStage.show();


            // Hide the main window
            Stage mainStage = (Stage) timerLabel.getScene().getWindow();
            mainStage.hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startTimer(double timeRemainingAtStartOfRun) {
        if (timeline != null) timeline.stop();
        startTimeMillis = System.currentTimeMillis();

        timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            double elapsedSeconds = (double) elapsedMillis / 1000.0;
            pomodoroModel.setRemainingSeconds(timeRemainingAtStartOfRun - elapsedSeconds);

            if (pomodoroModel.getRemainingSeconds() <= 0) {
                pomodoroModel.setRemainingSeconds(0);
                timerFinished();
            }

            updateTimerLabel((int) Math.ceil(pomodoroModel.getRemainingSeconds()));
            updateTimerRingProgress();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimer() {
        if (timeline != null) timeline.stop();
    }

    private void timerFinished() {
        stopTimer();
        isRunning = false;
        isPaused = false;
        if (ringtone != null) {
            ringtone.play();
        }

        if (isPomodoroSession) {
            isPomodoroSession = false;
            pomodoroModel.setRemainingSeconds(pomodoroModel.getDurationInSeconds());
        } else {
            isPomodoroSession = true;
            syncEditableTime();
        }
        setRingVisible(false);
        updateButtonStates();
        updateTimerLabel((int) pomodoroModel.getRemainingSeconds());
    }

    private void updateTimerLabel(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        String timeText = String.format("%02d:%02d:%02d", h, m, s);
        timerLabel.setText(timeText);
    }

    private void updateTimerRingProgress() {
        if (timerProgressRing == null || pomodoroModel.getDurationInSeconds() == 0) return;
        final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        double fractionElapsed = 1.0 - (pomodoroModel.getRemainingSeconds() / pomodoroModel.getDurationInSeconds());
        if (fractionElapsed < 0) fractionElapsed = 0;
        if (fractionElapsed > 1.0) fractionElapsed = 1.0;
        timerProgressRing.setStrokeDashOffset(fractionElapsed * circumference);
    }

    private void updateButtonStates() {
        boolean controlsVisible = !isRunning && !isPaused;
        Button[] adjustmentButtons = {increaseHourButton, decreaseHourButton, increaseMinuteButton, decreaseMinuteButton, increaseSecondButton, decreaseSecondButton};

        if (increaseButtonHBox != null) increaseButtonHBox.setVisible(controlsVisible);
        if (decreaseButtonHBox != null) decreaseButtonHBox.setVisible(controlsVisible);

        for (Button btn : adjustmentButtons) {
            if (btn != null) {
                btn.setDisable(!controlsVisible);
            }
        }
        boolean timerIsEmpty = pomodoroModel.getRemainingSeconds() <= 0;
        if (playButton != null) playButton.setDisable(isRunning || timerIsEmpty);
        if (pauseButton != null) pauseButton.setDisable(!isRunning);
        if (stopButton != null) stopButton.setDisable(controlsVisible);
    }

    private void setRingVisible(boolean visible) {
        if (ringVisualsStack != null) {
            ringVisualsStack.setVisible(visible);
            if (timerProgressRing != null) timerProgressRing.setVisible(isRunning || isPaused);
        }
    }

    private int manageDurationPersistence(Integer newDuration) {
        Properties prop = new Properties();
        // default to 25 minutes to match UI default and expected Pomodoro duration
        final int defaultDurationSeconds = 25 * 60;
        boolean isLoadOperation = newDuration == null;
        try (FileInputStream input = new FileInputStream(CONFIG_FILE_NAME)) {
            prop.load(input);
        } catch (IOException ignored) {
        }
        if (!isLoadOperation) {
            prop.setProperty(DURATION_KEY, String.valueOf(newDuration));
            try (FileOutputStream output = new FileOutputStream(CONFIG_FILE_NAME)) {
                prop.store(output, null);
            } catch (IOException ignored) {
                System.err.println("Error saving duration.");
            }
            // Return the value that was just saved
            return newDuration;
        }

        try {
            String savedDurationStr = prop.getProperty(DURATION_KEY, String.valueOf(defaultDurationSeconds));
            return Integer.parseInt(savedDurationStr);
        } catch (NumberFormatException ignored) {
            return defaultDurationSeconds;
        }
    }
}
