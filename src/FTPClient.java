import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


public class FTPClient {
    DatagramSocket sock;
    static InetAddress address;
    static int PORT;
    static boolean V6_Mode;
    static int XOR;
    File file;
    long file_length = 0;
    int windowSize;
    static int MODE;


    public FTPClient(InetAddress a, boolean V6,int port) throws SocketException {

        PORT = port;
        V6_Mode = V6;
        sock = new DatagramSocket(PORT);
        if (V6_Mode){
            address = (Inet6Address)a;
        }else {
            address = (Inet4Address)a;
        }

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

    public int Auth() throws IOException {
        Random r = new Random();

        int mod_num = NumberGen.getPrime(600);//Public Modulo
        int base= r.nextInt(600);//Public base
        int secret_num = r.nextInt(600);//Secret number
        int A = (int) Math.pow(base,secret_num) % mod_num;

        ByteBuffer buff = ByteBuffer.allocate(12);// Stuffing are byte buffer

        ByteBuffer ACK_Buff = ByteBuffer.allocate(4);
        DatagramPacket Ack = new DatagramPacket(ACK_Buff.array(),ACK_Buff.array().length);




        buff.putInt(mod_num);
        System.out.println("Modulo:"+mod_num);
        buff.putInt(base);
        System.out.println("Base:"+base);
        buff.putInt(A);
        System.out.println("Computed:"+A);
        buff.flip();


        DatagramPacket SYN  = new DatagramPacket(buff.array(),buff.array().length,address,PORT);
        sock.send(SYN);

        sock.receive(Ack);
        ACK_Buff.put(Ack.getData());
        ACK_Buff.flip();

        int B = ACK_Buff.getInt();
        System.out.println("Bob sent:"+B);

        int s = (int)Math.pow(B,secret_num) % mod_num;
        System.out.println("Key:"+s);






        return s;
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
        buffer.put(hash(packet.getData()));
        buffer.flip();
        file_length = (int)buffer.getLong();

    }
    public DatagramPacket Fill_Request(short opcode,String Filename, short mode,int size) {
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2+2+2+8+255);//Opcode + Window Length + Name

        buff.putShort((short) opcode);//2bytes

        System.out.println("opcode:"+opcode);

        buff.putShort((short) mode);//2 bytes

        System.out.println("mode :"+mode);

        buff.putShort((short) size);//2 bytes

        System.out.println("window size :"+size);

        buff.putLong(file_length);//8 bytes
        System.out.println("file_length:"+file_length);

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
        buffer.put(p.getData());
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

    public static void main(String[] args) throws IOException {
        Scanner kb = new Scanner(System.in);
        FTPClient ftpClient = new FTPClient(InetAddress.getByName("192.168.1.11"),false,2770);
        System.out.println("[Waiting to Auth]");
        XOR = ftpClient.Auth();
        System.out.println("[Authed with]"+"\n"+"Host:"+address.getHostName()+"\n"+"Port:"+2770);
        System.out.println("Downloading from Server [2] or Uploading to Server [1]");
        MODE = kb.nextInt();
        ftpClient.windowSize = 10;
        String file_name = "file.txt";
        ftpClient.file = new File(file_name);

        if (MODE == 2){
            ftpClient.file_length = ftpClient.file.length();
            ftpClient.PacketUtil_W_Request();
        }else if(MODE==1){

            ftpClient.PacketUtil_R_Request();
            ftpClient.PacketUtilRecieveFileLength();
            System.out.println("[New File length]:"+ ftpClient.file_length);
        }


        //make window structure
        SlideWindow window = new SlideWindow(ftpClient.windowSize,(int)ftpClient.file_length,XOR);
        window.setFile(ftpClient.file);
        //if Downloading from the Server
        if (MODE == 1){

            while (!(window.isFull(window.Data_Array))){//TODO change to when Data array != full
                ByteBuffer buffer = ByteBuffer.allocate(512);
                DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
                ftpClient.sock.receive(p);
                window.add_Data(p);
            }
            for(int i=0;i<ftpClient.windowSize;i++){

                ftpClient.sock.send(ftpClient.Fill_Ack(ftpClient.UnPack_Data(window.Data_Array[i])));
                window.remove(i);
            }






        }else if (MODE == 2){
            window.Fill_Data();

            //if uploading to the server

            //fill data buffer with file
            //fill data array
            window.Fill_Data_Array();
            //send
            for (int i=0;i<ftpClient.windowSize;i++) {
            ftpClient.sock.send(window.Data_Array[i]);
            window.add_Timer(i);//set timers


            }

            //wait for ACK packet then check timers
            while (!(window.isFull(window.Ack_Array))) {//TODO change to when ACK array != full
                ByteBuffer buffer = ByteBuffer.allocate(4);
                DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
                ftpClient.sock.receive(p);
                window.add_ACK(p);
                window.remove(window.getData(ftpClient.getBlockNumber(p)));
                //check all timers
                int index = window.checkTimers();
                if (index != -1){
                    ftpClient.sock.send(window.Data_Array[index]);
                }
            }
            window.clearAcks();



        }









    }
}
