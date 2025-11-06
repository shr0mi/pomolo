package pages.confirmation_dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmationDialogController {

    @FXML
    private Label messageLabel;

    private Stage dialogStage;
    private boolean confirmed = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleYes() {
        confirmed = true;
        dialogStage.close();
    }

    @FXML
    private void handleNo() {
        confirmed = false;
        dialogStage.close();
    }
}
