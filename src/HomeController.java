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

    @FXML
    private void initialize(){
        // --- THIS LINE IS REMOVED ---
        // Main.getRootController().showPlayerBar();
        // --- END REMOVAL ---

        playerManager = MusicPlayerManager.getInstance();
        loadSongs();
        if (loadedSongs != null) {
            playerManager.setQueue(loadedSongs);
        }
    }

    @FXML
    public void goToSettings(ActionEvent e) throws Exception{
        Parent settings = FXMLLoader.load(Main.class.getResource("/settings.fxml"));
        Main.getRootController().setPage(settings);
    }

    @FXML
    public void goToPlaylists() throws IOException {
        Parent playlists = FXMLLoader.load(Main.class.getResource("/playlists.fxml"));
        Main.getRootController().setPage(playlists);
    }

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
            loadSongs();
            if (loadedSongs != null) {
                playerManager.setQueue(loadedSongs);
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