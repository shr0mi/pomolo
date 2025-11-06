package pages.playlists;

import com.Main;
import com.SqliteDBManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pages.confirmation_dialog.ConfirmationDialogController;
import pages.new_playlist_dialog.NewPlaylistDialogController;
import pages.playlist_songs.PlaylistSongsPageController;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PlaylistsPageController {

    @FXML private AnchorPane rootPane;
    @FXML private VBox vbox;

    @FXML
    private void initialize(){
        SqliteDBManager.insertDefaultPlaylist();
        loadPlaylists();
    }

    @FXML
    public void goToSettings(ActionEvent e) throws Exception{
        Parent settings = FXMLLoader.load(Main.class.getResource("/pages/settings/settings.fxml"));
        Main.getRootController().setPage(settings);
    }

    @FXML
    public void goToSongs() throws IOException {
        Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
        Main.getRootController().setPage(home);
    }

    @FXML
    private void AddNewPlaylist() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/new_playlist_dialog/NewPlaylistDialog.fxml"));
        Parent root = loader.load();

        NewPlaylistDialogController controller = loader.getController();
        controller.setPlaylistsPageController(this);

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(rootPane.getScene().getWindow());
        dialogStage.setScene(new Scene(root));

        dialogStage.showAndWait();
    }

    public void loadPlaylists(){
        List<SqliteDBManager.PlaylistInfo> playlists = SqliteDBManager.getAllPlaylists();
        vbox.getChildren().clear();
        for(SqliteDBManager.PlaylistInfo p : playlists){
            vbox.getChildren().add(createPlaylistRow(p));
        }
    }

    public HBox createPlaylistRow(SqliteDBManager.PlaylistInfo playlist){
        HBox playlistRow = new HBox();
        playlistRow.setPrefHeight(40.0);
        playlistRow.setMaxWidth(Double.MAX_VALUE);
        playlistRow.getStyleClass().add("row-box");

        playlistRow.setOnMouseClicked(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/playlist_songs/PlaylistSongsPage.fxml"));
                Parent playlistSongsPage = loader.load();
                PlaylistSongsPageController controller = loader.getController();
                controller.setPlaylistName(playlist.name);
                Main.getRootController().setPage(playlistSongsPage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
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

        Text nameText = new Text(playlist.name);
        nameText.setFill(Color.WHITE);
        nameText.setFont(Font.font("Monospaced", 13));

        Text songCountText = new Text(String.valueOf(playlist.songCount));
        songCountText.setFill(Color.WHITE);
        songCountText.setFont(Font.font("Monospaced", 13));

        Text durationText = new Text(formatDuration(playlist.duration));
        durationText.setFill(Color.WHITE);
        durationText.setFont(Font.font("Monospaced", 13));

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> {
            e.consume(); // Prevent the row's onMouseClicked from firing
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pages/confirmation_dialog/ConfirmationDialog.fxml"));
                Parent root = loader.load();
                ConfirmationDialogController controller = loader.getController();
                controller.setMessage("Are you sure you want to delete the playlist: " + playlist.name + "?");

                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initOwner(rootPane.getScene().getWindow());
                dialogStage.setScene(new Scene(root));
                controller.setDialogStage(dialogStage);

                dialogStage.showAndWait();

                if (controller.isConfirmed()) {
                    SqliteDBManager.deletePlaylist(playlist.name);
                    loadPlaylists();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        grid.add(nameText, 0, 0);
        grid.add(songCountText, 1, 0);
        grid.add(durationText, 2, 0);
        grid.add(deleteButton, 3, 0);

        playlistRow.getChildren().add(grid);
        HBox.setHgrow(grid, Priority.ALWAYS);

        return playlistRow;
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
