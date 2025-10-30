// Save as: src/UserProperties.java (OVERWRITE)

import java.io.File; // Added
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UserProperties {

    // --- New ---
    // Store the config file in the same dedicated folder
    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String CONFIG_PATH = APP_DIR + File.separator + "config.properties";
    // --- End New ---

    public Properties loadProperties(){
        Properties config = new Properties();
        String bg_image = "Background.jpg"; //By default

        // --- New ---
        // Ensure the directory exists before trying to load
        new File(APP_DIR).mkdirs();
        // --- End New ---

        // --- Updated: Now loads from the full CONFIG_PATH ---
        try(FileInputStream in = new FileInputStream(CONFIG_PATH)){
            config.load(in);
        } catch (IOException e){
            // This is safe: if config fails to load, create defaults in memory
            config.setProperty("default_background", bg_image);
            config.setProperty("background", bg_image);
        }
        // --- End Updated ---

        return config;
    }

    // --- Updated: Added 'throws IOException' ---
    public void SetProperties(String backgroundImagePath) throws IOException {
        Properties config = loadProperties();
        config.setProperty("background", backgroundImagePath);

        // --- Updated: Removed try-catch, exception is thrown to the caller ---
        // Also saves to the full CONFIG_PATH
        try(FileOutputStream out = new FileOutputStream(CONFIG_PATH)){
            config.store(out, "Set Settings");
        }
        // --- End Updated ---
    }
}