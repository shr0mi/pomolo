package pages.pomodoro;

import com.Main;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.SequentialTransition;
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
    private static Timeline timeline;
    private static boolean isRunning = false, isPaused = false, isPomodoroSession = true, pomodoroModeActive = false;
    private static int editableHours = 0, editableMinutes = POMODORO_DEFAULT_MINUTES, editableSeconds = 0, previousEditableTimeSeconds = 0;
    private static double timeRemaining;
    private static long startTimeMillis;
    private static int sessionDurationSeconds;
    private static AudioClip ringtone;
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String DURATION_KEY = "pomodoro_duration_seconds";

    private static final String SESSION_KEY_PREFIX = "session.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static int currentDayTotalMinutes = 0;
    private static int currentDaySessionCount = 0;

    private List<DataPoint> lastSevenDaysData = new ArrayList<>();
    private List<String> xAxisCategories = new ArrayList<>();
    private static boolean animationPlayed = false;

    private final PomodoroModel pomodoroModel = PomodoroModel.getInstance();


    @FXML
    public void initialize() {
        if (pomodoroModeActive) {
            editableHours = 0; editableMinutes = 25; editableSeconds = 0;
            pomodoroIcon.setIconLiteral("fas-laptop"); }
        else {
            editableHours = previousEditableTimeSeconds / 3600;
            editableMinutes = (previousEditableTimeSeconds % 3600) / 60;
            editableSeconds = previousEditableTimeSeconds % 60;
            pomodoroIcon.setIconLiteral("fas-clock"); }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE_NAME)) { p.load(in); } catch (Exception ignored) {}
        LocalDate cutoff = LocalDate.now().minusDays(7);
        p.keySet().removeIf(k -> {
            if (k instanceof String && ((String) k).startsWith(SESSION_KEY_PREFIX)) {
                try {
                    LocalDate d = LocalDate.parse(((String) k).substring(SESSION_KEY_PREFIX.length()), DATE_FORMATTER);
                    return d.isBefore(cutoff);
                } catch (Exception ignored) { return false; } }
            return false; });
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE_NAME)) { p.store(out, null); } catch (Exception ignored) {}

        boolean wasRunning = isRunning;
        int[] dayStats = loadCurrentDayStats();
        currentDayTotalMinutes = dayStats[0];
        currentDaySessionCount = dayStats[1];
        updateCurrentDayTimeLabel();
        loadAndDrawStats();
        if (editableMinutes == 0 && !isRunning && !isPaused) {
            int savedDurationSeconds = manageDurationPersistence(null);
            editableHours = savedDurationSeconds / 3600;
            editableMinutes = (savedDurationSeconds % 3600) / 60;
            editableSeconds = savedDurationSeconds % 60; }
        if (!isRunning && !isPaused) syncEditableTime();
        else updateTimerLabel((int) Math.ceil(timeRemaining));
        if (ringtone == null) {
            try {
                File audioFile = new File("1_second_tone.mp3");
                String soundPath = audioFile.toURI().toURL().toExternalForm();
                ringtone = new AudioClip(soundPath);
            } catch (Exception e) {
                System.err.println("Alarm will be silent."); } }
        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            setRingVisible(isRunning || isPaused); }
        updateTimerRingProgress();
        updateButtonStates();
        if (wasRunning) startTimer(timeRemaining);


    }

    private void syncEditableTime() {
        int totalEditableSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        POMODORO_PREV_TIME = totalEditableSeconds;
        timeRemaining = totalEditableSeconds;
        sessionDurationSeconds = totalEditableSeconds;
        pomodoroModel.setDurationInSeconds(sessionDurationSeconds);
        pomodoroModel.setRemainingSeconds(timeRemaining);
        updateTimerLabel(totalEditableSeconds);
        updateButtonStates(); }

    @FXML
    private void handlePlayPauseToggle(ActionEvent event) {
        if (isRunning) {
            stopTimer();
            updateTimerLabel((int) Math.ceil(timeRemaining));
            isRunning = false; isPaused = true;
            playPauseIcon.setIconLiteral("fas-play");
        } else {
            if (timeRemaining < 0.1) {
                if (isPomodoroSession) syncEditableTime();
                else { timeRemaining = POMODORO_PREV_TIME; sessionDurationSeconds = POMODORO_PREV_TIME; }
                if (timeRemaining < 1) return; }
            startTimer(timeRemaining);
            isRunning = true;
            isPaused = false;
            playPauseIcon.setIconLiteral("fas-pause"); }
        setRingVisible(true);
        updateButtonStates(); }

    @FXML
    private void handlePomodoro(ActionEvent event) {
        String currentIcon = pomodoroIcon.getIconLiteral();
        if (currentIcon.equals("fas-clock")) {
            previousEditableTimeSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
            editableHours = 0; editableMinutes = 25; editableSeconds = 0;
            pomodoroIcon.setIconLiteral("fas-laptop");
            pomodoroModeActive = true;
            syncEditableTime(); }
            else {
            editableHours = previousEditableTimeSeconds / 3600;
            editableMinutes = (previousEditableTimeSeconds % 3600) / 60;
            editableSeconds = previousEditableTimeSeconds % 60;
            pomodoroIcon.setIconLiteral("fas-clock");
            pomodoroModeActive = false;
            syncEditableTime(); }
        updateButtonStates(); }
    @FXML
    private void handleStop(ActionEvent event) {
        stopTimer(); isRunning = false; isPaused = false;
        if (isPomodoroSession) syncEditableTime();
        else {
            timeRemaining = POMODORO_PREV_TIME;
            sessionDurationSeconds = POMODORO_PREV_TIME;
            updateTimerLabel((int)timeRemaining); }
        pomodoroModel.setRemainingSeconds(0);
        pomodoroModel.setDurationInSeconds(0);
        setRingVisible(false); updateButtonStates(); }

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
        manageDurationPersistence(newTotalSeconds); }

    @FXML
    public void goToHome(ActionEvent e) throws IOException {
        Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
        Main.getRootController().setPage(home); }

    private void startTimer(double timeRemainingAtStartOfRun) {
        if (timeline != null) timeline.stop();
        startTimeMillis = System.currentTimeMillis();
        pomodoroModel.setDurationInSeconds(sessionDurationSeconds);
        timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            double elapsedSeconds = (double) elapsedMillis / 1000.0;
            timeRemaining = timeRemainingAtStartOfRun - elapsedSeconds;
            if (timeRemaining <= 0) { timeRemaining = 0; timerFinished(); }

            pomodoroModel.setRemainingSeconds(timeRemaining);
            updateTimerLabel((int) Math.ceil(timeRemaining));
            updateTimerRingProgress(); }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play(); }

    private void stopTimer() {
        if (timeline != null) timeline.stop();
        pomodoroModel.setRemainingSeconds(timeRemaining);
    }

    private void timerFinished() {
        stopTimer(); isRunning = false; isPaused = false;
        pomodoroModel.setRemainingSeconds(0);
        pomodoroModel.setDurationInSeconds(0);
        if (ringtone != null) ringtone.play();
        if (isPomodoroSession) {
            int successfulSessionDuration = sessionDurationSeconds;
            currentDayTotalMinutes += successfulSessionDuration / 60;
            currentDaySessionCount++;
            saveCurrentDayStats(currentDayTotalMinutes, currentDaySessionCount);
            notifyStatsUpdate();
            isPomodoroSession = false;
            timeRemaining = POMODORO_PREV_TIME;
            sessionDurationSeconds = POMODORO_PREV_TIME;
        } else {
            isPomodoroSession = true;
            syncEditableTime(); }
        setRingVisible(false);
        updateButtonStates();
        updateTimerLabel((int) timeRemaining); }

    private void updateTimerLabel(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        String timeText = String.format("%02d:%02d:%02d", h, m, s);
        timerLabel.setText(timeText); }

    private void updateTimerRingProgress() {
        if (timerProgressRing == null || sessionDurationSeconds == 0) return;
        final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
        double fractionElapsed = 1.0 - (timeRemaining / sessionDurationSeconds);
        if (fractionElapsed < 0) fractionElapsed = 0;
        if (fractionElapsed > 1.0) fractionElapsed = 1.0;
        timerProgressRing.setStrokeDashOffset(fractionElapsed * circumference); }

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
        if (pomodoroButton != null) pomodoroButton.setDisable(isRunning || isPaused); }

    private void setRingVisible(boolean visible) {
        if (ringVisualsStack != null) {
            ringVisualsStack.setVisible(visible);
            if (timerProgressRing != null) timerProgressRing.setVisible(isRunning || isPaused); } }

    private int manageDurationPersistence(Integer newDuration) {
        Properties prop = new Properties();
        final int defaultDurationSeconds = POMODORO_DEFAULT_MINUTES * 60;
        boolean isLoadOperation = newDuration == null;
        try (FileInputStream input = new FileInputStream(CONFIG_FILE_NAME)) { prop.load(input); }
        catch (IOException ignored) {}
        if (!isLoadOperation) {
            prop.setProperty(DURATION_KEY, String.valueOf(newDuration));
            try (FileOutputStream output = new FileOutputStream(CONFIG_FILE_NAME)) { prop.store(output, null); }
            catch (IOException ignored) { System.err.println("Error saving duration."); } return defaultDurationSeconds; }
        try { String savedDurationStr = prop.getProperty(DURATION_KEY, String.valueOf(defaultDurationSeconds)); return Integer.parseInt(savedDurationStr); }
        catch (NumberFormatException ignored) { return defaultDurationSeconds; } }

    private int[] loadCurrentDayStats() {
        Properties p = new Properties();
        String k = SESSION_KEY_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        int m = 0, s = 0;
        try (FileInputStream in = new FileInputStream(CONFIG_FILE_NAME)) {
            p.load(in);
            String[] parts = p.getProperty(k, "0,0").split(",");
            m = Integer.parseInt(parts[0]);
            s = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception ignored) {}
        return new int[]{m, s}; }

    private void saveCurrentDayStats(int m, int s) {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE_NAME)) { p.load(in); } catch (Exception ignored) {}
        String k = SESSION_KEY_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        p.setProperty(k, m + "," + s);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE_NAME)) {
            LocalDate w = LocalDate.now().minusDays(7);
            p.keySet().removeIf(x -> {
                if (x instanceof String && ((String) x).startsWith(SESSION_KEY_PREFIX)) {
                    try { return LocalDate.parse(((String) x).substring(SESSION_KEY_PREFIX.length()), DATE_FORMATTER).isBefore(w); }
                    catch (Exception e) { return false; } }
                return false; });
            p.store(out, "Pomodoro Daily Statistics");
        } catch (Exception ignored) { System.err.println("Error saving daily stats."); } }

    private void updateCurrentDayTimeLabel() {
        if (currentDayTimeLabel != null) {
            currentDayTimeLabel.setText(String.format(" Time:%dh %dm  Sessions:%d", currentDayTotalMinutes / 60, currentDayTotalMinutes % 60, currentDaySessionCount)); } }

    private void loadAndDrawStats() {
        loadLastSevenDaysData();
        if (pomodoroBarChart == null) return;
        pomodoroBarChart.getData().clear();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        double max = lastSevenDaysData.stream().mapToDouble(d -> d.minutes / 60.0).max().orElse(1.0);
        yAxis.setUpperBound(Math.max(Math.min(max, 12), 1));
        yAxis.setTickUnit(1);
        yAxis.setLabel("");
        yAxis.setTickMarkVisible(false);
        xAxis.setCategories(FXCollections.observableArrayList(lastSevenDaysData.stream().map(d -> d.day).collect(Collectors.toList())));
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
                seq.getChildren().add(new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(bar.YValueProperty(), 0)), new KeyFrame(Duration.millis(dur), new KeyValue(bar.YValueProperty(), t, Interpolator.EASE_OUT)))); }
            seq.setDelay(Duration.millis(500));
            seq.play(); } }

    private void notifyStatsUpdate() {
        updateCurrentDayTimeLabel();
        if (updateIndicator != null) {
            updateIndicator.setText("Updated!");
            new Timeline(new KeyFrame(Duration.seconds(0), new KeyValue(updateIndicator.opacityProperty(), 1.0)), new KeyFrame(Duration.seconds(2), new KeyValue(updateIndicator.opacityProperty(), 0.0))).play(); } }

    private void loadLastSevenDaysData() {
        Properties p = new Properties();
        lastSevenDaysData.clear();
        xAxisCategories.clear();
        LocalDate t = LocalDate.now();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE_NAME)) { p.load(in); } catch (Exception ignored) {}
        for (int i = 7; i >= 1; i--) {
            LocalDate d = t.minusDays(i);
            String k = SESSION_KEY_PREFIX + d.format(DATE_FORMATTER);
            String lbl = d.getDayOfWeek().name().substring(0, 3);
            int m = 0;
            try { m = Integer.parseInt(p.getProperty(k, "0,0").split(",")[0]); } catch (Exception ignored) {}
            lastSevenDaysData.add(new DataPoint(lbl, m));
            xAxisCategories.add(lbl); } }

    private static class DataPoint {
        String day; int minutes;
        DataPoint(String d, int m) { day = d; minutes = m; } }
}
