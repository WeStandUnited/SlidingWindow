import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;



public class PacketService {
 /*
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

         -Send and Receive Packets


         NOTES :

            In the Error Packet and The Read/Write Request I have eliminated the byte at the end to pad the string.

            I have limited all Strings sent to 255 bytes, due to OS's Windows and Linux max filename length is 255 bytes (*This does not account for Path size)

            Error messsages I've changed to 255 bytes which is more than enough.

        */


    File file;
    long file_length = 0;
    DatagramSocket datagramSocket;
    boolean V6_Mode;
    boolean PacketLossMode;
    Inet4Address Hostv4;
    Inet6Address Hostv6;
    int PORT;
    int XOR;
    short MODE;
    //Mode 1 : I AM READING FROM HOST
    //Mode 2 : I AM BEING READ FROM

    short windowSize = 0;




    public PacketService(DatagramSocket sock,boolean V6, boolean PacketLossMode, InetAddress Host, int port, int XOR) {
        /*
        V6 [TYPE:boolean]: Are the packets V6 or not?
        PackelossMode [TYPE boolean]: PacketLossMode if true enable's 1% packet loss
        Host [TYPE InetAddress]: Host will be casted to V4 or V6 address , based on V6
        port [TYPE port]: port number
        XOR  [TYPE int ]: XOR is the key we get from Auth to tell us by what factor we should XOR bytes
        windowSize [TYPE int]: Size of sliding window
        */


        V6_Mode = V6;
        datagramSocket = sock;
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

    public void setWindowSize(short w){
        w = windowSize;
    }

    public void PacketUtilSendFileLength() throws IOException {
        DatagramPacket packet = null;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(file_length);
        buffer.flip();



        if (V6_Mode) {
            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv4, PORT);


        datagramSocket.send(packet);

    }

    public void PacketUtilRecieveFileLength() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        byte b [] = new byte[8];
        DatagramPacket packet = new DatagramPacket(b,b.length);
        datagramSocket.receive(packet);
        buffer.put(packet.getData());
        buffer.flip();
        file_length=buffer.getLong();



        if (V6_Mode) {
            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, Hostv4, PORT);


        datagramSocket.send(packet);

    }
    public void PacketUtilSend(DatagramPacket p){

        try {
            datagramSocket.send(p);

        } catch (IOException e) {
            PacketUtilSendError();
            e.printStackTrace();

        }


    }
    public DatagramPacket PacketUtilRecieve(DatagramPacket p){

        try {
            datagramSocket.receive(p);
            return p;
        } catch (IOException e) {
            PacketUtilSendError();
            e.printStackTrace();
        }
        return null;
    }

    public void PacketUtilSendError(){
        Fill_Error((short) 1,"IO Excpetion");

    }


    public void PacketUtil_W_Request(){
        try {
            datagramSocket.send(Fill_Request((short) 2,file.getName(),(short)2,windowSize));
        } catch (IOException e) {
            PacketUtilSendError();
            e.printStackTrace();
        }
    }

    public void PacketUtil_R_Request(){
        try {
            datagramSocket.send(Fill_Request((short) 1,file.getName(),(short)1,windowSize));
        } catch (IOException e) {
            PacketUtilSendError();
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










    // Fill RWRQ Packet

    public DatagramPacket Fill_Request(short opcode,String Filename, short mode,int size) {
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2+2+2+8+255);//Opcode + Window Length + Name

        buff.putShort((short) opcode);//2bytes

        buff.putShort((short) mode);//2 bytes

        buff.putShort((short) size);//2 bytes

        buff.putLong(file_length);//8 bytes

        buff.put(Filename.getBytes());//255 max bytes

        buff.flip();

        if (V6_Mode) {
            packet = new DatagramPacket(hash(buff.array()), buff.array().length, Hostv6, PORT);

        } else packet = new DatagramPacket(hash(buff.array()), buff.array().length, Hostv4, PORT);


        return packet;
    }

    // Fill RWRQ Packet


    public void Unpack_Request(DatagramPacket p) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2+2+2+8+255);
        buffer.put(p.getData());
        buffer.flip();

        //System.out.println("Opcode:"+buffer.getShort());
        short opcode = buffer.getShort();
        //System.out.println("Mode:"+buffer.getShort());
        MODE = buffer.getShort();
        //System.out.println("size:"+buffer.getShort());
        windowSize = buffer.getShort();
        //System.out.println("Length:"+buffer.getInt());
        file_length = buffer.getLong();
        byte b [] = new byte[255];
        buffer.get(b);
        //System.out.println("Filename"+new String(b).trim());
        String name = new String(b).trim();
       //System.out.println(name);
        file = new File(name);
        if (opcode == 1) {
            //Mode 1 : I AM READING FROM HOST
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        } else if (opcode == 2) {
            //Mode 2 : I AM BEING READ FROM
            file_length = file.length();

        }




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
        buffer.putShort((short) 4);//opcode
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
    public short UnPack_Ack(DatagramPacket p) {

        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put(hash(p.getData()));

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


    //Getters Area
    public short getmode(){
        return MODE;
    }
    public void setmode(short mode){
        MODE = mode;
    }
    public long getFileLength(){
        return file_length;
    }

    public short getWindowSize() {
        return windowSize;
    }
    public void setFile_length(long l){
        file_length=l;
    }
    public File getFile(){
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
}

