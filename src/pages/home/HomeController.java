package pages.home;

import com.Main;
import com.MusicPlayerManager;
import com.SongManager;
import com.SqliteDBManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import pages.components.Toast;
import pages.confirmation_dialog.ConfirmationDialogController;
import pages.root.RootPageController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeController {

    @FXML private AnchorPane rootPane;
    @FXML private VBox vbox;

    private MusicPlayerManager playerManager;
    private List<SongManager.SongInfo> loadedSongs;

    @FXML
    private void initialize(){
        playerManager = MusicPlayerManager.getInstance();
        loadSongs();
        if (loadedSongs != null) {
            playerManager.setQueue(loadedSongs);
        }
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        rootPane.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });

        rootPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                processDroppedFiles(db.getFiles());
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void processDroppedFiles(List<File> files) {
        AtomicInteger addedCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (File file : files) {
                    processFileOrDirectory(file, addedCount, duplicateCount);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                List<String> messages = new ArrayList<>();
                if (addedCount.get() > 0) {
                    messages.add(addedCount.get() + (addedCount.get() == 1 ? " new song added" : " new songs added"));
                }
                if (duplicateCount.get() > 0) {
                    messages.add(duplicateCount.get() + (duplicateCount.get() == 1 ? " song already exists" : " songs already exist"));
                }

                if (!messages.isEmpty()) {
                    String finalMessage = String.join(". ", messages);
                    Toast.show(finalMessage, stage, this::loadSongs);
                }

                if (loadedSongs != null) {
                    playerManager.setQueue(loadedSongs);
                }
            });
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void processFileOrDirectory(File file, AtomicInteger addedCount, AtomicInteger duplicateCount) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    processFileOrDirectory(f, addedCount, duplicateCount);
                }
            }
        } else {
            List<String> validExtensions = List.of(".mp3", ".wav", ".flac");
            String fileName = file.getName().toLowerCase();
            boolean isValidExtension = validExtensions.stream().anyMatch(fileName::endsWith);

            if (isValidExtension) {
                try {
                    if (SqliteDBManager.songExists(file.getAbsolutePath())) {
                        duplicateCount.getAndIncrement();
                    } else {
                        SongManager.SongInfo music = SongManager.readMp3(file);
                        if (music != null) {
                            SqliteDBManager.insertNewSong(music);
                            addedCount.getAndIncrement();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @FXML
    public void goToSettings(ActionEvent e) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/settings/settings.fxml"));
        Parent settings = loader.load();
        settings.getProperties().put("controller", loader.getController());
        Main.getRootController().setPage(settings);
    }

    @FXML
    public void goToDownloads(ActionEvent e) throws Exception{
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/download/DownloadPage.fxml"));
        Parent downloads = loader.load();
        downloads.getProperties().put("controller", loader.getController());
        Main.getRootController().setPage(downloads);
    }

    @FXML
    public void goToPlaylists() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/playlists/playlists.fxml"));
        Parent playlists = loader.load();
        playlists.getProperties().put("controller", loader.getController());
        Main.getRootController().setPage(playlists);
    }

    @FXML
    public void goToPomodoro(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/pomodoro/Pomodoro.fxml"));
            Parent pomodoro = loader.load();
            pomodoro.getProperties().put("controller", loader.getController());
            Main.getRootController().setPage(pomodoro);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void AddNewMusic(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        //alert.setContentText("Add music from files or a folder?");

        // Center Label Text
        Label label = new Label("   Add music from files or a folder?");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15, 0, 0, 0));
        box.setSpacing(10);

        alert.getDialogPane().setContent(box);
        alert.getDialogPane().setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        dialogPane.getStyleClass().add("root");
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getScene().setFill(Color.TRANSPARENT);


        ButtonType buttonTypeFiles = new ButtonType("From Files", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeFolder = new ButtonType("From Folder", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeCancel, buttonTypeFiles, buttonTypeFolder);

        // Center the alert dialog at the center of the main window
        alert.initOwner(Main.getMainStage());
        alert.setOnShown(ev -> {
            Stage dialogStage = (Stage) alert.getDialogPane().getScene().getWindow();

            Window owner = dialogStage.getOwner();
            dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2);
        });

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonTypeFiles) {
                addMusicFromFiles();
            } else if (result.get() == buttonTypeFolder) {
                addMusicFromFolder();
            }
        }
    }

    private void addMusicFromFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Music Files", "*.mp3", "*.wav", "*.flac")
        );
        Stage stage = (Stage) rootPane.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null && !files.isEmpty()){
            int addedCount = 0;
            for (File file : files) {
                try {
                    if (!SqliteDBManager.songExists(file.getAbsolutePath())) {
                        SongManager.SongInfo music = SongManager.readMp3(file);
                        if (music != null) {
                            SqliteDBManager.insertNewSong(music);
                            addedCount++;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            String message = addedCount + (addedCount == 1 ? " song added" : " songs added");
            Toast.show(message, (Stage) rootPane.getScene().getWindow(), this::loadSongs);
            if (loadedSongs != null) {
                playerManager.setQueue(loadedSongs);
            }
        }
    }

    private void addMusicFromFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Import");
        Stage stage = (Stage) rootPane.getScene().getWindow();
        File dir = dirChooser.showDialog(stage);
        if (dir == null || !dir.isDirectory()) return;

        AtomicInteger importedCount = new AtomicInteger();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                scanAndImport(dir, importedCount);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String message = importedCount.get() + (importedCount.get() == 1 ? " song imported" : " songs imported");
                Toast.show(message, (Stage) rootPane.getScene().getWindow(), this::loadSongs);
                if (loadedSongs != null) {
                    playerManager.setQueue(loadedSongs);
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
        });

        Thread th = new Thread(task, "ImportFolderTask");
        th.setDaemon(true);
        th.start();
    }

    private void scanAndImport(File directory, AtomicInteger importedCount) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanAndImport(f, importedCount);
            } else {
                String name = f.getName().toLowerCase();
                if ((name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac")) && !SqliteDBManager.songExists(f.getAbsolutePath())) {
                    try {
                        SongManager.SongInfo music = SongManager.readMp3(f);
                        if (music != null) {
                            SqliteDBManager.insertNewSong(music);
                            importedCount.getAndIncrement();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void loadSongs(){
        loadedSongs = SqliteDBManager.getAllSongs();
        vbox.getChildren().clear();
        int index = 0;
        for(SongManager.SongInfo s : loadedSongs){
            vbox.getChildren().add(createSongRow(s, index));
            index++;
        }
    }

    public HBox createSongRow(SongManager.SongInfo song, int index){
        HBox songRow = new HBox();
        songRow.setPrefHeight(40.0);
        songRow.setMaxWidth(Double.MAX_VALUE);
        songRow.getStyleClass().add("row-box");
        songRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Play song on click, but not if the delete button was the source
        songRow.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button || e.getTarget() instanceof FontIcon)) {
                playerManager.setQueue(loadedSongs);
                playerManager.playSong(index);
            }
        });

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setPrefHeight(30.0);
        grid.setPadding(new Insets(0, 10, 0, 10));
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.LEFT);
        col1.setPercentWidth(40);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.CENTER);
        col2.setPercentWidth(30);
        col2.setHgrow(Priority.ALWAYS);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHalignment(HPos.RIGHT);
        col3.setPercentWidth(25.0);
        col3.setHgrow(Priority.ALWAYS);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setHalignment(HPos.RIGHT);
        col4.setPercentWidth(5.0);
        col4.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().addAll(col1, col2, col3, col4);

        String fileName = song.fileName;
        if(fileName.length() > 30){
            fileName = fileName.substring(0, 27) + "...";
        }
        Text titleText = new Text(fileName);
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font("Monospaced", 13));

        Text artistText = new Text(song.artist);
        artistText.setFill(Color.WHITE);
        artistText.setFont(Font.font("Monospaced", 13));

        Text durationText = new Text(formatDuration(song.duration));
        durationText.setFill(Color.WHITE);
        durationText.setFont(Font.font("Monospaced", 13));



        // --- DELETE BUTTON ---
        Button deleteButton = new Button();
        deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
        deleteButton.getStyleClass().add("delete-btn");
        deleteButton.setOnAction(e -> {
            handleDeleteSong(song);
            e.consume(); // Prevents the row's click event from firing
        });

        grid.add(titleText, 0, 0);
        grid.add(artistText, 1, 0);
        grid.add(durationText, 2, 0);
        grid.add(deleteButton, 3, 0);



        songRow.getChildren().addAll(grid);
        HBox.setHgrow(grid, Priority.ALWAYS);

        return songRow;
    }

    private void handleDeleteSong(SongManager.SongInfo song) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/confirmation_dialog/ConfirmationDialog.fxml"));
            Parent root = loader.load();
            ConfirmationDialogController controller = loader.getController();
            controller.setMessage("Are you sure you want to delete this song?\n\n" + song.fileName);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootPane.getScene().getWindow());
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmed()) {
                SqliteDBManager.deleteSong(song.path);
                Toast.show("Song removed from library", (Stage) rootPane.getScene().getWindow(), this::loadSongs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatDuration(int seconds){
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;

        if(h>0){
            return String.format("%d hr %02d min %02d s", h, m, s);
        }else{
            return String.format("%02d min %02d s", m, s);
        }
    }

    // Search Functionality
    @FXML private HBox headingHBox;
    @FXML private TextField searchTextField;
    @FXML private Button searchBtn;
    @FXML private FontIcon searchBtnIcon;

    private ChangeListener<String> searchListener;
    private double maxSearchWidth = 300;
    private String currentSearchQuery = "";
    //private final RootPageController rootPageController = Main.getRootController();

    @FXML
    public  void searchMusic(){
        // Fade out heading
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), headingHBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            headingHBox.setVisible(false);
            headingHBox.setManaged(false);
        });

        // Disable draganddrop feature
        Main.getRootController().disableDragAndDrop();

        // Show and enable search field
        searchTextField.setVisible(true);
        searchTextField.setManaged(true);
        searchTextField.setPrefWidth(0);


        // Animate width growth
        Timeline widthAnimation = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(searchTextField.prefWidthProperty(), maxSearchWidth)
                )
        );

        // Run animations in parallel
        ParallelTransition parallel = new ParallelTransition(fadeOut, widthAnimation);
        parallel.play();

        // Remove old listener if exists
        if (searchListener != null) {
            searchTextField.textProperty().removeListener(searchListener);
        }

        // Create and add new listener
        searchListener = (obs, oldVal, newVal) -> searchSongs(newVal);
        searchTextField.textProperty().addListener(searchListener);

        searchBtnIcon.setIconLiteral("fas-times-circle");

        // Change button action to cancelSearch
        searchBtn.setOnAction(e -> cancelSearch());

        // Focus on search field automatically
        searchTextField.requestFocus();
    }

    @FXML
    public void cancelSearch(){
        // Remove listener
        if (searchListener != null) {
            searchTextField.textProperty().removeListener(searchListener);
            searchListener = null;
        }

        // Clear search
        searchTextField.clear();
        clearSearch();

        // Enable draganddrop feature
        Main.getRootController().enableDragAndDrop();

        // Animate width shrinking
        Timeline widthAnimation = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(searchTextField.prefWidthProperty(), 0)
                )
        );
        widthAnimation.setOnFinished(e -> {
            // Hide and disable search field
            searchTextField.setVisible(false);
            searchTextField.setManaged(false);
        });

        // Fade in heading
        headingHBox.setVisible(true);
        headingHBox.setManaged(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), headingHBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Run animations in parallel
        ParallelTransition parallel = new ParallelTransition(widthAnimation, fadeIn);
        parallel.play();


        // Change icon back
        searchBtnIcon.setIconLiteral("fas-search"); // or whatever your original icon was
        // Change button action back to searchMusic
        searchBtn.setOnAction(e -> searchMusic());
    }

    public void searchSongs(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        vbox.getChildren().clear();

        int index = 0;
        for (SongManager.SongInfo s : loadedSongs) {
            // Check if song matches search query
            if (currentSearchQuery.isEmpty() ||
                    s.fileName.toLowerCase().contains(currentSearchQuery) ||
                    s.artist.toLowerCase().contains(currentSearchQuery)) {

                vbox.getChildren().add(createSongRow(s, index));
            }
            index++;
        }
    }

    public void clearSearch() {
        currentSearchQuery = "";
        loadSongs(); // Show all songs again
    }
}
