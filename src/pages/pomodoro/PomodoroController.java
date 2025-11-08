package pages.pomodoro;

import com.Main;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.File;
import java.util.Objects;
import static javafx.scene.paint.Color.WHITE;

public class PomodoroController {
    // FXML Component Bindings
    @FXML private Label timerLabel;
    @FXML private Button playButton, pauseButton, stopButton;
    @FXML private StackPane ringVisualsStack;
    @FXML private Circle timerProgressRing;
    @FXML private HBox increaseButtonHBox, decreaseButtonHBox;
    @FXML private Button increaseHourButton, decreaseHourButton, increaseMinuteButton, decreaseMinuteButton, increaseSecondButton, decreaseSecondButton;
    @FXML private VBox taskListVBox;
    @FXML private Button addtask, removetask;
    // STATIC STATE VARIABLES
    private static final int POMODORO_DEFAULT_MINUTES = 0;
    private static int POMODORO_PREV_TIME = 0;
    private static Timeline timeline;
    private static boolean isRunning = false, isPaused = false, isPomodoroSession = true;
    private static int editableHours = 0, editableMinutes = POMODORO_DEFAULT_MINUTES, editableSeconds = 0;
    private static double timeRemaining;
    private static long startTimeMillis;
    private static int sessionDurationSeconds;
    private static AudioClip ringtone;
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String DURATION_KEY = "pomodoro_duration_seconds";
    private static int taskCounter = 1;

    @FXML
    public void initialize() {
        boolean wasRunning = isRunning;
        if (editableMinutes == 0 && !isRunning && !isPaused) {
            int savedDurationSeconds = manageDurationPersistence(null);
            editableHours = savedDurationSeconds / 3600;
            editableMinutes = (savedDurationSeconds % 3600) / 60;
            editableSeconds = savedDurationSeconds % 60; }
        if (!isRunning && !isPaused) { syncEditableTime();}
        else { updateTimerLabel((int) Math.ceil(timeRemaining)); }
        if (ringtone == null) {
            try {
                File audioFile = new File("1_second_tone.mp3");
                String soundPath = audioFile.toURI().toURL().toExternalForm();
                ringtone = new AudioClip(soundPath);
            } catch (Exception e) { System.err.println("Alarm will be silent."); } }
        if (timerProgressRing != null) {
            final double circumference = 2 * Math.PI * timerProgressRing.getRadius();
            timerProgressRing.getStrokeDashArray().setAll(circumference);
            timerProgressRing.setRotate(-90);
            setRingVisible(isRunning || isPaused); }
            updateTimerRingProgress(); updateButtonStates();
        if (wasRunning) { startTimer(timeRemaining); } }

    private void syncEditableTime() {
        int totalEditableSeconds = editableHours * 3600 + editableMinutes * 60 + editableSeconds;
        POMODORO_PREV_TIME  = totalEditableSeconds;
        timeRemaining = totalEditableSeconds;
        sessionDurationSeconds = totalEditableSeconds;
        updateTimerLabel(totalEditableSeconds);
        updateButtonStates(); }

    @FXML
    private void handlePlay(ActionEvent event) {
        if (!isRunning) {
            if (timeRemaining < 0.1) {
                if (isPomodoroSession) syncEditableTime();
                else { timeRemaining = POMODORO_PREV_TIME; sessionDurationSeconds = POMODORO_PREV_TIME; }
                if (timeRemaining < 1) return; }

            startTimer(timeRemaining); isRunning = true; isPaused = false; setRingVisible(true); updateButtonStates(); } }

    @FXML
    private void handlePause(ActionEvent event) {
        if (isRunning) {
            stopTimer();
            isRunning = false; isPaused = true; setRingVisible(true); updateButtonStates(); } }

