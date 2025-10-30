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
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/rootPage.fxml"));
        Parent root = loader.load();

        rootController = loader.getController();

        // --- MODIFIED: Set height back to 540 ---
        Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(Main.class.getResource("/home.css").toExternalForm());
        stage.setTitle("Lofi Music Player");

        stage.setResizable(false);

        stage.setScene(scene);
        stage.show();
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