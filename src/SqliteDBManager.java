// Save as: src/SqliteDBManager.java (OVERWRITE)

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDBManager {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String DB_URL = "jdbc:sqlite:" + APP_DIR + File.separator + "songs.db";
    private static final String DB_URL_PLAYLIST = "jdbc:sqlite:" + APP_DIR + File.separator + "playlists.db";
    private static final String SONGS_DB_PATH = APP_DIR + File.separator + "songs.db";

    public static class PlaylistInfo {
        public final String name;
        public final int songCount;
        public final int duration;

        public PlaylistInfo(String name, int songCount, int duration) {
            this.name = name;
            this.songCount = songCount;
            this.duration = duration;
        }
    }

    public static Connection connect() throws SQLException {
        new File(APP_DIR).mkdirs();
        return DriverManager.getConnection(DB_URL);
    }

    public static Connection connectPlaylist() throws SQLException {
        new File(APP_DIR).mkdirs();
        return DriverManager.getConnection(DB_URL_PLAYLIST);
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

    public static void createPlaylistTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS playlists (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        );
        """;
        try (Connection conn = connectPlaylist(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating playlist table: " + e.getMessage());
        }

        sql = """
        CREATE TABLE IF NOT EXISTS playlist_songs (
            playlist_id INTEGER,
            song_id INTEGER,
            FOREIGN KEY (playlist_id) REFERENCES playlists(id),
            FOREIGN KEY (song_id) REFERENCES songs(id)
        );
        """;

        try (Connection conn = connectPlaylist(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating playlist_songs table: " + e.getMessage());
        }
    }

    public static void insertDefaultPlaylist() {
        createPlaylistTable();
        String sql = "INSERT OR IGNORE INTO playlists (name) VALUES ('demo')";
        try (Connection conn = connectPlaylist(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error inserting default playlist: " + e.getMessage());
        }
    }

    public static void insertNewPlaylist(String name) {
        createPlaylistTable();
        String sql = "INSERT OR IGNORE INTO playlists (name) VALUES (?)";
        try (Connection conn = connectPlaylist(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting new playlist: " + e.getMessage());
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

    public static void addSongToPlaylist(SongManager.SongInfo song, String playlistName) {
        String sql = """
            INSERT INTO playlist_songs (playlist_id, song_id)
            SELECT p.id, s.id
            FROM playlists p, songs_db.songs s
            WHERE p.name = ? AND s.path = ?
        """;
        try (Connection conn = connectPlaylist()) {
            Statement stmt = conn.createStatement();
            stmt.execute("ATTACH DATABASE '" + SONGS_DB_PATH + "' AS songs_db");
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, playlistName);
            pstmt.setString(2, song.path);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding song to playlist: " + e.getMessage());
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

    public static List<PlaylistInfo> getAllPlaylists() {
        List<PlaylistInfo> playlists = new ArrayList<>();
        createPlaylistTable(); // Ensure table exists
        String sql = """
            SELECT p.name, COUNT(ps.song_id), SUM(s.duration)
            FROM playlists p
            LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id
            LEFT JOIN songs_db.songs s ON ps.song_id = s.id
            GROUP BY p.name
            ORDER BY p.name ASC
        """;
        try (Connection conn = connectPlaylist()) {
            Statement stmt = conn.createStatement();
            stmt.execute("ATTACH DATABASE '" + SONGS_DB_PATH + "' AS songs_db");
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                playlists.add(new PlaylistInfo(
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all playlists: " + e.getMessage());
        }
        return playlists;
    }

    public static List<SongManager.SongInfo> getSongsForPlaylist(String playlistName) {
        List<SongManager.SongInfo> songs = new ArrayList<>();
        String sql = """
            SELECT s.fileName, s.path, s.artist, s.duration
            FROM songs_db.songs s
            JOIN playlist_songs ps ON s.id = ps.song_id
            JOIN playlists p ON ps.playlist_id = p.id
            WHERE p.name = ?
            ORDER BY s.fileName ASC
        """;
        try (Connection conn = connectPlaylist()) {
            Statement stmt = conn.createStatement();
            stmt.execute("ATTACH DATABASE '" + SONGS_DB_PATH + "' AS songs_db");
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, playlistName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                songs.add(new SongManager.SongInfo(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4)
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting songs for playlist: " + e.getMessage());
        }
        return songs;
    }
}