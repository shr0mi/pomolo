package pages.download;

import com.Main;
import com.DownloadManager;
import com.SongManager;
import com.SqliteDBManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DownloadPageController {

    @FXML TextField linkInput;
    @FXML TextArea terminalOutput;


    @FXML
    public void download(ActionEvent e) throws Exception{
        // Clear previous output
        terminalOutput.clear();

        // Run download in separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                String downloadPath = SqliteDBManager.getAppDir() + File.separator + "songs";
                File downloadDir = new File(downloadPath);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                String downloadedFilePath = DownloadManager.downloadAudio(
                        linkInput.getText(),
                        downloadPath,
                        line -> Platform.runLater(() -> terminalOutput.appendText(line))
                );

                Platform.runLater(() -> terminalOutput.appendText("\nDownload completed successfully!\nImporting to library..."));

                // Import into library
                File mp3File = new File(downloadedFilePath);
                if (mp3File.exists()) {
                    SongManager.SongInfo songInfo = SongManager.readMp3(mp3File);
                    if (songInfo != null) {
                        try {
                            SqliteDBManager.insertNewSong(songInfo);
                            Platform.runLater(() -> {
                                terminalOutput.appendText("\nImported: " + songInfo.fileName);
                                Main.getRootController().refreshCurrentPage();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> terminalOutput.appendText("\nFailed to import: " + mp3File.getName()));
                        }
                    }
                }
                Platform.runLater(() -> terminalOutput.appendText("\nLibrary import finished.\n"));


            } catch (Exception ex) {
                Platform.runLater(() -> {
                    terminalOutput.appendText("\nError: " + ex.getMessage() + "\n");
                    showError("Download Error", "Failed to download: " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void goToHome(ActionEvent e) {
        try {
            Parent home = FXMLLoader.load(Main.class.getResource("/pages/home/home.fxml"));
            Main.getRootController().setPage(home);
        } catch (IOException ioException) {
            showError("Navigation Error", "Could not load home page: " + ioException.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void openLink(ActionEvent event) {
        String url = (String) ((Node) event.getSource()).getUserData();
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            showError("Error", "Could not open link: " + e.getMessage());
        }
    }

}
