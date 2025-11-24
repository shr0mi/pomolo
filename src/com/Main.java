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

public class Main extends Application {
    public static RootPageController rootController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/root/rootPage.fxml"));
        Parent root = loader.load();
        rootController = loader.getController();

        Scene scene = new Scene(root, 1280, 720);
        //Scene scene = new Scene(root, 960, 540);
        scene.getStylesheets().add(Main.class.getResource("/css/home.css").toExternalForm());

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

    /**
     * Show the mini player in its own undecorated, non-resizable stage.
     * Keeps styling consistent by adding the dark theme stylesheet if available.
     */
    public static void showMiniPlayer(Parent miniRoot) {
        Stage miniStage = new Stage();
        // prefer the mini player's declared stylesheet, but ensure dark-theme is applied
        Scene miniScene = new Scene(miniRoot);
        try {
            var css = Main.class.getResource("/css/dark-theme.css");
            if (css != null) miniScene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {
        }
        miniStage.initStyle(StageStyle.UNDECORATED);
        miniStage.setResizable(false);
        miniStage.setAlwaysOnTop(true);
        miniStage.setScene(miniScene);
        miniStage.show();
    }
}