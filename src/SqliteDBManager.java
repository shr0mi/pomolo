import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteDBManager {

    // Connect with the DB and return the connection
    public static Connection connect() throws SQLException{
        String url = "jdbc:sqlite:songs.db";
        var conn = DriverManager.getConnection(url);
        return  conn;
    }

    // Check if table exists. If it does not then create one
    public static void createTable(){
        String createSongs = """
        CREATE TABLE IF NOT EXISTS songs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            fileName TEXT,
            path TEXT NOT NULL UNIQUE,
            artist TEXT,
            duration INTEGER
        );
        """;

        try(Connection c = connect();
            var stmt = c.createStatement()
        ){
            stmt.execute(createSongs);
            System.out.println("Table was created");
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    // Check if song exists in the "Database" songs already
    public static boolean songExists(String path){
        String sql = "SELECT 1 FROM songs WHERE path = ?";
        try(Connection c = connect();
            var pstmt = c.prepareStatement(sql)
        ){
            pstmt.setString(1, path);
            var rs = pstmt.executeQuery();
            return rs.next();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static void insertNewSong(SongManager.SongInfo s){
        createTable();
        if(songExists(s.path)){
            System.out.println("The song already exists in DB");
            return;
        }

        String sql = "INSERT INTO songs(fileName,path,artist,duration) VALUES(?,?,?,?)";
        try(Connection c = connect();
            var pstmt = c.prepareStatement(sql)
        ){
            pstmt.setString(1, s.fileName);
            pstmt.setString(2, s.path);
            pstmt.setString(3, s.artist);
            pstmt.setInt(4, s.duration);
            pstmt.executeUpdate();
            System.out.println("Song was inserted");
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    // Get All songs (for the homepage)
    public static List<SongManager.SongInfo> getAllSongs(){
        List<SongManager.SongInfo> songs = new ArrayList<>();

        String sql = "SELECT fileName, path, artist, duration FROM songs";

        try(Connection c = connect();
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ){
            while(rs.next()){
                SongManager.SongInfo s = new SongManager.SongInfo(
                        rs.getString("fileName"),
                        rs.getString("path"),
                        rs.getString("artist"),
                        rs.getInt("duration")
                );

                songs.add(s);

            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return songs;
    }


}
