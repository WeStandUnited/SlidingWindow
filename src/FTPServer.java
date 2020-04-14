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
    static int MODE;
    long file_length;// used for when we are downloading a file


    public FTPServer(boolean V6,int port) throws SocketException {

        V6_Mode = V6;
        PORT = port;

        sock = new DatagramSocket(PORT);
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
        file_length = buffer.getLong();

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
        buffer.put(hash(p.getData()));
        buffer.flip();

        short opcode = buffer.getShort();
        System.out.println("Opcode:"+opcode);

        short mode = buffer.getShort();
        System.out.println("Mode:"+mode);

        windowSize = buffer.getShort();
        System.out.println("windowsize:"+windowSize);

        file_length = buffer.getLong();

        System.out.println("Length:"+file_length);

        byte[] b = new byte[255];
        buffer.get(b);
        //System.out.println("Filename"+new String(b).trim());
        String name = new String(b).trim();
        //System.out.println(name);
        file = new File(name);
        if (opcode == 1) {
            //Mode 1 : I AM READING FROM HOST
            MODE = 1;
            file_length = file.length();

        } else if (opcode == 2) {
            //Mode 2 : I AM BEING READ FROM
            MODE = 2;

            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        }




    }
    public DatagramPacket Fill_Ack(short BlockNum) {

        DatagramPacket packet = null;

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort((short) 4);//opcode
        buffer.putShort(BlockNum);
        buffer.flip();


        packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, address, PORT);



        return packet;
    }
    public short UnPack_Data(DatagramPacket p) throws IOException {// Returns Void due to it just taking care of writing to the File
        RandomAccessFile ra = new RandomAccessFile(file, "w");
        ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 512);
        buffer.put(hash(p.getData()));
        buffer.flip();

        buffer.getShort();

        short BlockNum = buffer.getShort();

        byte[] data = new byte[512];



        ra.seek((long) BlockNum);

        ra.write(buffer.get(data).array());

        ra.close();

        // Write to file

        return BlockNum;
    }
    public short getBlockNumber(DatagramPacket p){

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this



        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();

        buff.getShort();

        return buff.getShort();
    }


    public static void main(String[] args) throws IOException {

        FTPServer ftpServer = new FTPServer(false,2770);

        XOR = ftpServer.Auth();

        ByteBuffer buffer = ByteBuffer.allocate(269);

        DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
        ftpServer.sock.receive(p);
        ftpServer.Unpack_Request(p);
        SlideWindow window = new SlideWindow(ftpServer.windowSize,(int)ftpServer.file_length,XOR);
        window.setFile(ftpServer.file);

        //if Downloading from the Server

        if (MODE == 1){

            while (!(window.isFull(window.Data_Array))){
                ByteBuffer b = ByteBuffer.allocate(512);
                DatagramPacket packet = new DatagramPacket(b.array(),b.array().length);
                ftpServer.sock.receive(packet);
                window.add_Data(packet);
            }
            for(int i=0;i<ftpServer.windowSize;i++){

                ftpServer.sock.send(ftpServer.Fill_Ack(ftpServer.UnPack_Data(window.Data_Array[i])));
                window.remove(i);
            }






        }else if (MODE == 2){
            window.Fill_Data();

            //if uploading to the server

            //fill data buffer with file
            //fill data array
            window.Fill_Data_Array();
            //send
            for (int i=0;i<ftpServer.windowSize;i++) {
                ftpServer.sock.send(window.Data_Array[i]);
                window.add_Timer(i);//set timers


            }

            //wait for ACK packet then check timers
            while (!(window.isFull(window.Ack_Array))) {
                ByteBuffer buff = ByteBuffer.allocate(4);
                DatagramPacket pa = new DatagramPacket(buff.array(),buff.array().length);
                ftpServer.sock.receive(pa);
                window.add_ACK(pa);
                window.remove(window.getData(ftpServer.getBlockNumber(p)));
                //check all timers
                int index = window.checkTimers();
                if (index != -1){
                    ftpServer.sock.send(window.Data_Array[index]);
                }
            }
            window.clearAcks();



        }

    }


}