import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

//Checking push
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

        //stage.setWidth(960);
        //stage.setHeight(540);
        stage.setResizable(false);


        stage.setScene(scene);
        stage.show();
    }

    // Easy access to RootController from all pages
    public static RootPageController getRootController(){
        return rootController;
    }
}
