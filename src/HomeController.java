// Save as: src/HomeController.java (OVERWRITE)

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
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

    //Initialize
    @FXML
    private void initialize(){
        // Get the single instance of the player manager
        playerManager = MusicPlayerManager.getInstance();

        // Load songs and add them to the VBox
        loadSongs();

        // Pass the loaded song list to the player manager
        if (loadedSongs != null) {
            playerManager.setQueue(loadedSongs);
        }
    }

    //Go to settings
    @FXML
    public void goToSettings(ActionEvent e) throws Exception{
        Parent settings = FXMLLoader.load(getClass().getResource("settings.fxml"));
        Main.getRootController().setPage(settings);

    }

    // Add new music
    @FXML
    private void AddNewMusic(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Music File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Music Files", "*.mp3", "*.wav", "*.flac")
        );

        Stage stage = (Stage) rootPane.getScene().getWindow();

        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            SongManager.SongInfo music = SongManager.readMp3(file);
            SqliteDBManager.insertNewSong(music);

            // Reload songs from DB
            loadSongs();
            // Update the player manager's queue
            if (loadedSongs != null) {
                playerManager.setQueue(loadedSongs);
            }
        }
    }

    // Load Songs and add them to the Vbox
    public void loadSongs(){
        loadedSongs = SqliteDBManager.getAllSongs();
        vbox.getChildren().clear(); // reset

        int index = 0;
        for(SongManager.SongInfo s : loadedSongs){
            vbox.getChildren().add(createSongRow(s, index));
            index++;
        }
    }

    // Create a hbox (music row)
    public HBox createSongRow(SongManager.SongInfo song, int index){
        HBox songRow = new HBox();
        songRow.setPrefHeight(40.0);
        songRow.setMaxWidth(Double.MAX_VALUE);
        songRow.getStyleClass().add("row-box");

        // --- THIS IS THE KEY CHANGE ---
        // On click, play the song AND navigate to the player page
        songRow.setOnMouseClicked(e -> {
            try {
                // 1. Tell the manager to play the song
                playerManager.playSong(index);

                // 2. Load and navigate to the player page
                Parent playerPage = FXMLLoader.load(getClass().getResource("PlayerPage.fxml"));
                Main.getRootController().setPage(playerPage);

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });


        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setPrefHeight(30.0);
        grid.setPadding(new Insets(0, 10, 0, 10));

        // Columns in Grid
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

        // Text: file name
        String fileName = song.fileName;
        if(fileName.length() > 30){
            fileName = fileName.substring(0, 27) + "...";
        }
        Text titleText = new Text(fileName);
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font("Monospaced", 13));

        // Text: artist name
        Text artistText = new Text(song.artist);
        artistText.setFill(Color.WHITE);
        artistText.setFont(Font.font("Monospaced", 13));

        // Text: duration
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