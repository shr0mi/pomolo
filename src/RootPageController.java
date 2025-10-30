// Save as: src/RootPageController.java (OVERWRITE)

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class RootPageController {
    // FXML fields from RootPage.fxml
    @FXML private StackPane root;
    @FXML private ImageView backgroundImage;

    UserProperties up = new UserProperties();

    @FXML
    private void initialize() {
        // Initialization for Root Page
        try {
            Properties settings = up.loadProperties();
            String imagePath = settings.getProperty("background");
            File img = new File(imagePath);
            if (!img.exists()) {
                imagePath = settings.getProperty("default_background");
                up.SetProperties(imagePath);
                img = new File(imagePath);
            }
            if (img.exists()) {
                backgroundImage.setImage(new Image(img.toURI().toString()));
            } else {
                showError("Image Error", "Could not load background image: " + imagePath);
            }
            // Load the home page by default
            Parent home = FXMLLoader.load(getClass().getResource("home.fxml"));
            // Add home page on top of the background image
            root.getChildren().add(home);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Application Error", "An unexpected error occurred during initialization: " + e.getMessage());
        }
    }

    // Page Navigation
    public void setPage(Parent node) {
        // Find the current page (if it exists) to fade it out
        Parent currentPage = null;
        if (root.getChildren().size() > 1) { // 0 is background, 1 is current page
            currentPage = (Parent) root.getChildren().get(1);
        }

        if (currentPage != null) {
            FadeTransition fadeout = new FadeTransition(Duration.millis(200), currentPage);
            fadeout.setFromValue(1.0);
            fadeout.setToValue(0.0);
            fadeout.setOnFinished(e -> {
                // After fade out, add new page and fade it in
                root.getChildren().setAll(backgroundImage, node); // Add new node on top of background
                node.setOpacity(0.0); // Start new node as transparent
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), node);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeout.play();
        } else {
            // No current page, just add the new one and fade it in
            root.getChildren().setAll(backgroundImage, node);
            node.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), node);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    // Background Image Control
    public void SetBackgroundImage(String path) {
        try {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                backgroundImage.setImage(new Image(imgFile.toURI().toString()));
            } else {
                showError("Image Error", "Could not find the selected image file: " + path);
            }
        } catch (Exception e) {
            showError("Image Error", "An error occurred while setting the background image: " + e.getMessage());
        }
    }

    // Utility Method
    public void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}