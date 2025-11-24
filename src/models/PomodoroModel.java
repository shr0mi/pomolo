package models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class PomodoroModel {

    private static PomodoroModel instance;
    private final DoubleProperty remainingSeconds = new SimpleDoubleProperty();
    private long durationInSeconds;

    private PomodoroModel() {
    }

    public static PomodoroModel getInstance() {
        if (instance == null) {
            instance = new PomodoroModel();
        }
        return instance;
    }

    public double getRemainingSeconds() {
        return remainingSeconds.get();
    }

    public void setRemainingSeconds(double remainingSeconds) {
        this.remainingSeconds.set(remainingSeconds);
    }

    public DoubleProperty getRemainingSecondsProperty() {
        return remainingSeconds;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(long durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }
}
