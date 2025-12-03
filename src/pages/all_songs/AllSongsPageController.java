package pages.all_songs;

import com.Main;
import com.MusicPlayerManager;
import com.SongManager;
import com.SqliteDBManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.stage.Stage;
import pages.components.Toast;

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
    private MusicPlayerManager playerManager;


    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public void setExistingSongs(List<SongManager.SongInfo> existingSongs) {
        this.existingSongs = existingSongs;
    }

    public void loadSongs() {
        vbox.getChildren().clear();
        checkBoxes.clear();
        playerManager = MusicPlayerManager.getInstance();
        allSongs = SqliteDBManager.getAllSongs();
        playerManager.setQueue(allSongs);
        populateSongs();
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

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(100);
        col1.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().add(col1);

        Text titleText = new Text(song.fileName);
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font("Monospaced", 13));

        grid.add(titleText, 0, 0);

        songRow.getChildren().addAll(checkBox, grid);
        return songRow;
    }

    @FXML
    private void addSelectedSongs() {
        if (playlistName == null || playlistName.isEmpty()) {
            // No playlist selected — go back safely
            Toast.show("No playlist selected", (Stage) vbox.getScene().getWindow(), this::goBackSafe);
            return;
        }

        int addedCount = 0;
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                SqliteDBManager.addSongToPlaylist(allSongs.get(i), playlistName);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            String message = addedCount + (addedCount == 1 ? " song added" : " songs added");
            Toast.show(message, (Stage) vbox.getScene().getWindow(), this::goBackSafe);
        } else {
            goBackSafe();
        }
    }

    @FXML
    private void goBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/playlist_songs/PlaylistSongsPage.fxml"));
        Parent playlistSongsPage = loader.load();
        pages.playlist_songs.PlaylistSongsPageController controller = loader.getController();
        controller.setPlaylistName(playlistName);
        playlistSongsPage.getProperties().put("controller", controller);
        Main.getRootController().setPage(playlistSongsPage);
    }

    private void goBackSafe() {
        Platform.runLater(() -> {
            try {
                goBack();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
