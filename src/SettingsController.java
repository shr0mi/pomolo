// Save as: src/SettingsController.java (OVERWRITE)

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class SettingsController {
    @FXML private AnchorPane rootPane;
    @FXML private Text path_text;

    UserProperties up = new UserProperties();

    @FXML
    private void initialize(){
        try {
            Properties settings = up.loadProperties();
            String imagePath = settings.getProperty("background");

            File img = new File(imagePath);
            if(!img.exists()){
                imagePath = settings.getProperty("default_background");
                up.SetProperties(imagePath);
            }
            path_text.setText(imagePath);
        } catch (IOException e) {
            showError("Properties Error", "Could not load user settings: " + e.getMessage());
        }
    }

    @FXML
    private void chooseBackground(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Background Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if(file != null){
            String imagePath = file.getAbsolutePath();
            try {
                up.SetProperties(imagePath);
                Main.getRootController().SetBackgroundImage(imagePath);
                path_text.setText(imagePath);
            } catch (IOException e) {
                showError("Properties Error", "Could not save new background image setting: " + e.getMessage());
            }
        }
    }

    @FXML
    public void goToHome(ActionEvent e) {
        try {
            Parent home = FXMLLoader.load(getClass().getResource("home.fxml"));
            Main.getRootController().setPage(home);
        } catch (IOException ioException) {
            showError("Navigation Error", "Could not load home page: " + ioException.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}