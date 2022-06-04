package tcp;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Main {

    public static void main(String[] args) throws IOException {
        StartServer();
    }
    public static void StartClient() throws IOException {
        TSPSocket socket= new TSPSocket(0);
        socket.connect("localhost", 12313);
        byte[] data ;

        data = socket.receive();
        String string = new String(data, Charset.forName("UTF-8"));
        for(int i = 0 ; i< 10 ; i++){
            System.out.print(data[i] +" ");
        }
        socket.disconnect();

    }
    public static void StartServer() throws IOException {
        TSPSocket socket= new TSPSocket(0);
        socket.listen(12313);
        String fileToSend = "text.txt";

        String text = Files.readString(Paths.get(fileToSend),  Charset.forName("UTF-8"));
        byte[] data = text.getBytes();
        socket.send(data, data.length);
    }

}
