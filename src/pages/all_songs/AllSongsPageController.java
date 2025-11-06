package pages.all_songs;

import com.Main;
import com.SongManager;
import com.SqliteDBManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AllSongsPageController {

    @FXML
    private VBox vbox;

    private String playlistName;
    private List<SongManager.SongInfo> allSongs;
    private List<SongManager.SongInfo> existingSongs;
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public void setExistingSongs(List<SongManager.SongInfo> existingSongs) {
        this.existingSongs = existingSongs;
    }

    public void populateSongs() {
        allSongs = SqliteDBManager.getAllSongs();
        for (SongManager.SongInfo song : allSongs) {
            vbox.getChildren().add(createSongRow(song));
        }
    }

    private HBox createSongRow(SongManager.SongInfo song) {
        HBox songRow = new HBox();
        songRow.setSpacing(10);
        songRow.setPadding(new Insets(5, 10, 5, 10));

        CheckBox checkBox = new CheckBox();
        if (existingSongs != null) {
            for (SongManager.SongInfo existingSong : existingSongs) {
                if (existingSong.path.equals(song.path)) {
                    checkBox.setDisable(true);
                    break;
                }
            }
        }
        checkBoxes.add(checkBox);

        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(grid, Priority.ALWAYS);

        grid.getColumnConstraints().add(new ColumnConstraints(400));
        grid.getColumnConstraints().add(new ColumnConstraints(200));
        grid.getColumnConstraints().add(new ColumnConstraints(100));

        Text titleText = new Text(song.fileName);
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

        GridPane.setHalignment(artistText, HPos.CENTER);
        GridPane.setHalignment(durationText, HPos.RIGHT);

        songRow.getChildren().addAll(checkBox, grid);
        return songRow;
    }

    @FXML
    private void addSelectedSongs() throws IOException {
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                SqliteDBManager.addSongToPlaylist(allSongs.get(i), playlistName);
            }
        }
        goBack();
    }

    @FXML
    private void goBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/playlist_songs/PlaylistSongsPage.fxml"));
        Parent playlistSongsPage = loader.load();
        pages.playlist_songs.PlaylistSongsPageController controller = loader.getController();
        controller.setPlaylistName(playlistName);
        Main.getRootController().setPage(playlistSongsPage);
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
}
