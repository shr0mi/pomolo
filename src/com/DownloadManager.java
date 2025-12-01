package com;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class DownloadManager {

    UserProperties up = new UserProperties();

    public static void downloadAudio(String url, String ytdlpLocation, String ffmpegLocation, String outputPath, Consumer<String> outputCallback) throws Exception {
        String output = outputPath + "/%(title)s.%(ext)s";
        ProcessBuilder pb = new ProcessBuilder(
                ytdlpLocation,
                "-x", "--audio-format", "mp3",
                "--ffmpeg-location", ffmpegLocation,
                "-o", output,
                url
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (outputCallback != null) {
                    outputCallback.accept(line + "\n");
                }
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("yt-dlp failed with exit code " + exitCode);
        }
    }

}
