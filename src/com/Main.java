package com;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import pages.root.RootPageController;

import java.io.IOException;

public class Main extends Application {
    public static RootPageController rootController;
    public static Stage main_stage;
    public static UserProperties up;

    public static Stage mainStage;
    private static boolean focusHandlerActive = true;


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

        attachHotkeys(scene);
        setupGlobalFocusHandler(scene, root);


        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Pomolo");
        stage.setResizable(false);
        stage.setScene(scene);
        mainStage = stage;

        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        root.requestFocus();
    }

    public static void attachHotkeys(Scene scene) {
        scene.setOnKeyPressed(event -> {
            // Context Check: Only trigger if window is focused
            if (scene.getWindow() != null && !scene.getWindow().isFocused()) {
                return;
            }

            // Input Check: Don't trigger if typing
            if (event.getTarget() instanceof TextInputControl) {
                return;
            }

            MusicPlayerManager manager = MusicPlayerManager.getInstance();
            boolean consumed = true;

            switch (event.getCode()) {
                case SPACE:
                    manager.playPause();
                    break;
                case LEFT:
                    manager.previous();
                    break;
                case RIGHT:
                    manager.next();
                    break;
                case UP:
                    double volUp = manager.volumeProperty().get();
                    manager.volumeProperty().set(Math.min(1.0, volUp + 0.05));
                    break;
                case DOWN:
                    double volDown = manager.volumeProperty().get();
                    manager.volumeProperty().set(Math.max(0.0, volDown - 0.05));
                    break;
                default:
                    consumed = false;
            }

            if (consumed) {
                event.consume();
            }
        });
    }

    // NEW: Aggressive Global Focus Handler
    // This intercepts mouse clicks on the Scene. If a button is clicked,
    // it lets the click happen but then immediately steals focus back to the root.
    public static void setupGlobalFocusHandler(Scene scene, Parent root) {
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
            if (focusHandlerActive) {
                // We run this 'later' so the button click still registers its action
                // but focus is immediately taken away after.
                javafx.application.Platform.runLater(root::requestFocus);
            }
        });

        // Also recursively strip focus traversal
        makeButtonsNonFocusable(root);
    }

    public static void setFocusHandlerActive(boolean active) {
        focusHandlerActive = active;
    }


    public static void makeButtonsNonFocusable(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Button) {
                node.setFocusTraversable(false);
            } else if (node instanceof Parent) {
                makeButtonsNonFocusable((Parent) node);
            }
        }
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


    public static Stage getMainStage(){
        return mainStage;
    }



    public static void showMiniPlayer(Parent miniRoot) {
        Stage miniStage = new Stage();
        Scene miniScene = new Scene(miniRoot);

        // 1. Attach Hotkeys
        attachHotkeys(miniScene);

        // 2. Attach Aggressive Focus Handler
        setupGlobalFocusHandler(miniScene, miniRoot);

        try {
            var css = Main.class.getResource("/css/dark-theme.css");
            if (css != null) miniScene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        miniStage.initStyle(StageStyle.TRANSPARENT);
        miniStage.setResizable(false);
        miniStage.setAlwaysOnTop(true);
        miniStage.setScene(miniScene);
        miniStage.show();

        miniRoot.requestFocus();
    }

}
