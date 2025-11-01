import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;

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
    private void minimize() {
        Stage stage = (Stage) toolBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void close() {
        Platform.exit();
    }
}
