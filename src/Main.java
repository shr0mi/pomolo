// Save as: src/Main.java (OVERWRITE)

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static RootPageController rootController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("rootPage.fxml"));
        Parent root = loader.load();

        rootController = loader.getController();

        Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(getClass().getResource("home.css").toExternalForm());
        stage.setTitle("Lofi Music Player");

        stage.setResizable(false);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Override the stop method to shut down the music player.
     * This is crucial for releasing media resources.
     */
    @Override
    public void stop() throws Exception {
        MusicPlayerManager.getInstance().shutdown();
        super.stop();
    }

    // Easy access to RootController from all pages
    public static RootPageController getRootController(){
        return rootController;
    }
}