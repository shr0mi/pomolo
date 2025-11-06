package pages.custom_title_bar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class CustomTitleBarController {

    @FXML
    private ToolBar toolBar;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void initialize() {
        toolBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        toolBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) toolBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    public void minimize() {
        try {
            // Hide the main window
            Stage currentStage = (Stage) toolBar.getScene().getWindow();
            currentStage.hide();

            // Load miniPlayer.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pages/mini_player/miniPlayer.fxml"));
            Parent miniRoot = loader.load();

            // --- Make Scene transparent so circle shape can show cleanly ---
            Scene miniScene = new Scene(miniRoot, 300, 300);
            miniScene.setFill(javafx.scene.paint.Color.TRANSPARENT); // ✅ Transparent background

            Stage miniStage = new Stage();
            miniStage.setScene(miniScene);
            miniStage.initStyle(StageStyle.TRANSPARENT); // ✅ No borders, allows custom shapes
            miniStage.setResizable(false);
            miniStage.setTitle("Pomolo Mini Player");

            // Optional: make it always float above other windows
            miniStage.setAlwaysOnTop(true);

            // When mini window closes, restore the main window
            miniStage.setOnCloseRequest(event -> currentStage.show());

            miniStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void close() {
        Platform.exit();
    }
}
