import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;
import java.util.Scanner;


public class PacketService {


    public static File file;

    boolean V6_Mode;
    boolean PacketLossMode;
    Inet4Address Hostv4;
    Inet6Address Hostv6;
    int PORT;
    private int XOR;
    int MODE;


    public PacketService(boolean V6, boolean PacketLossMode, InetAddress Host, int port, int XOR) {
        /*
        V6 [TYPE:boolean]: Are the packets V6 or not?
        PackelossMode [TYPE boolean]: PacketLossMode if true enable's 1% packet loss
        Host [TYPE InetAddress]: Host will be casted to V4 or V6 address , based on V6
        port [TYPE port]: port number
        XOR  [TYPE int ]: XOR is the key we get from Auth to tell us by what factor we should XOR bytes

        This class will have the ability to :

        -Fill & UnPack :
                Packet TYPES: RWRQ, DATA , ACK,and ERROR
          opcode  operation

            1     Read request (RRQ)
            2     Write request (WRQ)
            3     Data (DATA)
            4     Acknowledgment (ACK)
            5     Error (ERROR)



         -Write to a file


         -Alert Server and or Client if they are Uploading or Downloading


         NOTES :

            In the Error Packet and The Read/Write Request I have eliminated the byte at the end to pad the string.

            I have limited all Strings sent to 255 bytes, due to OS's Windows and Linux max filename length is 255 bytes (*This does not account for Path size)

            Error messsages I've changed to 255 bytes which is more than enough.


        */


        V6_Mode = V6;

        XOR = this.XOR;

        PacketLossMode = this.PacketLossMode;

        if (V6) {
            Hostv6 = (Inet6Address) Host;

        } else Hostv4 = (Inet4Address) Host;

        PORT = port;

    }

    //Hash Fn
    public byte[] hash(byte[] b) {
        //we need int XOR


        //TODO Make hash funtion


        return b;
    }


    //Handler
    public DatagramPacket Handler(DatagramPacket p) throws IOException {
        // Takes in DataGramPacket

        // Sends it to the appropriate unPack Function


        //Read first 2 bytes from packet

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this

        byte[] opcode = {raw_packet[0], raw_packet[2]};

        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(2).put(opcode).flip();

        short code = buff.getShort();

        /*  1     Read request (RRQ)
            2     Write request (WRQ)
            3     Data (DATA)
            4     Acknowledgment (ACK)
            5     Error (ERROR)
        */

        switch (code) {

            case 1://Read
                MODE = Unpack_Request(raw_packet, 1);
                break;
            case 2://Write
                MODE = Unpack_Request(raw_packet, 2);

                break;
            case 3://Data
                UnPack_Data(raw_packet);
                break;
            case 4:
                UnPack_Ack(raw_packet);
                break;
            case 5:
                UnPack_Error(raw_packet);
                break;
            default:
                System.out.println("[Error] Packet Malformed!");

        }


        return null;
    }


    // Fill RWRQ Packet

    public DatagramPacket Fill_Request(String Filename, short mode) {
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2 + 255);

        buff.putShort((short) mode);

        buff.put(Filename.getBytes());

        buff.flip();

        if (V6_Mode) {
            packet = new DatagramPacket(hash(buff.array()), buff.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buff.array()), buff.array().length, Hostv4, PORT);


        return packet;
    }

    // Fill RWRQ Packet


    public int Unpack_Request(byte[] raw_packet, int opcode) {

        int mode = 0;

        ByteBuffer buffer = ByteBuffer.allocate(2 + 255);// 2 bytes is for the opcode| 255 bytes is max length for name in Linux and Windows|

        buffer.put(raw_packet);// need hash her to XOR it back

        buffer.flip();

        if (opcode == 1) {
            // i wanna read from you
            //Downloading
            byte[] b = new byte[255];

            buffer.get(b);

            file = new File(new String(b));
            mode = 1;

        } else if (opcode == 2) {
            //i wanna write a file to you
            //uploading
            mode = 2;

        }


        // Probably want to make global for client / Server to read to understand if they are Writer or Reader


        return mode;
    }


    // Fill Data Packet

    public DatagramPacket Fill_Data(short BlockNum) throws IOException {

        RandomAccessFile ra = new RandomAccessFile(file, "r");
        byte[] data = new byte[512];
        ra.seek((long) BlockNum);
        ra.read(data);
        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 512);

        buffer.putShort(BlockNum);
        buffer.put(data);
        buffer.flip();
        DatagramPacket packet;

        if (V6_Mode) {
            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv4, PORT);

        ra.close();

        return packet;
    }

    //Unpack Data Pack

    public void UnPack_Data(byte[] raw_packet) throws IOException {// Returns Void due to it just taking care of writing to the File
        RandomAccessFile ra = new RandomAccessFile(file, "w");
        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 512);
        buffer.put(raw_packet);
        buffer.flip();

        buffer.getShort();

        short BlockNum = buffer.getShort();

        byte[] data = new byte[512];

        ra.seek((long) BlockNum);

        ra.write(data);

        ra.close();

        // Write to file


    }


    //Fill ACK
    public DatagramPacket Fill_Ack(short BlockNum) {

        DatagramPacket packet = null;

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort((short) 4);
        buffer.putShort(BlockNum);
        buffer.flip();

        if (V6_Mode) {
            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv4, PORT);

        return packet;
    }

    //UnPack_Ack

    public short UnPack_Ack(byte[] raw_packet) {

        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put(raw_packet);

        buffer.flip();

        buffer.getShort();

        return buffer.getShort();

    }


    //Fill_Error Message

    public DatagramPacket Fill_Error(short ErrorCode, String ErrorMsg) {

        DatagramPacket packet = null;

        // Error Message is limited to 255 characters

        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 255);

        buffer.putShort((short) 5);

        buffer.putShort(ErrorCode);

        buffer.put(ErrorMsg.getBytes());

        buffer.flip();

        if (V6_Mode) {
            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv4, PORT);


        return packet;
    }


    //UnPack Error
    public String UnPack_Error(byte[] raw_packet) {

        ByteBuffer buffer = ByteBuffer.allocate(2 + 255);// size of packet:259

        buffer.put(raw_packet);

        buffer.flip();

        short ErrorCode = buffer.getShort();

        byte[] msg = new byte[255];

        String Error_msg = new String(buffer.get(msg).array());

        return "[Error]:" + ErrorCode + "- " + Error_msg;
    }


    //Testing Area
    public static void main(String[] args) throws IOException {
        file = new File("file.txt");
        PacketService n = new PacketService(false, false, InetAddress.getByName("localhost"), 2770, 12);
    }


}
