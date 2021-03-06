import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class FTPServer {
    static boolean windowSuccess = true;
    static int end_control =0;
    DatagramSocket sock;
    static InetAddress address;
    static int PORT;
    static boolean V6_Mode;
    static int XOR;
    boolean packetLoss = false;
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
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        } else if (opcode == 2) {
            //Mode 2 : I AM BEING READ FROM
            MODE = 2;
            file_length = file.length();

        }




    }
    public DatagramPacket Fill_Ack(long BlockNum) {

        DatagramPacket packet = null;

        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.putShort((short) 4);//opcode
        buffer.putLong(BlockNum);
        buffer.flip();


        packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, address, PORT);



        return packet;
    }
    public long UnPack_Data(DatagramPacket p) throws IOException {// Returns Void due to it just taking care of writing to the File
        RandomAccessFile ra = new RandomAccessFile(file, "rw");
        ByteBuffer buffer = ByteBuffer.allocate(512);
//        System.out.println("Packet Size:"+p.getLength());
        if (p == null){
            return -1;

        }else {
            buffer.put(hash(p.getData()));
            buffer.flip();

            long BlockNum = buffer.getLong();

            //System.out.println("BlockNum:" + BlockNum);

            byte[] bytes = new byte[504];


            buffer.get(bytes, 0, bytes.length);

            ra.seek(BlockNum);

            String str = new String(bytes);
            System.out.println("DATA:" + str);
            ra.write(bytes);

            ra.close();

            // Write to file

            return BlockNum;
        }
    }
    public long getBlockNumber(DatagramPacket p){

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this



        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();



        return buff.getLong();
    }
    public static boolean PacketLoss(){
        Random rand = new Random();
        int testy = 0;

        testy=rand.nextInt(70);
        // System.out.println(testy);

        return testy == 1;
    }
    public void ReceiveData(FTPServer ftpServer,SlideWindow window) throws IOException {

        try {
            for (int i = 0; i < ftpServer.windowSize; i++) {
                ByteBuffer bufbfer = ByteBuffer.allocate(512);
                DatagramPacket pa = new DatagramPacket(bufbfer.array(), bufbfer.array().length);
                System.out.println("Awaiting Packet");
                ftpServer.sock.receive(pa);
                System.out.println("Recieved Packet!");
                long blockNum = ftpServer.getBlockNumber(pa);
                System.out.println("BlockNum:" + blockNum);

                if (blockNum == -1) {
                    end_control = -1;
                    break;
                }
                long index = ftpServer.UnPack_Data(pa);// Write to the file and get the block number

                window.add_ACK(ftpServer.Fill_Ack(index));// make an ACK and add to the ACK array

            }
        }catch (SocketTimeoutException e){
            ByteBuffer errorBuff = ByteBuffer.allocate(10);
            errorBuff.putShort((short)2);
            errorBuff.putLong(-2L);
            errorBuff.flip();
            DatagramPacket error = new DatagramPacket(errorBuff.array(),errorBuff.array().length,address,PORT);
            ftpServer.sock.send(error);
            window.clearData();
            ReceiveData(ftpServer,window);
        }
    }
    public void SendACKs(FTPServer ftpServer,SlideWindow window) throws IOException {
        for (int i = 0; i < window.size; i++) {
            if(end_control == -1){
                for (int j = 0; j <window.ACK_Bookkeeping; j++) {
                    ftpServer.sock.send(window.Ack_Array[j]);
                    System.out.println("Sending ACKS" + window.getACKBlockNumber(window.Ack_Array[j]));
                }
                System.out.println("File Transfer Complete!");
                System.exit(1);
            }
            try {
                ftpServer.sock.send(window.Ack_Array[i]);
                System.out.println("Sending ACKS" + window.getACKBlockNumber(window.Ack_Array[i]));
                end_control++;
                window.remove(i);
            }catch (NullPointerException e){
                System.out.println("File Transfer Complete!");
                System.exit(1);
            }
        }
        window.clearAcks();
    }
    public void sendWindow(SlideWindow window, FTPServer ftpServer) throws IOException {
        for (int i = 0; i < ftpServer.windowSize; i++) {
            try {

                if (ftpServer.packetLoss) {
                    if (PacketLoss()) {
                        DatagramPacket faulty_packet = window.Data_Array[i];
                        faulty_packet.setPort(9999);
                        ftpServer.sock.send(faulty_packet);// TODO not sending full file because the array doesnt get full bc end of file!
                        System.out.println("Sending DuD:" + ftpServer.getBlockNumber(window.Data_Array[i])+":"+window.Data_Array[i].getPort());
                        window.Data_Array[i].setPort(PORT);
                    } else {
                        ftpServer.sock.send(window.Data_Array[i]);// TODO not sending full file because the array doesnt get full bc end of file!
                        System.out.println("Sending BlockNum:" + ftpServer.getBlockNumber(window.Data_Array[i])+":"+window.Data_Array[i].getPort());
                    }
                } else {
                    ftpServer.sock.send(window.Data_Array[i]);// TODO not sending full file because the array doesnt get full bc end of file!
                    System.out.println("Sending BlockNum:" + ftpServer.getBlockNumber(window.Data_Array[i]));
                }

            } catch (NullPointerException c) {// Send a packet with a negative block number
                ByteBuffer bb = ByteBuffer.allocate(512);
                bb.putLong(-1);
                bb.flip();
                DatagramPacket packet;
                packet = new DatagramPacket(ftpServer.hash(bb.array()), bb.array().length, address, PORT);
                ftpServer.sock.send(packet);
                break;
            }
        }


    }


    public void recieveWindow(SlideWindow window,FTPServer ftpServer) throws IOException {
        for (int i = 0; i < window.size; i++) {

            ByteBuffer buff = ByteBuffer.allocate(10);
            DatagramPacket pa = new DatagramPacket(buff.array(), buff.array().length);
            System.out.println("Awaiting ACKS");
            ftpServer.sock.receive(pa);
            long acknum = window.getACKBlockNumber(pa);
            if (acknum < 0||acknum>file_length){
                System.out.println("Recieve Error!");
                windowSuccess = false;
                window.clearAcks();
                break;
            }
            System.out.println("ACK BLock Recieved:" + acknum);
            window.add_ACK(pa);
            window.removeByBlockNum(acknum);
            end_control++;
            System.out.println("[null counter]:" + window.null_counter);
            if (acknum == file_length-504){
                System.out.println("File Transfer Complete!");
                System.exit(1);
            }
            windowSuccess = true;

        }

        window.clearAcks();

    }

    public static void main(String[] args) throws IOException {

        FTPServer ftpServer = new FTPServer(false,2770);

        XOR = ftpServer.Auth();
        ByteBuffer buffer = ByteBuffer.allocate(269);
        //ftpServer.packetLoss = //Boolean.getBoolean(args[0]);
        DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
        ftpServer.sock.receive(p);
        ftpServer.Unpack_Request(p);
        System.out.println("Sent:"+ftpServer.file_length);
        SlideWindow window = new SlideWindow(ftpServer.windowSize,(int)ftpServer.file_length,XOR);
        window.setFile(ftpServer.file);
        window.host = address;
        window.port = PORT;

        //if Downloading from the Server


        if (MODE == 1){
            ftpServer.sock.setSoTimeout(1000);

            while (end_control != ftpServer.file_length/512) {


                ftpServer.ReceiveData(ftpServer, window);

                ftpServer.SendACKs(ftpServer, window);
            }






        }else if (MODE == 2) {
            ftpServer.packetLoss = true;
            ftpServer.PacketUtilSendFileLength();
            window.Fill_Data();

            while (end_control != window.DataBuffer.size()) {

                if(windowSuccess){
                    window.Fill_Data_Array();
                }
                ftpServer.sendWindow(window, ftpServer);
                ftpServer.recieveWindow(window, ftpServer);

            }
        }
        }
    }