    @FXML
    private void handleStop(ActionEvent event) {
        stopTimer(); isRunning = false; isPaused = false;
        if (isPomodoroSession) syncEditableTime();
        else {
            timeRemaining = POMODORO_PREV_TIME;
            sessionDurationSeconds = POMODORO_PREV_TIME;
            updateTimerLabel((int)timeRemaining);
        }
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
    public void goToHome(ActionEvent e) throws Exception{
        Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
        Main.getRootController().setPage(home);
    }

    private void startTimer(double timeRemainingAtStartOfRun) {
        if (timeline != null) timeline.stop();
        startTimeMillis = System.currentTimeMillis();

        timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            double elapsedSeconds = (double) elapsedMillis / 1000.0;
            timeRemaining = timeRemainingAtStartOfRun - elapsedSeconds;

            if (timeRemaining <= 0) {
                timeRemaining = 0; timerFinished(); }

            updateTimerLabel((int) Math.ceil(timeRemaining)); updateTimerRingProgress();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); timeline.play(); }

    private void stopTimer() { if (timeline != null) timeline.stop(); }

    private void timerFinished() {
        stopTimer(); isRunning = false; isPaused = false;
        if (ringtone != null) { ringtone.play(); }

        if (isPomodoroSession) {
            isPomodoroSession = false;
            timeRemaining = POMODORO_PREV_TIME;
            sessionDurationSeconds = POMODORO_PREV_TIME; }

        else { isPomodoroSession = true; syncEditableTime(); }
        setRingVisible(false); updateButtonStates(); updateTimerLabel((int) timeRemaining); }

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
        if (playButton != null) playButton.setDisable(isRunning || timerIsEmpty);
        if (pauseButton != null) pauseButton.setDisable(!isRunning);
        if (stopButton != null) stopButton.setDisable(controlsVisible); }

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
    @FXML
    private void addtask(ActionEvent event) {
        String taskText = "New Task " + taskCounter++;
        HBox newTask = createTaskItem(taskText);
        // Add the new task to the VBox inside the ScrollPane
        taskListVBox.getChildren().add(newTask);
    }

    @FXML
    private void removetask(ActionEvent event) {
        // Simple implementation: remove the last task added
        if (!taskListVBox.getChildren().isEmpty()) {
            taskListVBox.getChildren().remove(taskListVBox.getChildren().size() - 1);
            // Decrement the counter if you want tasks to be numbered sequentially again
            if (taskCounter > 1) taskCounter--;
        }
    }

    @FXML
    private void toggleTaskStatus(ActionEvent event) {
        Button statusButton = (Button) event.getSource();
        FontIcon icon = (FontIcon) statusButton.getGraphic();

        // Get the current status (defaults to false if not set)
        boolean isComplete = (boolean) statusButton.getUserData();

        if (isComplete) {
            // Task is now incomplete: switch to empty square
            icon.setIconLiteral("far-square");
            statusButton.setUserData(false);
            // Optional: Change Text color/style of the parent HBox's Text node if needed
        } else {
            // Task is now complete: switch to checkmark
            icon.setIconLiteral("fas-check-square");
            statusButton.setUserData(true);
            // Optional: Change Text color/style to show completion
        }
    }

    /**
     * Helper method to create a single HBox component for a task.
     * @param taskText The descriptive text for the task.
     * @return The complete HBox node.
     */
    private HBox createTaskItem(String taskText) {
        HBox taskBox = new HBox();
        taskBox.setAlignment(Pos.CENTER_LEFT);
        taskBox.setSpacing(10.0);
        taskBox.setPadding(new Insets(5, 5, 5, 5));

        // Task Text
        Text text = new Text(taskText);
        text.setFill(WHITE);
        text.getStyleClass().add("task-text");
        HBox.setHgrow(text, Priority.ALWAYS);

        // Check Button
        FontIcon icon = new FontIcon("far-square");
        icon.setIconSize(20);
        icon.getStyleClass().add("iconli-font-icon");

        Button statusButton = new Button();
        statusButton.setGraphic(icon);
        statusButton.getStyleClass().add("pomodoro-icon-btn");
        statusButton.setOnAction(this::toggleTaskStatus);
        statusButton.setUserData(false); // Stores task status (false = incomplete)

        taskBox.getChildren().addAll(text, statusButton);
        return taskBox;
    }

}
