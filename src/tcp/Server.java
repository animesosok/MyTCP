package tcp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
    public static void main(String[] args) throws IOException {
        StartServer();
    }
    public static void StartServer() throws IOException {
        TCPSocket socket= new TCPSocket(0);
        socket.listen(12313);
        String fileToSend = "text.txt";

        String text = Files.readString(Paths.get(fileToSend),  Charset.forName("UTF-8"));
        byte[] data = text.getBytes();
        socket.send(data, data.length);
    }
}
