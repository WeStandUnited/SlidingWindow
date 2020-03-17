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
        int p;//Modulo num
        int g;//base
        int b = r.nextInt(600);
        int A;
        ByteBuffer buff = ByteBuffer.allocate(12);


        DatagramPacket SYN = new DatagramPacket(buff.array(),buff.array().length);

        serverSocket.receive(SYN);
        buff.put(SYN.getData());
        buff.flip();
        p = buff.getInt();
        System.out.println("Modulo:"+p);
        g = buff.getInt();
        System.out.println("Base:"+g);
        A = buff.getInt();
        System.out.println("Computed:"+A);

        int B = (int) Math.pow(g,b) % p;




        return 0;
    }

    public static void main(String[] args) throws IOException {
        Server s =new Server(2770);

            s.Auth();


    }
}
