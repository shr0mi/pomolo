package pages.playlist_songs;

import com.Main;
import com.MusicPlayerManager;
import com.SongManager;
import com.SqliteDBManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import pages.all_songs.AllSongsPageController;
import pages.components.Toast;
import pages.confirmation_dialog.ConfirmationDialogController;

import java.io.IOException;
import java.util.List;

public class PlaylistSongsPageController {

    @FXML private AnchorPane rootPane;
    @FXML private Text playlistNameText;
    @FXML private VBox vbox;
    @FXML private ScrollPane scrollPane;

    private String playlistName;
    private MusicPlayerManager playerManager;
    private List<SongManager.SongInfo> playlistSongs;

    public void setPlaylistName(String name) {
        this.playlistName = name;
        playlistNameText.setText(name);
        loadSongs();
    }

    @FXML
    private void initialize() {
        playerManager = MusicPlayerManager.getInstance();
        vbox.prefHeightProperty().bind(scrollPane.heightProperty());
    }

    private void loadSongs() {
        playlistSongs = SqliteDBManager.getSongsForPlaylist(playlistName);
        playerManager.setQueue(playlistSongs);
        vbox.getChildren().clear();
        int index = 0;
        for (SongManager.SongInfo s : playlistSongs) {
            vbox.getChildren().add(createSongRow(s, index));
            index++;
        }
    }

    @FXML
    private void AddNewMusic() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/all_songs/AllSongsPage.fxml"));
        Parent allSongsPage = loader.load();
        AllSongsPageController controller = loader.getController();
        controller.setPlaylistName(playlistName);
        controller.setExistingSongs(playlistSongs);
        controller.populateSongs();
        Main.getRootController().setPage(allSongsPage);
    }

    public HBox createSongRow(SongManager.SongInfo song, int index) {
        HBox songRow = new HBox();
        songRow.setPrefHeight(40.0);
        songRow.setMaxWidth(Double.MAX_VALUE);
        songRow.getStyleClass().add("row-box");

        songRow.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) {
                return;
            }
            playerManager.setQueue(playlistSongs);
            playerManager.playSong(index);
        });

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setPrefHeight(30.0);
        grid.setPadding(new Insets(0, 10, 0, 10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.LEFT);
        col1.setPercentWidth(40.0);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.CENTER);
        col2.setPercentWidth(20.0);
        col2.setHgrow(Priority.ALWAYS);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHalignment(HPos.CENTER);
        col3.setPercentWidth(20.0);
        col3.setHgrow(Priority.ALWAYS);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setHalignment(HPos.RIGHT);
        col4.setPercentWidth(20.0);
        col4.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2, col3, col4);

        String fileName = song.fileName;
        if (fileName.length() > 30) {
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

        Button deleteButton = new Button();
        deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
        deleteButton.getStyleClass().add("delete-btn");
        deleteButton.setOnAction(e -> {
            e.consume(); // Prevent the row's onMouseClicked from firing
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/confirmation_dialog/ConfirmationDialog.fxml"));
                Parent root = loader.load();
                ConfirmationDialogController controller = loader.getController();
                controller.setMessage("Are you sure you want to remove the song: " + song.fileName + " from this playlist?");

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
                    SqliteDBManager.removeSongFromPlaylist(song, playlistName);
                    Toast.show("Song removed from playlist", (Stage) rootPane.getScene().getWindow(), this::loadSongs);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        grid.add(titleText, 0, 0);
        grid.add(artistText, 1, 0);
        grid.add(durationText, 2, 0);
        grid.add(deleteButton, 3, 0);


        songRow.getChildren().add(grid);
        HBox.setHgrow(grid, Priority.ALWAYS);

        return songRow;
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;

        if (h > 0) {
            return String.format("%d hr %02d min %02d s", h, m, s);
        } else {
            return String.format("%02d min %02d s", m, s);
        }
    }

    @FXML
    private void goBack() throws IOException {
        Parent playlists = FXMLLoader.load(Main.class.getResource("/pages/playlists/playlists.fxml"));
        Main.getRootController().setPage(playlists);
    }
}
