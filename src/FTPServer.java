import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class FTPServer {

    DatagramSocket sock;
    static InetAddress address;
    static int PORT;
    static boolean V6_Mode;
    static int XOR;
    File file;
    int windowSize;
    int mode;
    int file_length;// used for when we are downloading a file

    //Sliding Window Stuff
    ArrayList<DatagramPacket> Data_Buffer;
    DatagramPacket[] Data_Window;
    long [] Data_Timer;
    DatagramPacket [] Ack_Window;
    long [] ACK_Timer;


    public FTPServer(boolean V6) {

        V6_Mode = V6;

    }
    //Networking Functions
    public int Auth() throws IOException {
        Random r = new Random();
        int mod_num;//Modulo num
        int base;//base
        int secret_Num = r.nextInt(600);//secret number
        int A;
        ByteBuffer buff = ByteBuffer.allocate(12);
        ByteBuffer Ack_Buff = ByteBuffer.allocate(4);
        ByteBuffer opcode_buff = ByteBuffer.allocate(1);
        DatagramPacket SYN = new DatagramPacket(buff.array(),buff.array().length);


        sock.receive(SYN);
        buff.put(SYN.getData());
        buff.flip();
        mod_num = buff.getInt();
        System.out.println("Modulo:"+mod_num);
        base = buff.getInt();
        System.out.println("Base:"+base);
        A = buff.getInt();
        System.out.println("Computed:"+A);


        int B = (int) Math.pow(base,secret_Num) % mod_num;
        Ack_Buff.putInt(B);
        Ack_Buff.flip();
        System.out.println("Sending:"+ B);

        address = SYN.getAddress();
        DatagramPacket ACK = new DatagramPacket(Ack_Buff.array(),Ack_Buff.array().length,SYN.getAddress(),SYN.getPort());
        sock.send(ACK);

        int s = (int) Math.pow(A,secret_Num) % mod_num;


        System.out.println("Key:"+s);

        return s;
    }
    public byte [] hash(byte [] input){
        // Define XOR key
        // Any character value will work

        // Define String to store encrypted/decrypted String
        byte[] output = new byte[input.length];

        // calculate length of input string
        int len = input.length;

        // perform XOR operation of key
        // with every caracter in string
        for (int i = 0; i < len; i++) {
            output[i] = (byte)(input[i] ^ XOR);
        }
        return output;
    }

    public void Fill_Data() throws IOException {

        Data_Buffer = new ArrayList<>((int)file.length());

        RandomAccessFile ra = new RandomAccessFile(file, "r");

        byte[] data = new byte[512];

        for (long i=0;i<file.length();i++) {
            ra.seek(i);

            ra.read(data);

            ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 512);

            buffer.putShort((short) i);

            buffer.put(data);

            buffer.flip();

            DatagramPacket packet;

            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, address, PORT);

            Data_Buffer.add(packet);

        }
        ra.close();

    } // Use when sending file

    public void Prep_Data(){
        Data_Buffer = new ArrayList<>(file_length);
    }//used when downloading data


    public void PacketUtilSendFileLength() throws IOException {
        DatagramPacket packet = null;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(file_length);
        buffer.flip();
        packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, address, PORT);
        sock.send(packet);
    }
    public void PacketUtilRecieveFileLength() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        DatagramPacket packet = new DatagramPacket(buffer.array(),buffer.array().length);
        sock.receive(packet);
        buffer.put(packet.getData());
        buffer.flip();
        file_length = (int)buffer.getLong();

    }
    public DatagramPacket Fill_Request(short opcode, String Filename, short mode, int size) {
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2+2+2+8+255);//Opcode + Window Length + Name

        buff.putShort((short) opcode);//2bytes

        buff.putShort((short) mode);//2 bytes

        buff.putShort((short) size);//2 bytes

        buff.putLong(file.length());//8 bytes

        buff.put(Filename.getBytes());//255 max bytes

        buff.flip();

        packet = new DatagramPacket(hash(buff.array()), buff.array().length, address, PORT);



        return packet;
    }

    public void PacketUtil_W_Request(){
        try {
            sock.send(Fill_Request((short) 2,file.getName(),(short)2,windowSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void PacketUtil_R_Request(){
        try {
            sock.send(Fill_Request((short) 1,file.getName(),(short)1,windowSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void Unpack_Request(DatagramPacket p) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2+2+2+8+255);
        buffer.put(p.getData());
        buffer.flip();

        //System.out.println("Opcode:"+buffer.getShort());
        short opcode = buffer.getShort();
        //System.out.println("Mode:"+buffer.getShort());
        mode = buffer.getShort();
        //System.out.println("size:"+buffer.getShort());
        windowSize = buffer.getShort();
        //System.out.println("Length:"+buffer.getInt());
        file_length = (int)buffer.getLong();
        byte b [] = new byte[255];
        buffer.get(b);
        //System.out.println("Filename"+new String(b).trim());
        String name = new String(b).trim();
        //System.out.println(name);
        file = new File(name);
        if (opcode == 1) {
            //Mode 1 : I AM READING FROM HOST

            file_length = (int)file.length();

        } else if (opcode == 2) {
            //Mode 2 : I AM BEING READ FROM
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        }




    }


    public static void main(String[] args) throws IOException {

        FTPServer ftpServer = new FTPServer(false);
        XOR = ftpServer.Auth();

        String file = "file.txt";
        ByteBuffer buffer = ByteBuffer.allocate(269);

        DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
        ftpServer.sock.receive(p);
        ftpServer.Unpack_Request(p);
        if (ftpServer.mode == 2){



        }else if(ftpServer.mode==1){

            ftpServer.PacketUtilSendFileLength();




        }


    }


}
