import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

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
        Parent settings = FXMLLoader.load(Main.class.getResource("/settings.fxml"));
        Main.getRootController().setPage(settings);
    }

    @FXML
    public void goToSongs() throws IOException {
        Parent home = FXMLLoader.load(Main.class.getResource("/home.fxml"));
        Main.getRootController().setPage(home);
    }

    @FXML
    private void AddNewPlaylist() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/NewPlaylistDialog.fxml"));
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
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/PlaylistSongsPage.fxml"));
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

        Text nameText = new Text(playlist.name);
        nameText.setFill(Color.WHITE);
        nameText.setFont(Font.font("Monospaced", 13));

        Text songCountText = new Text(String.valueOf(playlist.songCount));
        songCountText.setFill(Color.WHITE);
        songCountText.setFont(Font.font("Monospaced", 13));

        Text durationText = new Text(formatDuration(playlist.duration));
        durationText.setFill(Color.WHITE);
        durationText.setFont(Font.font("Monospaced", 13));

        grid.add(nameText, 0, 0);
        grid.add(songCountText, 1, 0);
        grid.add(durationText, 2, 0);

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