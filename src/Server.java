import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Server {



    DataOutputStream out = null;
    DataInputStream in = null;
    DatagramSocket serverSocket = null;
    boolean V6;

    InetAddress address;
    static int PORT;
    int encrpytNum;


    public Server(int port,boolean V6) throws IOException {

        PORT = port;
        serverSocket = new DatagramSocket(port);



    }


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
        DatagramPacket opcode_packet = new DatagramPacket(opcode_buff.array(),opcode_buff.array().length);


        serverSocket.receive(SYN);
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
        serverSocket.send(ACK);

        int s = (int) Math.pow(A,secret_Num) % mod_num;


        System.out.println("Key:"+s);

        return s;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server s =new Server(2770,false);
        System.out.println("Authing!");
        PacketService ps = new PacketService(s.serverSocket,s.V6,false,s.address,PORT,s.Auth());
        System.out.println("Done!");
        ByteBuffer buffer = ByteBuffer.allocate(269);

        DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
        ps.Unpack_Request(ps.PacketUtilRecieve(p));
        //ps.Handler(ps.PacketUtilRecieve(p));
        //ps.MODE =  1 // READ
        //ps.MODE = 2 // WRITE
        //Mode 1 : I AM READING FROM HOST
        //Mode 2 : I AM BEING READ FROM
        System.out.println("Mode:"+ps.MODE);

        if (ps.MODE == 2){
            System.out.println("[SET-MODE]: Downloading from Client");
            ps.PacketUtilSendFileLength();
        }else if (ps.MODE == 1){
            System.out.println("[SET-MODE]: Downloading to Client");

        }


        SlidingWindow window = new SlidingWindow(ps.windowSize,ps.MODE,ps);//Size must be from Client


        //while (window.null_counter != ps.file.length()){

            // TODO send data and recieve here




       //}





    }
}
