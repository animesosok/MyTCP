package tcp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/* Packet structure:
    0 byte - Flags
    1 - 4 bytes - Sequence number
    5 - 8 bytes - Ack number
    9 - 1023 - Data

    Max data size 1015 bytes
 */

public class Packet {
    // Flags
    static public final byte NO_FLAGS = 0;
    static public final byte SEQ = 8;
    static public final byte SYN = 2;
    static public final byte FIN = 4;
    static public final byte ACK = 1;

    static public final int MAX_PACKET_SIZE = 1024; //bytes
    static public final int MAX_DATA_SIZE = 1015;
    static public final int HEADER_SIZE = 9;

    static DatagramPacket createPacket(byte flag, int seqNum, int ackNum,
                                       InetAddress address, int port,
                                       byte[] data, int dataSize)
    {
        int sendSize = dataSize + HEADER_SIZE;
        ByteBuffer buff = ByteBuffer.allocate(sendSize);
        buff.put(flag);
        buff.putInt(seqNum);
        buff.putInt(ackNum);
        if (data != null) {
            buff.put(data, 0, dataSize);
        }
        byte[] sendData = buff.array();
        return new DatagramPacket(sendData, sendSize, address, port);
    }

    static int isACK(DatagramPacket packet){
        int ackNum = -1;
        byte[] packetData = packet.getData();
        if((packetData[0] & ACK) == ACK){
            ackNum = getACKNum(packetData);
        }
        return ackNum;
    }
    static int getACKNum(byte[] packetData){
        int ackNum;
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.put(packetData,5, 4);
        buff.position(0);
        ackNum = buff.getInt();
        return ackNum;
    }
    static int getSEQNum(byte[] packetData){
        int seqNum;
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.put(packetData,1, 4);
        buff.position(0);
        seqNum = buff.getInt();
        return seqNum;
    }
    static byte[] getData(DatagramPacket packet){
        byte[] packetData = packet.getData();
        ByteBuffer buff = ByteBuffer.allocate(MAX_DATA_SIZE);
        buff.put(packetData, 9, getLength(packet));
        return buff.array();
    }
    static int getLength(DatagramPacket packet){
        return packet.getLength() - Packet.HEADER_SIZE;
    }
    static boolean isSYN(DatagramPacket packet){
        byte[] packetData = packet.getData();
        if((packetData[0] & SYN) == SYN){
            return true;
        }
        return false;
    }
    static boolean isFIN(DatagramPacket packet){
        byte[] packetData = packet.getData();
        if((packetData[0] & FIN) == FIN){
            return true;
        }
        return false;
    }
}
