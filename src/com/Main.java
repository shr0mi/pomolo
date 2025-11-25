package com;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import pages.root.RootPageController;

import java.io.IOException;

public class Main extends Application {
    public static RootPageController rootController;
    public static Stage main_stage;
    public static UserProperties up;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/root/rootPage.fxml"));
        Parent root = loader.load();
        rootController = loader.getController();

        up = new UserProperties();

        Scene scene = new Scene(root, up.getWindowWidth(), up.getWindowHeight());
        //Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(Main.class.getResource("/css/home.css").toExternalForm());

        main_stage = stage;

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

    public static void changeWindowWidth(double width, double aspect_ratio){
        main_stage.setWidth(width);
        main_stage.setHeight(width / aspect_ratio);

        try{
            up.setWindowWidth(width);
            up.setWindowHeight(width / aspect_ratio);
        }catch (IOException e){
            System.out.println("Settings Error, Couldn't set width and height: " + e.getMessage());

        }
    }

}