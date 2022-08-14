package tcp;

import java.io.IOException;
import java.nio.charset.Charset;

public class Client {
    public static void main(String[] args) throws IOException {
        StartClient();
    }
    public static void StartClient() throws IOException {
        TCPSocket socket= new TCPSocket(0);
        socket.connect("localhost", 12313);
        byte[] data ;

        data = socket.receive();
        String string = new String(data, Charset.forName("UTF-8"));
        for(int i = 0 ; i< 10 ; i++){
            System.out.print(data[i] +" ");
        }
        socket.disconnect();

    }
}
