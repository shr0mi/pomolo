package pages.download;

import com.Main;
import com.DownloadManager;
import com.UserProperties;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;

public class DownloadPageController {

    @FXML TextField linkInput;
    @FXML TextArea terminalOutput;
    @FXML TextField yt_dlp_location;
    @FXML TextField ffmpeg_location;
    @FXML TextField download_location;

    UserProperties up = new UserProperties();

    String ytdlpLocation = "yt-dlp";
    String ffmpegLocation = "ffmpeg";
    String downloadLocation = "";

    @FXML
    private void initialize(){
        yt_dlp_location.setText(up.get_ytdlp_location());
        ffmpeg_location.setText(up.get_ffmpeg_location());
        download_location.setText(up.get_download_location());

        ytdlpLocation = up.get_ytdlp_location();
        ffmpegLocation = up.get_ffmpeg_location();
        downloadLocation = up.get_download_location();
    }

    @FXML
    public void download(ActionEvent e) throws Exception{
        // Clear previous output
        terminalOutput.clear();

        // Run download in separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                DownloadManager.downloadAudio(
                        linkInput.getText(),
                        ytdlpLocation,
                        ffmpegLocation,
                        downloadLocation,
                        line -> Platform.runLater(() -> terminalOutput.appendText(line))
                );
                Platform.runLater(() -> terminalOutput.appendText("\nDownload completed successfully!\n"));
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

    @FXML
    public void setYtDlpLocation(ActionEvent e){
        try {
            up.set_ytdlp_location(yt_dlp_location.getText());
            ytdlpLocation = up.get_ytdlp_location();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    public void setFfmpegLocation(ActionEvent e){
        try {
            up.set_ffmpeg_location(ffmpeg_location.getText());
            ffmpegLocation = up.get_ffmpeg_location();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    public void setDownloadLocation(ActionEvent e){
        try {
            up.set_download_location(download_location.getText());
            downloadLocation = up.get_download_location();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
