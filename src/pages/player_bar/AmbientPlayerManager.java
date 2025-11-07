package pages.player_bar;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class AmbientPlayerManager {
    public boolean isPlaying = false;
    Media rain = new Media(getClass().getResource("/ambient_sounds/rain.wav").toExternalForm());
    Media fireplace = new Media(getClass().getResource("/ambient_sounds/fireplace.mp3").toExternalForm());
    Media wind = new Media(getClass().getResource("/ambient_sounds/wind.mp3").toExternalForm());

    MediaPlayer rainPlayer = new MediaPlayer(rain);
    MediaPlayer firePlayer = new MediaPlayer(fireplace);
    MediaPlayer windPlayer = new MediaPlayer(wind);

    public void playAmbientMusic(){
        rainPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        firePlayer.setCycleCount(MediaPlayer.INDEFINITE);
        windPlayer.setCycleCount(MediaPlayer.INDEFINITE);

        windPlayer.play();
        rainPlayer.play();
        firePlayer.play();
        isPlaying = true;
    }

    public void stopAmbientMusic(){
        rainPlayer.stop();
        firePlayer.stop();
        windPlayer.stop();
        isPlaying = false;
    }

    // Rain sound control
    public void setRainVolume(double volume){
        rainPlayer.setVolume(volume);
    }

    public double getRainVolume(){
        return rainPlayer.getVolume();
    }

    // Fire sound control
    public void setFireplaceVolume(double volume){
        firePlayer.setVolume(volume);
    }

    public double getFireplaceVolume(){
        return firePlayer.getVolume();
    }

    // Wind sound control
    public void setWindVolume(double volume){
        windPlayer.setVolume(volume);
    }

    public double getWindVolume(){
        return windPlayer.getVolume();
    }
}
