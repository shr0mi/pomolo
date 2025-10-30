// Save as: src/SongManager.java (OVERWRITE)

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

public class SongManager {

    public static class SongInfo{
        public final String fileName;
        public final String path;
        public final String artist;
        public final Integer duration;

        SongInfo (String fileName, String path, String artist, Integer duration){
            this.fileName = fileName;
            this.path = path;
            this.artist = artist;
            this.duration = duration;
        }
    }

    // Reads the file information and returns SongInfo
    public static SongInfo readMp3(File mp3){
        String fileName = mp3.getName();
        String path = mp3.getAbsolutePath();
        String artist = "Unknown Artist";
        Integer durationSeconds = null;

        // --- Updated: Whole method is wrapped in try-catch ---
        try{
            AudioFile audioFile = AudioFileIO.read(mp3);

            // Get Artist Name
            Tag tag = audioFile.getTag();
            if(tag != null){
                String a = tag.getFirst(FieldKey.ARTIST);
                if(a != null && !a.trim().isBlank())
                    artist = a.trim();
            }

            // Get audio duration
            AudioHeader header = audioFile.getAudioHeader();
            if(header!=null){
                int len = header.getTrackLength(); // in seconds
                if(len > 0)
                    durationSeconds = Integer.valueOf(len);
            }

            // --- New: Fix for NullPointerException ---
            // Default duration to 0 if it's null or invalid
            if (durationSeconds == null) {
                durationSeconds = 0;
            }
            // --- End New ---

            return new SongInfo(fileName, path, artist, durationSeconds);
        } catch (Exception e) {
            // --- Updated: Don't crash the app, return null to signal failure ---
            e.printStackTrace(); // Log the error for debugging
            return null;
            // --- End Updated ---
        }
    }

    public static void main(String[] args) {
        // ... (main method is for testing, left as-is)
        File file = new File("Calm Your Anxiety.mp3");
        SongInfo info = readMp3(file);
        if (info != null) { // Added null check
            System.out.println("Filename: " + info.fileName);
            System.out.println("path: " + info.path);
            System.out.println("artist: " + info.artist);
            System.out.println("duration: " + info.duration);
            try {
                SqliteDBManager.insertNewSong(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}