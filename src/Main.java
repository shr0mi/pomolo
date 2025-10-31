import javafx.animation.FadeTransition; // Import FadeTransition
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration; // Import Duration

public class Main extends Application {
    public static RootPageController rootController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/rootPage.fxml"));

        // 'root' is loaded here, but it's invisible (Opacity 0)
        Parent root = loader.load();

        rootController = loader.getController();

        Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(Main.class.getResource("/home.css").toExternalForm());
        scene.getStylesheets().add(Main.class.getResource("/style.css").toExternalForm());
        stage.setTitle("Pomolo");

        stage.setResizable(false);

        stage.setScene(scene);
        stage.show(); // The window is now shown, but it's invisible

        // --- THIS IS THE FIX ---
        // Now, we fade in the entire root pane.
        // Because the BG and Overlay are both children of 'root',
        // they will fade in together.
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        // --- END FIX ---
    }

    @Override
    public void stop() throws Exception {
        MusicPlayerManager.getInstance().shutdown();
        super.stop();
    }

    public static RootPageController getRootController(){
        return rootController;
    }
}