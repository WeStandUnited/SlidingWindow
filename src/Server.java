import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Server {



    DataOutputStream out = null;
    DataInputStream in = null;
    DatagramSocket serverSocket = null;


    static int PORT;
    private int encrpytNum;



    public Server(int port) throws IOException {

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

        DatagramPacket SYN = new DatagramPacket(buff.array(),buff.array().length);

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

        DatagramPacket ACK = new DatagramPacket(Ack_Buff.array(),Ack_Buff.array().length,SYN.getAddress(),SYN.getPort());
        serverSocket.send(ACK);

        int s = (int) Math.pow(A,secret_Num) % mod_num;


        System.out.println("Key:"+s);




        return s;
    }

    public static void main(String[] args) throws IOException {
        Server s =new Server(2770);

            s.Auth();


    }
}
