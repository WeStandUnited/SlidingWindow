import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;

public class Client {


    DatagramSocket sock =null;
    DataInputStream in = null;
    DataOutputStream out = null;


    static int PORT;
    static Inet4Address HOSTv4;
    static Inet6Address HOSTv6;
    private int xor_key;

    public Client(int port,String host) throws UnknownHostException, SocketException {

        PORT = port;
        HOSTv4 = (Inet4Address) Inet4Address.getByName(host);
        sock = new DatagramSocket();
    }



    public int Auth() throws IOException {
        Random r = new Random();

        int p = r.nextInt(600);//Public Modulo
        int g= r.nextInt(600);//Public base
        int a = r.nextInt(600);//Secret number
        int A = (int) Math.pow(g,a) % p;

        ByteBuffer buff = ByteBuffer.allocate(12);// Stuffing are byte buffer
        buff.putInt(p);
        System.out.println("Modulo:"+p);
        buff.putInt(g);
        System.out.println("Base:"+g);
        buff.putInt(A);
        System.out.println("Computed:"+A);
        buff.flip();


        DatagramPacket SYN  = new DatagramPacket(buff.array(),buff.array().length,HOSTv4,PORT);
        sock.send(SYN);







        return 0;
    }



    public void Send(File f){




    }


    public void Receive(){







    }


    public static void main(String[] args) throws IOException {
        Client c = new Client(2770,"localhost");

        c.Auth();
    }
}
