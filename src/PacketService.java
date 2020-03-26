import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;
import java.util.Scanner;




public class PacketService {


    public File file;

    boolean V6_Mode;
    boolean PacketLossMode;
    Inet4Address Hostv4;
    Inet6Address Hostv6;
    int PORT;
    private int XOR;

    public PacketService(boolean V6, boolean PacketLossMode, InetAddress Host, int port,int XOR){
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


        */

        V6_Mode = V6;

        XOR = this.XOR;

        PacketLossMode = this.PacketLossMode;

        if (V6){
            Hostv6 = (Inet6Address)Host;

        }else Hostv4 = (Inet4Address)Host;

        PORT = port;

    }
    //Hash Fn
    public byte [] hash(byte [] b){
        //we need int XOR


        //TODO Make hash funtion


        return b;
    }



    //Handler
    public DatagramPacket Handler(DatagramPacket p){
        // Takes in DataGramPacket

        // Sends it to the appropriate unPack Function


        //Read first 2 bytes from packet

        byte [] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this

        byte [] opcode = {raw_packet[0],raw_packet[2]};

        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(2).put(opcode).flip();

        short code = buff.getShort();



        /*  1     Read request (RRQ)
            2     Write request (WRQ)
            3     Data (DATA)
            4     Acknowledgment (ACK)
            5     Error (ERROR)
        */

        switch (code){




            case 1://Read
                Unpack_Request(p.getData(),1);
                break;
            case 2://Write
                Unpack_Request(p.getData(),2);

                break;
            case 3://Data

                break;
            case 4:

                break;
            case 5:

                break;
            default:
                System.out.println("[Error] Packet Malformed!");

        }





        return null;
    }









    // Fill RWRQ Packet

    public DatagramPacket Fill_Request(String Filename,short mode){
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2+Filename.length()+1);

        buff.putShort((short)mode);

        buff.put(Filename.getBytes());

        buff.put((byte)0);



        buff.flip();

        if (V6_Mode){
            packet = new DatagramPacket(hash(buff.array()),buff.array().length,Hostv6,PORT);

        }else packet = new DatagramPacket(hash(buff.array()),buff.array().length,Hostv4,PORT);


        return packet;
    }

    // Fill RWRQ Packet


    public int Unpack_Request(byte [] raw_packet,int opcode){

        int mode = 0;

        ByteBuffer buffer = ByteBuffer.allocate(2+256);// 2 bytes is for the opcode| 255 bytes is max length for name in Linux | +1 For padding byte

        buffer.put(raw_packet);// need hash her to XOR it back

        buffer.flip();

        if (opcode == 1){
            // i wanna read from you
            //Downloading
            byte [] b = new byte[255];

            buffer.get(b);

            file = new File(new String(b));
            mode = 1;

        }else if(opcode == 2){
            //i wanna write a file to you
            //uploading
            mode = 2;

        }


        // Probably want to make global for client / Server to read to understand if they are Writer or Reader


        return mode;
    }


    // Fill Data Packet

    public DatagramPacket Fill_Data(short BlockNum,byte [] data){

        ByteBuffer buffer = ByteBuffer.allocate(2+2+512000);


        buffer.putShort(BlockNum);
        buffer.put(data);
        buffer.flip();

        DatagramPacket packet;

        if (V6_Mode){
            packet = new DatagramPacket(hash(buffer.array()),buffer.array().length,Hostv6,PORT);

        }else packet = new DatagramPacket(hash(buffer.array()),buffer.array().length,Hostv4,PORT);


        return packet;
    }

    //Unpack Data Pack

    public byte[] UnPack_Data(byte [] raw_packet){

        ByteBuffer buffer = ByteBuffer.allocate(2+2+512000);
        buffer.put(raw_packet);
        buffer.flip();

        buffer.getShort();

        short BlockNum = buffer.getShort();

        byte [] data = new byte[512000];

        // Write to file




        return null;
    }


}
