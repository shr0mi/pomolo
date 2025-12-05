package pages.pomodoro;

import com.Main;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import models.PomodoroModel;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PomodoroController {
    @FXML private Label timerLabel;
    @FXML private Button togglePlayPauseButton, stopButton, pomodoroButton;
    @FXML private FontIcon playPauseIcon, pomodoroIcon;
    @FXML private StackPane ringVisualsStack;
    @FXML private Circle timerProgressRing;
    @FXML private HBox increaseButtonHBox, decreaseButtonHBox;
    @FXML private Button increaseHourButton, decreaseHourButton, increaseMinuteButton, decreaseMinuteButton, increaseSecondButton, decreaseSecondButton;
    @FXML private Label currentDayTimeLabel;
    @FXML private Label updateIndicator;
    @FXML private BarChart<String, Number> pomodoroBarChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private static final int POMODORO_DEFAULT_MINUTES = 0;
    private static int POMODORO_PREV_TIME = 0;
    // timeline handles VISUAL updates only (stopped when leaving page)
    private static Timeline timeline;
    // Timer for LOGIC checks (keeps running in background)
    private static Timeline logicTimer;

    // --- STATE VARIABLES (Static for global access) ---
    private static boolean isRunning = false, isPaused = false, isPomodoroSession = true, pomodoroModeActive = false;
    private static int editableHours = 0, editableMinutes = POMODORO_DEFAULT_MINUTES, editableSeconds = 0, previousEditableTimeSeconds = 0;
    private static double timeRemaining;
    private static int sessionDurationSeconds;
    private static long targetEndTimeMillis = 0; // The source of truth for time

    private static AudioClip ringtone;
    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String CONFIG_FILE_NAME = APP_DIR + File.separator + "pomodoro.properties";;
    private static final String DURATION_KEY = "pomodoro_duration_seconds";

    private static final String SESSION_KEY_PREFIX = "session.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static int currentDayTotalMinutes = 0;
    private static int currentDaySessionCount = 0;

    private List<DataPoint> lastSevenDaysData = new ArrayList<>();
    private List<String> xAxisCategories = new ArrayList<>();
    private static boolean animationPlayed = false;

    private final PomodoroModel pomodoroModel = PomodoroModel.getInstance();
    private Properties cachedProperties = new Properties();

    @FXML
    public void initialize() {
        if(togglePlayPauseButton != null) togglePlayPauseButton.setDisable(true);

        if (pomodoroModeActive) {
            editableHours = 0; editableMinutes = 25; editableSeconds = 0;
            pomodoroIcon.setIconLiteral("fas-laptop");
        } else {
            editableHours = previousEditableTimeSeconds / 3600;
            editableMinutes = (previousEditableTimeSeconds % 3600) / 60;
            editableSeconds = previousEditableTimeSeconds % 60;
            pomodoroIcon.setIconLiteral("fas-clock");
        }

        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            setRingVisible(isRunning || isPaused);
        }

        new Thread(() -> {
            loadPropertiesFromFile();
            boolean saveNeeded = cleanupOldData();
            if (saveNeeded) savePropertiesToFile();

            if (ringtone == null) {
                try {
                    File audioFile = new File("1_second_tone.mp3");
                    if (audioFile.exists()) {
                        String soundPath = audioFile.toURI().toURL().toExternalForm();
                        ringtone = new AudioClip(soundPath);
                    }
                } catch (Exception e) {
                    System.err.println("Alarm will be silent: " + e.getMessage());
                }
            }

            // Always load freshest stats on initialize
            int[] dayStats = loadCurrentDayStats();
            currentDayTotalMinutes = dayStats[0];
            currentDaySessionCount = dayStats[1];

            prepareLastSevenDaysData();

            int savedDurationSeconds = 0;
            if (editableMinutes == 0 && !isRunning && !isPaused) {
                savedDurationSeconds = manageDurationPersistence(null);
            }

            int finalSavedDuration = savedDurationSeconds;
            Platform.runLater(() -> {
                updateCurrentDayTimeLabel();
                drawChart();

                if (editableMinutes == 0 && !isRunning && !isPaused) {
                    editableHours = finalSavedDuration / 3600;
                    editableMinutes = (finalSavedDuration % 3600) / 60;
                    editableSeconds = finalSavedDuration % 60;
                }

                // Resume logic
                if (isRunning) {
                    long now = System.currentTimeMillis();
                    timeRemaining = (targetEndTimeMillis - now) / 1000.0;

                    if (timeRemaining <= 0) {
                        // Timer finished while away. Stats already saved by background timer.
                        // Reset UI state.
                        timeRemaining = 0;
                        isRunning = false;
                        isPaused = false;

                        // Ensure UI reflects the reset state set by timerFinished()
                        if (isPomodoroSession) {
                            syncEditableTime();
                        }

                        setRingVisible(false);
                        updateButtonStates();
                        updateTimerLabel((int) timeRemaining);
                    } else {
                        startTimer();
                        startLogicTimer();
                        playPauseIcon.setIconLiteral("fas-pause");
                    }
                } else {
                    if (!isPaused) syncEditableTime();
                    else {
                        updateTimerLabel((int) Math.ceil(timeRemaining));
                        playPauseIcon.setIconLiteral("fas-play");
                    }
                }

                updateTimerRingProgress();
                updateButtonStates();
            });

        }).start();
    }

    public void shutdown() {
        if (timeline != null) {
            timeline.stop(); // Stop visual updates
        }
        // logicTimer continues running in background
    }

    private void startLogicTimer() {
        if (logicTimer != null) logicTimer.stop();
        // Check once per second if the time is up
        logicTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isRunning) return;
            long now = System.currentTimeMillis();
            if (now >= targetEndTimeMillis) {
                timerFinished();
            }
        }));
        logicTimer.setCycleCount(Timeline.INDEFINITE);
        logicTimer.play();
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();

        pomodoroModel.setDurationInSeconds(sessionDurationSeconds);

        timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long now = System.currentTimeMillis();
            timeRemaining = (targetEndTimeMillis - now) / 1000.0;

            if (timeRemaining <= 0) timeRemaining = 0;

            pomodoroModel.setRemainingSeconds(timeRemaining);
            updateTimerLabel((int) Math.ceil(timeRemaining));
            updateTimerRingProgress();
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handlePlayPauseToggle(ActionEvent event) {
        if (isRunning) {
            stopTimer();
            updateTimerLabel((int) Math.ceil(timeRemaining));
            isRunning = false;
            isPaused = true;
            playPauseIcon.setIconLiteral("fas-play");
        } else {
            if (timeRemaining < 0.1) {
                if (isPomodoroSession) syncEditableTime();
                else { timeRemaining = POMODORO_PREV_TIME; sessionDurationSeconds = POMODORO_PREV_TIME; }
                if (timeRemaining < 1) return;
            }

            targetEndTimeMillis = System.currentTimeMillis() + (long)(timeRemaining * 1000);

            startTimer(); // Visuals
            startLogicTimer(); // Background logic
            isRunning = true;
            isPaused = false;
            playPauseIcon.setIconLiteral("fas-pause");
        }
        setRingVisible(true);
        updateButtonStates();
    }

    // --- ACCESSORS FOR MINIPLAYER ---
    public static double getLiveTimeRemaining() {
        if (!isRunning) return timeRemaining;
        long now = System.currentTimeMillis();
        double remaining = (targetEndTimeMillis - now) / 1000.0;
        return Math.max(0, remaining);
    }

    public static double getLiveTotalDuration() {
        return sessionDurationSeconds;
    }

    public static boolean isTimerActive() {
        return isRunning || isPaused;
    }

    // --- Helper Methods ---

    private void loadPropertiesFromFile() {
        try (FileInputStream in = new FileInputStream(CONFIG_FILE_NAME)) {
            cachedProperties.load(in);
        } catch (Exception ignored) {}
    }

    private void savePropertiesToFile() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE_NAME)) {
            cachedProperties.store(out, null);
        } catch (Exception ignored) {}
    }

    private boolean cleanupOldData() {
        boolean changed = false;
        LocalDate cutoff = LocalDate.now().minusDays(7);
        Iterator<Object> it = cachedProperties.keySet().iterator();
        while (it.hasNext()) {
            Object k = it.next();
            if (k instanceof String && ((String) k).startsWith(SESSION_KEY_PREFIX)) {
                try {
                    LocalDate d = LocalDate.parse(((String) k).substring(SESSION_KEY_PREFIX.length()), DATE_FORMATTER);
                    if (d.isBefore(cutoff)) {
                        it.remove();
                        changed = true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return changed;
    }

    private int manageDurationPersistence(Integer newDuration) {
        final int defaultDurationSeconds = POMODORO_DEFAULT_MINUTES * 60;
        if (newDuration == null) {
            String val = cachedProperties.getProperty(DURATION_KEY, String.valueOf(defaultDurationSeconds));
            try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultDurationSeconds; }
        } else {
            cachedProperties.setProperty(DURATION_KEY, String.valueOf(newDuration));
            savePropertiesToFile();
            return defaultDurationSeconds;
        }
    }

    private int[] loadCurrentDayStats() {
        String k = SESSION_KEY_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        int m = 0, s = 0;
        try {
            String val = cachedProperties.getProperty(k, "0,0");
            String[] parts = val.split(",");
            m = Integer.parseInt(parts[0]);
            s = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception ignored) {}
        return new int[]{m, s};
    }

    // --- SAFE STATS SAVING ---
    private void incrementAndSaveStats(int minutesToAdd) {
        // 1. Disk Operations (Always Safe)
        int[] freshStats = loadCurrentDayStats();
        int newTotalMinutes = freshStats[0] + minutesToAdd;
        int newSessionCount = freshStats[1] + 1;

        String k = SESSION_KEY_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        cachedProperties.setProperty(k, newTotalMinutes + "," + newSessionCount);
        savePropertiesToFile();

        // 2. UI Updates (Must check if UI exists)
        Platform.runLater(() -> {
            // Update static vars
            currentDayTotalMinutes = newTotalMinutes;
            currentDaySessionCount = newSessionCount;

            // Only update labels if they are actively part of the scene
            if (currentDayTimeLabel != null && currentDayTimeLabel.getScene() != null) {
                notifyStatsUpdate();

                // Redraw the graph
                loadAndDrawStats();
            }
        });
    }

    private void prepareLastSevenDaysData() {
        lastSevenDaysData.clear();
        xAxisCategories.clear();
        LocalDate t = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = t.minusDays(i);
            String k = SESSION_KEY_PREFIX + d.format(DATE_FORMATTER);
            String lbl = d.getDayOfWeek().name().substring(0, 3);
            int m = 0;
            try {
                String val = cachedProperties.getProperty(k, "0,0");
                m = Integer.parseInt(val.split(",")[0]);
            } catch (Exception ignored) {}
            lastSevenDaysData.add(new DataPoint(lbl, m));
            xAxisCategories.add(lbl);
        }
    }

    private void drawChart() {
        if (pomodoroBarChart == null) return;
        pomodoroBarChart.getData().clear();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        double max = lastSevenDaysData.stream().mapToDouble(d -> d.minutes / 60.0).max().orElse(1.0);
        yAxis.setUpperBound(Math.max(Math.min(max, 12), 1));
        yAxis.setTickUnit(1);
        yAxis.setLabel("");
        yAxis.setTickMarkVisible(false);
        xAxis.setCategories(FXCollections.observableArrayList(xAxisCategories));
        xAxis.setTickMarkVisible(false);
        xAxis.setLabel("");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Double> vals = lastSevenDaysData.stream().map(d -> d.minutes / 60.0).collect(Collectors.toList());
        for (int i = 0; i < lastSevenDaysData.size(); i++) series.getData().add(new XYChart.Data<>(lastSevenDaysData.get(i).day, animationPlayed ? vals.get(i) : 0));


        pomodoroBarChart.getData().add(series);
        if (!animationPlayed) {
            animationPlayed = true;
            SequentialTransition seq = new SequentialTransition();
            double spd = 0.5;
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> bar = series.getData().get(i);
                double t = vals.get(i), dur = Math.max(t / spd, 50);
                seq.getChildren().add(new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(bar.YValueProperty(), 0)), new KeyFrame(Duration.millis(dur), new KeyValue(bar.YValueProperty(), t, Interpolator.EASE_OUT))));
            }
            seq.setDelay(Duration.millis(500));
            seq.play();
        }
    }

    private void drawChartWithAnimation() {
        if (pomodoroBarChart == null || pomodoroBarChart.getData().isEmpty()) return;

        // Get the current series (already drawn on chart)
        XYChart.Series<String, Number> series = pomodoroBarChart.getData().get(0);

        // Find today's bar (last bar in the series, index 6)
        int todayIndex = lastSevenDaysData.size() - 1;
        XYChart.Data<String, Number> todayBar = series.getData().get(todayIndex);

        // Get the current value and target value
        double currentValue = todayBar.YValueProperty().get().doubleValue();
        double targetValue = lastSevenDaysData.get(todayIndex).minutes / 60.0;

        // Animate only if there's a change
        if (Math.abs(targetValue - currentValue) > 0.01) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(todayBar.YValueProperty(), currentValue)),
                    new KeyFrame(Duration.millis(300), new KeyValue(todayBar.YValueProperty(), targetValue, Interpolator.EASE_OUT))
            );
            timeline.play();
        }
    }

    private void loadAndDrawStats() {
        prepareLastSevenDaysData();
        drawChartWithAnimation();
    }

    private void syncEditableTime() {
        int totalEditableSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        POMODORO_PREV_TIME = totalEditableSeconds;
        timeRemaining = totalEditableSeconds;
        sessionDurationSeconds = totalEditableSeconds;
        pomodoroModel.setDurationInSeconds(sessionDurationSeconds);
        pomodoroModel.setRemainingSeconds(timeRemaining);
        updateTimerLabel(totalEditableSeconds);
        updateButtonStates();
    }

    @FXML
    private void handlePomodoro(ActionEvent event) {
        String currentIcon = pomodoroIcon.getIconLiteral();
        if (currentIcon.equals("fas-clock")) {
            previousEditableTimeSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
            editableHours = 0; editableMinutes = 25; editableSeconds = 0;
            pomodoroIcon.setIconLiteral("fas-laptop");
            pomodoroModeActive = true;
            syncEditableTime();
        } else {
            editableHours = previousEditableTimeSeconds / 3600;
            editableMinutes = (previousEditableTimeSeconds % 3600) / 60;
            editableSeconds = previousEditableTimeSeconds % 60;
            pomodoroIcon.setIconLiteral("fas-clock");
            pomodoroModeActive = false;
            syncEditableTime();
        }
        updateButtonStates();
    }

    @FXML
    private void handleStop(ActionEvent event) {
        stopTimer();
        isRunning = false;
        isPaused = false;

        if (isPomodoroSession) syncEditableTime();
        else {
            timeRemaining = POMODORO_PREV_TIME;
            sessionDurationSeconds = POMODORO_PREV_TIME;
            updateTimerLabel((int)timeRemaining);
        }
        pomodoroModel.setRemainingSeconds(0);
        pomodoroModel.setDurationInSeconds(0);
        setRingVisible(false); updateButtonStates();
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
        syncEditableTime(); updateButtonStates();
        int newTotalSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        manageDurationPersistence(newTotalSeconds);
    }

    @FXML
    public void goToHome(ActionEvent e) throws IOException {
        Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
        Main.getRootController().setPage(home);
    }

    private void stopTimer() {
        // Stop both visual and logic timers
        if (timeline != null) timeline.stop();
        if (logicTimer != null) logicTimer.stop();
        pomodoroModel.setRemainingSeconds(timeRemaining);
    }

    // --- UPDATED TIMER FINISHED LOGIC WITH SAFETY CHECKS ---
    private void timerFinished() {
        stopTimer();
        isRunning = false;
        isPaused = false;
        pomodoroModel.setRemainingSeconds(0);
        pomodoroModel.setDurationInSeconds(0);
        if (ringtone != null) ringtone.play();

        // FIX: Always count the session if it was a work session.
        if (isPomodoroSession) {
            int finishedMinutes = sessionDurationSeconds / 60;
            incrementAndSaveStats(finishedMinutes);
        }

        // FIX: ALWAYS reset state to ready for a new Pomodoro session.
        // This ensures the next click starts a work session that counts.
        isPomodoroSession = true;
        syncEditableTime();

        // Ensure UI updates happen on FX thread AND check if UI exists
        Platform.runLater(() -> {
            if (timerLabel != null && timerLabel.getScene() != null) {
                setRingVisible(false);
                updateButtonStates();
                updateTimerLabel((int) timeRemaining);
            }
        });
    }

    private void updateTimerLabel(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        String timeText = String.format("%02d:%02d:%02d", h, m, s);
        timerLabel.setText(timeText);
    }

    private void updateTimerRingProgress() {
        if (timerProgressRing == null || sessionDurationSeconds == 0) return;
        final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        double fractionElapsed = 1.0 - (timeRemaining / sessionDurationSeconds);
        if (fractionElapsed < 0) fractionElapsed = 0;
        if (fractionElapsed > 1.0) fractionElapsed = 1.0;
        timerProgressRing.setStrokeDashOffset(fractionElapsed * circumference);
    }

    private void updateButtonStates() {
        boolean controlsVisible = !isRunning && !isPaused;
        Button[] adjustmentButtons = { increaseHourButton, decreaseHourButton, increaseMinuteButton, decreaseMinuteButton, increaseSecondButton, decreaseSecondButton };
        if (increaseButtonHBox != null) increaseButtonHBox.setVisible(controlsVisible);
        if (decreaseButtonHBox != null) decreaseButtonHBox.setVisible(controlsVisible);
        for (Button btn : adjustmentButtons) { if (btn != null) { btn.setDisable(!controlsVisible); } }
        boolean timerIsEmpty = timeRemaining <= 0;
        if (togglePlayPauseButton != null) {
            togglePlayPauseButton.setDisable(timerIsEmpty && !isRunning && !isPaused);
            if (playPauseIcon != null) { if (isRunning) { playPauseIcon.setIconLiteral("fas-pause"); }
            else { playPauseIcon.setIconLiteral("fas-play"); } } }
        if (stopButton != null) stopButton.setDisable(controlsVisible);
        if (pomodoroButton != null) pomodoroButton.setDisable(isRunning || isPaused);
    }

    private void setRingVisible(boolean visible) {
        if (ringVisualsStack != null) {
            ringVisualsStack.setVisible(visible);
            if (timerProgressRing != null) timerProgressRing.setVisible(isRunning || isPaused);
        }
    }

    private void updateCurrentDayTimeLabel() {
        if (currentDayTimeLabel != null) {
            currentDayTimeLabel.setText(String.format("Time: %dh %dm | Sessions: %d", currentDayTotalMinutes / 60, currentDayTotalMinutes % 60, currentDaySessionCount));
        }
    }

    private void notifyStatsUpdate() {
        updateCurrentDayTimeLabel();
//        if (updateIndicator != null) {
//            updateIndicator.setText("Updated!");
//            new Timeline(new KeyFrame(Duration.seconds(0), new KeyValue(updateIndicator.opacityProperty(), 1.0)), new KeyFrame(Duration.seconds(2), new KeyValue(updateIndicator.opacityProperty(), 0.0))).play();
//        }
    }

    private static class DataPoint {
        String day; int minutes;
        DataPoint(String d, int m) { day = d; minutes = m; }
    }
}