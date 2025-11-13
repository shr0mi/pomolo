package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class SimpleHttpServer {
    // store the current track being served
    private static volatile File currentMusicFile = null;


    public static void startHttpServer(){
        try {
            // Create an HttpServer instance
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/", new MyHandler());



            // Serve MP3 dynamically
            server.createContext("/music", new MusicHandler());

            // Start web socket server
            WebSocketLauncher.startServer();

            // Start htttp server with default configuration
            server.setExecutor(null);
            server.start();
            System.out.println("HTTP server at http://<host-ip>:8081");

            // Control Test
//            Scanner sc = new Scanner(System.in);
//            while(true) {
//                System.out.println("Enter code:");
//                System.out.flush();
//                int x = Integer.parseInt(sc.nextLine().trim());
//                if(x==1){
//                    SyncWebSocketServer.broadcast("PLAY");
//                    System.out.println("PLAY");
//                }else if(x==2){
//                    SyncWebSocketServer.broadcast("PAUSE");
//                }else{
//                    System.out.println("Enter track path:");
//                    String path = sc.nextLine().trim();
//                    File music = new File(path);
//                    setCurrentMusic(music);
//                }
//            }

        } catch (IOException e){
            System.out.println("Error starting the server: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- NEW METHOD ---
    public static void setCurrentMusic(File file) {
        if (file != null && file.exists() && file.isFile()) {
            currentMusicFile = file;
            SyncWebSocketServer.broadcast("NEW_TRACK:/music?t=" + System.currentTimeMillis());
            System.out.println("Now serving: " + file.getName());
        } else {
            System.out.println("Invalid file selected for streaming.");
        }
    }

    // --- NEW HANDLER FOR /music ---
    static class MusicHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (currentMusicFile == null) {
                String msg = "No music file selected yet.";
                exchange.sendResponseHeaders(404, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes());
                }
                return;
            }

            long fileLength = currentMusicFile.length();
            String range = exchange.getRequestHeaders().getFirst("Range");

            long start = 0;
            long end = fileLength - 1;

            if (range != null && range.startsWith("bytes=")) {
                String[] parts = range.substring(6).split("-");
                try {
                    start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (end >= fileLength) end = fileLength - 1;
            long contentLength = end - start + 1;

            // Set headers
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(contentLength));

            if (range != null) {
                exchange.sendResponseHeaders(206, contentLength); // Partial Content
            } else {
                exchange.sendResponseHeaders(200, fileLength); // Full file
            }

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(currentMusicFile)) {

                fis.skip(start);
                byte[] buffer = new byte[8192];
                long bytesRemaining = contentLength;

                while (bytesRemaining > 0) {
                    int bytesToRead = (int) Math.min(buffer.length, bytesRemaining);
                    int bytesRead = fis.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) break;
                    os.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
            }
        }
    }


    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            String response = """
                <!DOCTYPE html>
                <html>
                <body>
                  <h2>Shared Music Player</h2>
                  <audio id='player' controls></audio>
                  <script>
                    const audio = document.getElementById('player');
                    audio.src = '/music';

                    const ws = new WebSocket('ws://' + location.hostname + ':8080/sync');
                    console.log('ws://' + location.hostname + ':8080/sync');
                    ws.onmessage = e => {
                        if (e.data === 'PLAY') audio.play();
                        else if (e.data === 'PAUSE') audio.pause();
                        else if (e.data.startsWith('SEEK:')){
                            const time = parseFloat(e.data.split(':')[1]);
                          
                            // Ensure audio is ready before seeking
                            if (audio.readyState >= 2) {  // HAVE_METADATA
                                audio.currentTime = time;
                                console.log(time);
                                console.log(audio.currentTime);
                            } else {
                                // Wait until metadata (duration, etc.) is loaded
                                audio.addEventListener('loadedmetadata', () => {
                                audio.currentTime = time;
                                console.log(time);
                                console.log(audio.currentTime);
                                }, { once: true });
                            }
                        }else if (e.data.startsWith('NEW_TRACK:')) {
                            audio.src = e.data.split(':')[1];
                            audio.load();
                            audio.preload = 'auto'; // Force full buffering
                            audio.play();
                        }
                    };
                  </script>
                </body>
                </html>
            """;
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
