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
    boolean packetLoss = false;

    public FTPClient(InetAddress a, boolean V6, int port) throws SocketException {

        PORT = port;
        V6_Mode = V6;
        sock = new DatagramSocket(PORT);
        if (V6_Mode) {
            address = (Inet6Address) a;
        } else {
            address = (Inet4Address) a;
        }

    }

    public byte[] hash(byte[] input) {
        // Define XOR key
        // Any character value will work

        // Define String to store encrypted/decrypted String
        byte[] output = new byte[input.length];

        // calculate length of input string
        int len = input.length;

        // perform XOR operation of key
        // with every caracter in string
        for (int i = 0; i < len; i++) {
            output[i] = (byte) (input[i] ^ XOR);
        }
        return output;
    }

    public int Auth() throws IOException {
        Random r = new Random();

        int mod_num = NumberGen.getPrime(600);//Public Modulo
        int base = r.nextInt(600);//Public base
        int secret_num = r.nextInt(600);//Secret number
        int A = (int) Math.pow(base, secret_num) % mod_num;

        ByteBuffer buff = ByteBuffer.allocate(12);// Stuffing are byte buffer

        ByteBuffer ACK_Buff = ByteBuffer.allocate(4);
        DatagramPacket Ack = new DatagramPacket(ACK_Buff.array(), ACK_Buff.array().length);


        buff.putInt(mod_num);
        System.out.println("Modulo:" + mod_num);
        buff.putInt(base);
        System.out.println("Base:" + base);
        buff.putInt(A);
        System.out.println("Computed:" + A);
        buff.flip();


        DatagramPacket SYN = new DatagramPacket(buff.array(), buff.array().length, address, PORT);
        sock.send(SYN);

        sock.receive(Ack);
        ACK_Buff.put(Ack.getData());
        ACK_Buff.flip();

        int B = ACK_Buff.getInt();
        System.out.println("Bob sent:" + B);

        int s = (int) Math.pow(B, secret_num) % mod_num;
        System.out.println("Key:" + s);


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
        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length);
        sock.receive(packet);
        buffer.put(hash(packet.getData()));
        buffer.flip();
        file_length = buffer.getLong();
        System.out.println("File Length gotten");

    }

    public DatagramPacket Fill_Request(short opcode, String Filename, short mode, int size) throws IOException {
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2 + 2 + 2 + 8 + 255);//Opcode + Window Length + Name

        buff.putShort((short) opcode);//2bytes

        System.out.println("opcode:" + opcode);

        buff.putShort((short) mode);//2 bytes

        System.out.println("mode :" + mode);

        buff.putShort((short) size);//2 bytes

        System.out.println("window size :" + size);

        buff.putLong(file_length);//8 bytes
        System.out.println("file_length:" + file_length);


        buff.put(Filename.getBytes());//255 max bytes

        buff.flip();

        packet = new DatagramPacket(hash(buff.array()), buff.array().length, address, PORT);


        return packet;
    }

    public void PacketUtil_W_Request() {
        try {
            sock.send(Fill_Request((short) 2, file.getName(), (short) 2, windowSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void PacketUtil_R_Request() {
        try {
            sock.send(Fill_Request((short) 1, file.getName(), (short) 1, windowSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getBlockNumber(DatagramPacket p) {

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this


        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();

        //buff.getShort();

        return buff.getLong();
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
        // System.out.println("Packet Size:"+p.getLength());
        if (p == null) {
            System.out.println("File Transfer Error!");
            System.exit(-1);
            return -1;
        } else {

            buffer.put(hash(p.getData()));
            buffer.flip();

            long BlockNum = buffer.getLong();

            if (BlockNum == -1) {
                System.out.println("File Transfer Error!");
                System.exit(1);
            }


            //System.out.println("BlockNum:" + BlockNum);

            byte[] bytes = new byte[504];


            buffer.get(bytes, 0, bytes.length);

            ra.seek(BlockNum);

            // String str = new String(bytes);
            //System.out.println("DATA:" + str);


            ra.write(bytes);

            ra.close();

            // Write to file


            return BlockNum;
        }
    }

    public static boolean PacketLoss() {
        Random rand = new Random();
        int testy = 0;

        testy = rand.nextInt(10);
        // System.out.println(testy);

        return testy == 1;
    }


    public static void main(String[] args) throws IOException {
        Scanner kb = new Scanner(System.in);

        boolean V_Mode = Boolean.getBoolean(args[0]);
        String file_name= args[1];
        int DorUMode=Integer.getInteger(args[2]);
        String address_local = args[3];
        int port_local = Integer.getInteger(args[4]);



        FTPClient ftpClient = new FTPClient(InetAddress.getByName(address_local), V_Mode, port_local);
        System.out.println("[Waiting to Auth]");
        XOR = ftpClient.Auth();
        System.out.println("[Authed with]" + "\n" + "Host:" + address.getHostName() + "\n" + "Port:" + port_local);
        System.out.println("Downloading from Server [2] or Uploading to Server [1]");
        MODE = kb.nextInt();
        ftpClient.windowSize = 10;
        ftpClient.file = new File(file_name);
        boolean result = ftpClient.file.createNewFile();

        if (result) {
            System.out.println("File created:" + ftpClient.file.getName());
        } else {
            System.out.println("File Exists");
        }


        if (MODE == 2) {
            ftpClient.PacketUtil_W_Request();
            ftpClient.PacketUtilRecieveFileLength();
            ByteBuffer data_buff = ByteBuffer.allocate(8);
//            DatagramPacket data = new DatagramPacket(data_buff.array(), data_buff.array().length);
//            ftpClient.sock.receive(data);
//            data = null;
            System.out.println("[New File length]:" + ftpClient.file_length);
        } else if (MODE == 1) {
            ftpClient.file_length = ftpClient.file.length();
            ftpClient.PacketUtil_R_Request();
//            ByteBuffer data_buff = ByteBuffer.allocate(8);
//            DatagramPacket data = new DatagramPacket(data_buff.array(), data_buff.array().length);
//            ftpClient.sock.receive(data);
//            data = null;
            //ftpClient.PacketUtilSendFileLength();
            System.out.println("[New File length]:" + ftpClient.file_length);
        }


        //make window structure
        SlideWindow window = new SlideWindow(ftpClient.windowSize, (int) ftpClient.file_length, XOR);
        window.setFile(ftpClient.file);
        window.host = address;
        window.port = PORT;
        //ftpClient.packetLoss = true;
        //if Downloading from the Server
        if (MODE == 2){

            int end_control =0;
            ftpClient.sock.setSoTimeout(2500);
            while (end_control != ftpClient.file_length/512) {
               // ftpClient.sock.setSoTimeout(3000);

                //while ((window.isFull(window.Data_Array))){//TODO change to when Data array != full
                for (int i = 0; i < ftpClient.windowSize; i++) {
                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    DatagramPacket p = new DatagramPacket(buffer.array(), buffer.array().length);
                    try {
                        ftpClient.sock.receive(p);
                        System.out.println("Recieved Packet!");
                        long blockNum = ftpClient.getBlockNumber(p);
                        System.out.println("BlockNum:" + blockNum);
                        if (blockNum == -1) {
                            System.out.println("File Transfer Complete!");
                            System.exit(1);
                        }
                        long index = ftpClient.UnPack_Data(p);
                        if (index == -1) {
                            System.out.println("File Transfer Complete!");
                            System.exit(1);
                        }
                        window.add_ACK(ftpClient.Fill_Ack(index));
                    }catch (SocketTimeoutException e) {
                        System.out.println("Timeout!");
                        break;
                    }
                }

                    for (int i = 0; i < window.ACK_Bookkeeping; i++) {
                        ftpClient.sock.send(window.Ack_Array[i]);
                        System.out.println("Sending ACKS"+window.getACKBlockNumber(window.Ack_Array[i]));
                        end_control++;
                        window.remove(i);
                }
                window.clearAcks();
            }

            }

        else if (MODE == 1) {
            window.Fill_Data();
            int end_control = 0;
            int timout_Counter=0;
            //if uploading to the server

            //fill data buffer with file
            //fill data array

            while (end_control != window.DataBuffer.size()) {
                window.Fill_Data_Array();
                //send
                ftpClient.sock.setSoTimeout(3000);



                for (int i = 0; i < ftpClient.windowSize; i++) {
                    try {

                        if (ftpClient.packetLoss){
                            if (PacketLoss()) {
                                DatagramPacket faulty_packet = window.Data_Array[i];
                                faulty_packet.setPort(9999);
                                ftpClient.sock.send(faulty_packet);// TODO not sending full file because the array doesnt get full bc end of file!
                                System.out.println("Sending DuD:" + ftpClient.getBlockNumber(window.Data_Array[i]));
                            }else{
                                ftpClient.sock.send(window.Data_Array[i]);// TODO not sending full file because the array doesnt get full bc end of file!
                                System.out.println("Sending BlockNum:" + ftpClient.getBlockNumber(window.Data_Array[i]));
                            }
                        }else{
                            ftpClient.sock.send(window.Data_Array[i]);// TODO not sending full file because the array doesnt get full bc end of file!
                            System.out.println("Sending BlockNum:" + ftpClient.getBlockNumber(window.Data_Array[i]));
                        }

                    }catch (NullPointerException c){// Send a packet with a negative block number
                        ByteBuffer bb = ByteBuffer.allocate(512);
                        bb.putLong(-1);
                        bb.flip();
                        DatagramPacket packet;
                        packet = new DatagramPacket(ftpClient.hash(bb.array()), bb.array().length,address, PORT);
                        ftpClient.sock.send(packet);
                        break;
                    }
                }


                //wait for ACK packet then check timers
                for (int i = 0; i < window.size; i++) {

                    ByteBuffer buff = ByteBuffer.allocate(10);
                    DatagramPacket pa = new DatagramPacket(buff.array(), buff.array().length);
                    try {
                        System.out.println("Awaiting ACKS");
                        ftpClient.sock.receive(pa);
                        timout_Counter=0;
                        System.out.println("Ack Recieved!");
                        long acknum = window.getACKBlockNumber(pa);
                        System.out.println("ACK BLock Recieved:" + acknum);
                        window.add_ACK(pa);
                        window.removeByBlockNum(acknum);
                        end_control++;
                        System.out.println("[null counter]:" + window.null_counter);
                    }catch (SocketTimeoutException e){
                        timout_Counter++;
                        if(timout_Counter == 5){
                            System.out.println("File Transfer Complete");
                            System.exit(1);
                        }
                        break;
                    }


                    //check all timers
                }

                for (int i = 0; i <window.size ; i++) {
                    if(window.Data_Array[i] != null){
                        ftpClient.sock.send(window.Data_Array[i]);// TODO not sending full file because the array doesnt get full bc end of file!
                        System.out.println("ReSending BlockNum:" + ftpClient.getBlockNumber(window.Data_Array[i]));
                    }

                }

                window.clearAcks();

            }
        }
        System.out.println("File Transfer Complete!");
    }


}
