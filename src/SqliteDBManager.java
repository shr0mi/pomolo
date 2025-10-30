// Save as: src/SqliteDBManager.java (OVERWRITE)

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDBManager {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String DB_URL = "jdbc:sqlite:" + APP_DIR + File.separator + "songs.db";

    public static Connection connect() throws SQLException {
        new File(APP_DIR).mkdirs();
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS songs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            fileName TEXT NOT NULL,
            path TEXT NOT NULL UNIQUE,
            artist TEXT,
            duration INTEGER
        );
        """;
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    public static boolean songExists(String path) {
        String sql = "SELECT 1 FROM songs WHERE path = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking if song exists: " + e.getMessage());
        }
        return false;
    }

    public static void insertNewSong(SongManager.SongInfo song) {
        if (song == null || song.path == null || songExists(song.path)) {
            return; // Song is invalid or already exists
        }
        createTable(); // Ensure table exists
        String sql = "INSERT INTO songs(fileName, path, artist, duration) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, song.fileName);
            pstmt.setString(2, song.path);
            pstmt.setString(3, song.artist);
            pstmt.setInt(4, song.duration);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting new song: " + e.getMessage());
        }
    }

    public static List<SongManager.SongInfo> getAllSongs() {
        List<SongManager.SongInfo> songs = new ArrayList<>();
        createTable(); // Ensure table exists
        String sql = "SELECT fileName, path, artist, duration FROM songs ORDER BY fileName ASC";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                songs.add(new SongManager.SongInfo(
                        rs.getString("fileName"),
                        rs.getString("path"),
                        rs.getString("artist"),
                        rs.getInt("duration")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all songs: " + e.getMessage());
        }
        return songs;
    }
}