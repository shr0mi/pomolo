package com;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDBManager {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".LofiMusicPlayer";
    private static final String DB_URL = "jdbc:sqlite:" + APP_DIR + File.separator + "songs.db";
    private static final String DB_URL_PLAYLIST = "jdbc:sqlite:" + APP_DIR + File.separator + "playlists.db";
    private static final String SONGS_DB_PATH = APP_DIR + File.separator + "songs.db";

    public static String getAppDir() {
        return APP_DIR;
    }

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
        String sql = "INSERT OR IGNORE INTO playlists (name) VALUES ('liked_songs')";
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

    public static void deletePlaylist(String name) {
        String sql = "DELETE FROM playlists WHERE name = ?";
        String sql2 = "DELETE FROM playlist_songs WHERE playlist_id = (SELECT id FROM playlists WHERE name = ?)";
        try (Connection conn = connectPlaylist()) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.executeUpdate();

            PreparedStatement pstmt2 = conn.prepareStatement(sql2);
            pstmt2.setString(1, name);
            pstmt2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting playlist: " + e.getMessage());
        }
    }

    public static void removeSongFromPlaylist(SongManager.SongInfo song, String playlistName) {
        String sql = """
            DELETE FROM playlist_songs
            WHERE playlist_id = (SELECT id FROM playlists WHERE name = ?)
            AND song_id = (SELECT id FROM songs_db.songs WHERE path = ?)
        """;
        try (Connection conn = connectPlaylist()) {
            Statement stmt = conn.createStatement();
            stmt.execute("ATTACH DATABASE '" + SONGS_DB_PATH + "' AS songs_db");
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, playlistName);
            pstmt.setString(2, song.path);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing song from playlist: " + e.getMessage());
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

    public static void deleteSong(String path) {
        // First, get the song's ID
        int songId = -1;
        String findIdSql = "SELECT id FROM songs WHERE path = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(findIdSql)) {
            pstmt.setString(1, path);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                songId = rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error finding song ID for deletion: " + e.getMessage());
            return; // Exit if we can't find the song
        }

        if (songId == -1) {
            System.out.println("Song not found in database: " + path);
            return;
        }

        // Next, delete from playlist_songs table
        String deleteFromPlaylistsSql = "DELETE FROM playlist_songs WHERE song_id = ?";
        try (Connection conn = connectPlaylist();
             PreparedStatement pstmt = conn.prepareStatement(deleteFromPlaylistsSql)) {
            pstmt.setInt(1, songId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting song from playlists: " + e.getMessage());
        }

        // Finally, delete from songs table
        String deleteFromSongsSql = "DELETE FROM songs WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(deleteFromSongsSql)) {
            pstmt.setInt(1, songId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Song removed from database: " + path);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting song from main table: " + e.getMessage());
        }
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
            SELECT p.name, COUNT(s.id), SUM(s.duration)
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

    public static void verifyAndCleanSongDatabase() {
        createTable(); // Ensure the table exists before we start.
        List<String> pathsToDelete = new ArrayList<>();
        String sql = "SELECT path FROM songs";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String path = rs.getString("path");
                if (path != null && !new File(path).exists()) {
                    pathsToDelete.add(path);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying song database: " + e.getMessage());
            return; // Exit if we can't read the database.
        }

        // Now, delete all the invalid paths we found.
        if (!pathsToDelete.isEmpty()) {
            System.out.println("Found " + pathsToDelete.size() + " missing songs. Cleaning database...");
            for (String path : pathsToDelete) {
                deleteSong(path); // Use the existing deleteSong method.
            }
            System.out.println("Database cleaning complete.");
        }
    }
}
