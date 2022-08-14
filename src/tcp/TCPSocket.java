package tcp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;


public class TCPSocket {
    private DatagramSocket socket;
    private InetAddress destAddress;
    private int destPort;
    private int  seqNumber;
    private double badconnect;

    private boolean connection = false;

    private final int WAIT_TIME = 100; // ms
    private final int WINDOW_SIZE = 5;
    private final int TIMEOUT = 50; // ms
    private final int CONNECT_TRIES = 32;


    public TCPSocket(double bad) {
        badconnect = bad;
    }
    // Send datagrams and change seqNumber
    private void sendPackets(DatagramPacket[] packets, int numPackets) throws IOException {
        DatagramPacket recvPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE );
        int windowPos = 0;
        int lastSend = -1;
        long lastSendTime = System.currentTimeMillis();
        int lastAck = -1;
        System.out.println(numPackets);
        while (lastAck+1 < numPackets){
            if (windowPos + WINDOW_SIZE - 1 > lastSend){
                for(int i = lastSend + 1; i < windowPos + WINDOW_SIZE && i < numPackets; i++){
                    try {
                        socket.send(packets[i]);

                    } catch (IOException ignored) {}
                    lastSend++;
                    System.out.println("Try to send: " + Packet.getSEQNum(packets[i].getData()));
                }

                lastSendTime = System.currentTimeMillis();
            }
            if(lastAck < lastSend){
                try {
                    socket.receive(recvPacket);
                    // If client cant recv SYN ack
                    if (Packet.isSYN(recvPacket)) {
                        try {
                            socket.send(Packet.createPacket(Packet.ACK, seqNumber, seqNumber, destAddress, destPort, null, 0));
                        } catch (IOException ignored) {}
                        continue;
                    }
                    int ackNum = Packet.isACK(recvPacket) - seqNumber;
                    if (ackNum >= 0){
                        // If double ACK
                        if(ackNum < windowPos){
                            socket.send(packets[windowPos]);
                        }
                        else {
                            lastAck = ackNum;
                            System.out.println("Sended: " + Packet.isACK(recvPacket));
                        }
                    }
                } catch (IOException ignored) {}

            }
            if (lastAck >= windowPos){
                windowPos = lastAck + 1;
            }
            if (System.currentTimeMillis() - lastSendTime > WAIT_TIME){
                for(int i = windowPos; i < lastSend+1; i++){
                    try {
                        socket.send(packets[i]);
                        System.out.println("Try to send: " + Packet.getSEQNum(packets[i].getData()));
                    } catch (IOException ignored) {}
                }
                lastSendTime = System.currentTimeMillis();
            }

        }
        seqNumber += numPackets;
    }
    private DatagramPacket[] recvPackets(int numPackets){
        DatagramPacket[] packets = new DatagramPacket[numPackets];
        DatagramPacket newPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        int totalRecv = 0;
        int windowPos = 0;
        int seq;

        while( totalRecv < numPackets){
            try {
                socket.receive(newPacket);
                if(Math.random() < badconnect){
                    System.out.println("My packet lose");
                    continue;
                }
                seq = Packet.getSEQNum(newPacket.getData());
                System.out.println("Packet received: "+ seq);
                if(Packet.isFIN(newPacket)){
                    connection = false;
                    break;
                }
                if (seq > -1){
                    if( seq >= seqNumber && packets[seq - seqNumber] == null)  {
                        packets[seq - seqNumber] = newPacket;
                        newPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
                        totalRecv++;
                        while (windowPos < numPackets && packets[windowPos] != null ){
                            windowPos++;
                        }
                    }
                    sendACK(windowPos -1 + seqNumber);
                }

            } catch (IOException ignored) {}
        }
        return packets;
    }
    private void sendACK(int ackNumber){
        DatagramPacket ack = Packet.createPacket(Packet.ACK, ackNumber, ackNumber, destAddress, destPort, null, 0);
        try {
            socket.send(ack);
        } catch (IOException ignored) {}
        System.out.println("ACK sended:" + ackNumber);
    }

    public void connect(String address, int port) throws IOException {
        socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        destAddress = InetAddress.getByName(address);
        destPort = port;
        seqNumber = (int) (Math.random() * (2048));  // Randomize first seqNumber
        connection = true;

        DatagramPacket syn = Packet.createPacket(Packet.SYN, seqNumber, seqNumber, destAddress, destPort, null, 0);
        DatagramPacket recvPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        int tries = 0; // Tries to connect
        while (tries < CONNECT_TRIES) {
            try {
                socket.send(syn);
                socket.receive(recvPacket);
                if( Packet.isACK(recvPacket) >= 0){
                    break;
                }
            } catch (IOException ignored) {}
            tries++;
        }
        if (tries >= CONNECT_TRIES){
            connection = false;
        }
    }
    public void send(byte[] data, int size) throws IOException {
        if (!connection){
            System.out.println("No connection");
            return;
        }

        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt(size);
        DatagramPacket recvPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        DatagramPacket sizePacket = Packet.createPacket(Packet.NO_FLAGS, seqNumber, seqNumber, destAddress, destPort, buff.array(), 4);
        int tries = 0;
        while (tries < CONNECT_TRIES) {
            try {
                socket.send(sizePacket);
                socket.receive(recvPacket);
                if( Packet.isACK(recvPacket) == seqNumber){
                    seqNumber++;
                    break;
                }
            } catch (IOException ignored) {}
            tries++;
        }
        if (tries >= CONNECT_TRIES){
            connection = false;
        }
        buff.clear();

        int offset = 0;
        int numToSend = size / Packet.MAX_DATA_SIZE;
        buff = ByteBuffer.allocate(Packet.MAX_DATA_SIZE);

        DatagramPacket[] sendPackets = new DatagramPacket[numToSend + 1];
        for(int i = 0; i < numToSend; i++){
            buff.put(data, offset, Packet.MAX_DATA_SIZE);
            sendPackets[i] = Packet.createPacket(Packet.NO_FLAGS, seqNumber+i, seqNumber+i,
                    destAddress, destPort, buff.array(), Packet.MAX_DATA_SIZE);
            buff.clear();
            offset += Packet.MAX_DATA_SIZE;
        }
        if (size % Packet.MAX_DATA_SIZE > 0){
            buff.put(data, offset, size % Packet.MAX_DATA_SIZE);
            sendPackets[numToSend] = Packet.createPacket(Packet.NO_FLAGS, seqNumber+numToSend, seqNumber+numToSend,
                    destAddress, destPort, buff.array(), size % Packet.MAX_DATA_SIZE);
            buff.clear();
            numToSend++;
        }

        sendPackets(sendPackets, numToSend);
    }
    public byte[] receive(){
        if (!connection){
            System.out.println("No connection");
            return null;
        }
        ByteBuffer buff;
        int size = 0;
        // Get size data ro recv
        DatagramPacket recvPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        int tries = 0;
        while (tries < CONNECT_TRIES) {
            try{
                socket.receive(recvPacket);
                if( Packet.getLength(recvPacket) == 4){
                    buff = ByteBuffer.allocate(4);
                    buff.put(Packet.getData(recvPacket), 0, Packet.getLength(recvPacket));
                    buff.position(0);
                    size = buff.getInt();
                    buff.clear();
                    break;
                }
            } catch (IOException ignored) {}
            tries++;
        }
        if (tries >= CONNECT_TRIES){
            connection = false;
            return null;
        }
        sendACK(seqNumber);
        seqNumber++;
        int dataSize = size;
        int recvNum = size / Packet.MAX_DATA_SIZE;
        if (size % Packet.MAX_DATA_SIZE > 0){
            recvNum ++;
        }

        DatagramPacket[] packets = recvPackets(recvNum);
        buff = ByteBuffer.allocate(size);

        for(int n = 0; n < recvNum; n++){
            DatagramPacket i = packets[n];
            buff.put(Packet.getData(i), 0, Packet.getLength(i));
        }
        seqNumber += size;
        return buff.array();
    }
    public void listen(int port) throws UnknownHostException {
        try {
            socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
            socket.setSoTimeout(TIMEOUT);
        } catch (SocketException ignored) {}

        DatagramPacket newPacket = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        while(true){
            try {
                socket.receive(newPacket);
                System.out.println("Connection created");
                if(Packet.isSYN(newPacket)){
                    connection = true;
                    seqNumber = Packet.getSEQNum(newPacket.getData());
                    destPort = newPacket.getPort();
                    destAddress = newPacket.getAddress();
                    break;
                }
            } catch (IOException ignored) {}
        }
        try{
            socket.send(Packet.createPacket(Packet.ACK, seqNumber, seqNumber, destAddress, destPort, null, 0));
        } catch (IOException ignored) {}
    }
    public void disconnect(){
        DatagramPacket ack = Packet.createPacket(Packet.FIN, seqNumber, seqNumber, destAddress, destPort, null, 0);
        try {
            socket.send(ack);
        } catch (IOException ignored) {}
        connection = false;
    }

}
