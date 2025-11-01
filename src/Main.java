import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Main extends Application {
    public static RootPageController rootController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/rootPage.fxml"));
        Parent root = loader.load();
        rootController = loader.getController();

        Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(Main.class.getResource("/home.css").toExternalForm());

        // 1. Remove default window decorations
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Pomolo");
        stage.setResizable(false);

        stage.setScene(scene);
        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
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