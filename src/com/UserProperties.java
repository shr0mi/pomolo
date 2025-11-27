package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UserProperties {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String CONFIG_PATH = APP_DIR + File.separator + "config.properties";

    public Properties loadProperties(){
        Properties config = new Properties();
        // Correctly set the default background resource path
        String default_bg_image = "/src/default_bg.png";
        String default_opacity = "0.0";
        String default_width = "960";
        String default_height = "540";

        // Ensure the application directory exists
        new File(APP_DIR).mkdirs();

        // Try to load existing properties from the config file
        try(FileInputStream in = new FileInputStream(CONFIG_PATH)){
            config.load(in);
        } catch (IOException e){
            // If the file doesn't exist or can't be read, create the default properties in memory.
            // This is the key part for a fresh install.
            config.setProperty("default_background", default_bg_image);
            config.setProperty("background", default_bg_image);
        }

        // Ensure all required properties exist, adding them if they are missing from an older config file.
        if(!config.containsKey("default_background")) {
            config.setProperty("default_background", default_bg_image);
        }
        if(!config.containsKey("background")) {
            config.setProperty("background", default_bg_image);
        }
        if(!config.containsKey("overlay_opacity")){
            config.setProperty("overlay_opacity", default_opacity);
        }
        if(!config.containsKey("window_width")){
            config.setProperty("window_width", default_width);
        }
        if(!config.containsKey("window_height")){
            config.setProperty("window_height", default_height);
        }

        return config;
    }

    private void saveConfig(Properties config) throws IOException {
        try(FileOutputStream out = new FileOutputStream(CONFIG_PATH)){
            config.store(out, "User Settings");
        }
    }

    public void SetProperties(String backgroundImagePath) throws IOException {
        Properties config = loadProperties();
        config.setProperty("background", backgroundImagePath);
        saveConfig(config);
    }

    public void setOverlayOpacity(double opacity) throws IOException{
        Properties config = loadProperties();
        config.setProperty("overlay_opacity", String.valueOf(opacity));
        saveConfig(config);
    }

    public double getOverlayOpacity(){
        Properties config = loadProperties();
        String val = config.getProperty("overlay_opacity", "0.0");
        try{
            return Double.parseDouble(val);
        } catch (NumberFormatException e){
            return 0.0;
        }
    }

    public void setWindowWidth(double val) throws IOException{
        Properties config = loadProperties();
        config.setProperty("window_width", String.valueOf(val));
        saveConfig(config);
    }

    public double getWindowWidth(){
        Properties config = loadProperties();
        String val = config.getProperty("window_width", "960.0");
        try{
            return Double.parseDouble(val);
        } catch (NumberFormatException e){
            return 960.0;
        }
    }

    public void setWindowHeight(double val) throws IOException{
        Properties config = loadProperties();
        config.setProperty("window_height", String.valueOf(val));
        saveConfig(config);
    }

    public double getWindowHeight(){
        Properties config = loadProperties();
        String val = config.getProperty("window_height", "540.0");
        try{
            return Double.parseDouble(val);
        } catch (NumberFormatException e){
            return 540.0;
        }
    }
}