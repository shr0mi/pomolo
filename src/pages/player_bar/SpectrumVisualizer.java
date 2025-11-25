package pages.player_bar;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SpectrumVisualizer extends HBox implements AudioSpectrumListener {

    private static final int NUM_BANDS = 60; // Must be an even number for mirroring
    private static final double BAR_WIDTH = 7.0;
    private static final double GAP = 2.0;
    private static final double BAR_MAX_HEIGHT = 40.0;
    private static final Color BAR_COLOR = Color.web("#a481ee", 0.6);
    private static final double MIN_DB_VALUE = -60.0;

    private final Rectangle[] bars;

    public SpectrumVisualizer() {
        this.bars = new Rectangle[NUM_BANDS];
        this.setSpacing(GAP);
        this.setAlignment(Pos.CENTER);
        this.setPrefHeight(BAR_MAX_HEIGHT);

        for (int i = 0; i < NUM_BANDS; i++) {
            bars[i] = new Rectangle(BAR_WIDTH, 1); // Start with a minimal height
            bars[i].setFill(BAR_COLOR);
            bars[i].setArcWidth(0);  // Sharp corners for pixel-art look
            bars[i].setArcHeight(0); // Sharp corners for pixel-art look
        }
        // Add bars to the HBox in the correct order for the mirrored layout
        for (int i = (NUM_BANDS / 2) - 1; i >= 0; i--) {
            this.getChildren().add(bars[i]);
        }
        for (int i = (NUM_BANDS / 2) - 1; i >= 0; i--) {
            this.getChildren().add(bars[NUM_BANDS - 1 - i]);
        }
    }

    @Override
    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
        if (magnitudes.length < NUM_BANDS / 2) {
            return; // Not enough data
        }

        // Process the first half of the bands for the mirrored effect
        for (int i = 0; i < NUM_BANDS / 2; i++) {
            double dbValue = magnitudes[i] - MIN_DB_VALUE;
            double magnitude = (dbValue < 0) ? 0 : dbValue;
            double barHeight = (magnitude / -MIN_DB_VALUE) * BAR_MAX_HEIGHT;

            // Clamp height
            if (barHeight <= 0) barHeight = 1; // Ensure a minimal bar is always visible
            if (barHeight > BAR_MAX_HEIGHT) barHeight = BAR_MAX_HEIGHT;

            // Update the center-out pairs
            bars[i].setHeight(barHeight);
            bars[NUM_BANDS - 1 - i].setHeight(barHeight);
        }
    }
}
