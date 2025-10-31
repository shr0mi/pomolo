import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NewPlaylistDialogController {

    @FXML private TextField playlistNameField;

    private PlaylistsPageController playlistsPageController;

    public void setPlaylistsPageController(PlaylistsPageController controller) {
        this.playlistsPageController = controller;
    }

    @FXML
    private void createPlaylist() {
        String playlistName = playlistNameField.getText();
        if (playlistName != null && !playlistName.isEmpty()) {
            SqliteDBManager.insertNewPlaylist(playlistName);
            playlistsPageController.loadPlaylists();
            closeDialog();
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) playlistNameField.getScene().getWindow();
        stage.close();
    }
}
