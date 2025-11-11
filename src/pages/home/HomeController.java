package pages.home;

// --- Imports explanation ---
// com.Main                     -> root controller access for navigation and background settings
// com.MusicPlayerManager       -> central music player manager (controls playback and queue)
// com.SongManager              -> helpers to read metadata from files and represent SongInfo
// com.SqliteDBManager          -> database helper for persisting and fetching songs (blocking calls)
// javafx.application.Platform  -> run UI updates on JavaFX Application Thread
// javafx.concurrent.Task       -> background task abstraction for running blocking work off UI thread
// javafx.stage.FileChooser     -> file picker for adding new music
// javafx.scene.control.ProgressIndicator -> small spinner shown while loading

import com.Main;
import com.MusicPlayerManager;
import com.SongManager;
import com.SqliteDBManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class HomeController {

    @FXML private AnchorPane rootPane;
    @FXML private VBox vbox;

    private MusicPlayerManager playerManager;
    private List<SongManager.SongInfo> loadedSongs;

    @FXML
    private void initialize(){
        // Purpose:
        //  - initialize references (playerManager)
        //  - kick off asynchronous loading of songs so UI remains responsive
        // Note: SqliteDBManager.getAllSongs() is blocking so we must not call it on the JavaFX thread.
        playerManager = MusicPlayerManager.getInstance();
        // Load songs off the UI thread and update UI when ready
        loadSongsAsync();
    }

    @FXML
    public void goToSettings(ActionEvent e) throws Exception{
        Parent settings = FXMLLoader.load(Main.class.getResource("/pages/settings/settings.fxml"));
        Main.getRootController().setPage(settings);
    }

    @FXML
    public void goToPlaylists() throws IOException {
        Parent playlists = FXMLLoader.load(Main.class.getResource("/pages/playlists/playlists.fxml"));
        Main.getRootController().setPage(playlists);
    }

    @FXML
    public void goToPomodoro(ActionEvent e) throws Exception{
        // Load the new Pomodoro FXML
        Parent pomodoro = FXMLLoader.load(Main.class.getResource("/pages/pomodoro/Pomodoro.fxml"));
        // Use the main controller to switch the view
        Main.getRootController().setPage(pomodoro);

    }

    @FXML
    private void AddNewMusic(){
        // Purpose:
        //  - open file chooser, read file metadata, insert into DB and refresh UI.
        //  - The DB insert is fast enough for this app, but we reload the list asynchronously.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Music Files", "*.mp3", "*.wav", "*.flac")
        );
        Stage stage = (Stage) rootPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            SongManager.SongInfo music = SongManager.readMp3(file);
            SqliteDBManager.insertNewSong(music); // persist new song
            // reload asynchronously after inserting
            loadSongsAsync();
        }
    }

    // Asynchronously load songs from DB and update UI on success/failure
    public void loadSongsAsync(){
        // Show a small progress indicator immediately on UI thread
        Platform.runLater(() -> {
            vbox.getChildren().clear();
            ProgressIndicator pi = new ProgressIndicator();
            pi.setMaxSize(48, 48);
            vbox.getChildren().add(pi);
        });

        // Create a Task that performs the blocking DB call off the UI thread
        Task<List<SongManager.SongInfo>> task = new Task<>() {
            @Override
            protected List<SongManager.SongInfo> call() throws Exception {
                // Blocking DB call - runs on background thread
                return SqliteDBManager.getAllSongs();
            }
        };

        // When task completes successfully, update the UI and player queue (on FX thread)
        task.setOnSucceeded(evt -> {
            List<SongManager.SongInfo> songs = task.getValue();
            updateSongsUI(songs);
        });

        // On failure, clear the loader and show fallback (handled in updateSongsUI)
        task.setOnFailed(evt -> {
            updateSongsUI(null);
        });

        Thread th = new Thread(task, "db-loader-thread");
        th.setDaemon(true); // doesn't prevent JVM exit
        th.start();
    }

    // Must update UI components on JavaFX Application Thread
    private void updateSongsUI(List<SongManager.SongInfo> songs){
        // This method always posts a Platform.runLater to ensure UI updates happen on FX thread
        Platform.runLater(() -> {
            vbox.getChildren().clear();
            if (songs == null || songs.isEmpty()) {
                Text empty = new Text("No songs available");
                empty.setFill(Color.WHITE);
                empty.setFont(Font.font("Monospaced", 13));
                vbox.getChildren().add(empty);
                loadedSongs = (songs == null) ? List.of() : songs;
                playerManager.setQueue(loadedSongs); // update player's queue even if empty
                return;
            }

            loadedSongs = songs;
            int index = 0;
            for (SongManager.SongInfo s : loadedSongs) {
                // Each row is an HBox that will call playerManager.playSong(index) on click
                vbox.getChildren().add(createSongRow(s, index));
                index++;
            }
            // update the player's queue now that songs are loaded
            playerManager.setQueue(loadedSongs);
        });
    }

    // createSongRow: builds a single clickable row representing a song
    public HBox createSongRow(SongManager.SongInfo song, int index){
        HBox songRow = new HBox();
        songRow.setPrefHeight(40.0);
        songRow.setMaxWidth(Double.MAX_VALUE);
        songRow.getStyleClass().add("row-box");

        // clicking a row sets the playlist and starts playback at the clicked index
        songRow.setOnMouseClicked(e -> {
            playerManager.setQueue(loadedSongs);
            playerManager.playSong(index);
        });

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setPrefHeight(30.0);
        grid.setPadding(new Insets(0, 10, 0, 10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.LEFT);
        col1.setPercentWidth(33.33);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.CENTER);
        col2.setPercentWidth(33.33);
        col2.setHgrow(Priority.ALWAYS);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHalignment(HPos.RIGHT);
        col3.setPercentWidth(33.33);
        col3.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2, col3);

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

        grid.add(titleText, 0, 0);
        grid.add(artistText, 1, 0);
        grid.add(durationText, 2, 0);

        songRow.getChildren().add(grid);
        HBox.setHgrow(grid, Priority.ALWAYS);

        return songRow;
    }

    // Utility: format seconds to readable duration string
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
}