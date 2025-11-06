package pages.root;

import com.Main;
import com.UserProperties;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.util.Properties;

public class RootPageController {

    @FXML private StackPane root;
    @FXML private ImageView backgroundImage;
    @FXML private StackPane pageContainer;
    @FXML private VBox playerBar;

    UserProperties up = new UserProperties();

    @FXML
    private void initialize() {
        // --- THIS IS THE FIX ---
        // 1. Make the root invisible, so it can be faded in later.
        root.setOpacity(0.0);
        // --- END FIX ---

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

            // Load the home page
            Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));

            // 2. Add the home page (it will be invisible as it's part of 'root
            pageContainer.getChildren().add(home);

            // 3. --- REMOVED FADE-IN LOGIC FROM HERE ---

        } catch (Exception e) {
            e.printStackTrace();
            showError("Application Error", "An unexpected error occurred during initialization: " + e.getMessage());
        }
    }

    // Page Navigation (This part is for page-to-page and is correct)
    public void setPage(Parent node) {
        Parent currentPage = null;
        if (pageContainer.getChildren().size() > 0) {
            currentPage = (Parent) pageContainer.getChildren().get(0);
        }

        if (currentPage != null) {
            FadeTransition fadeout = new FadeTransition(Duration.millis(300), currentPage);
            fadeout.setFromValue(1.0);
            fadeout.setToValue(0.0);
            fadeout.setOnFinished(e -> {
                pageContainer.getChildren().setAll(node);
                node.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeout.play();
        } else {
            pageContainer.getChildren().setAll(node);
            node.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    public void SetBackgroundImage(String path) {
        try {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                backgroundImage.setImage(new Image(imgFile.toURI().toString()));
            } else {
                showError("Image Error", "Could not find the selected image file: " + path);
            }
        } catch (Exception e) {
            // --- FIXING TYPO: Removed stray "D" ---
            showError("Image Error", "An error occurred while setting the background image: " + e.getMessage());
        }
    }

    public void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void hidePlayerBar() {
        if (playerBar != null) {
            playerBar.setVisible(false);
            playerBar.setManaged(false);
        }
    }

    public void showPlayerBar() {
        if (playerBar != null) {
            playerBar.setVisible(true);
            playerBar.setManaged(true);
        }
    }

    public StackPane getPageContainer() {
        return pageContainer;
    }

    public StackPane getRootPane() {
        return root;
    }

}