import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;


public class Client {


    DatagramSocket sock =null;
    DataInputStream in = null;
    DataOutputStream out = null;


    static int PORT;
    static Inet4Address HOSTv4;
    static Inet6Address HOSTv6;


    public Client(int port,String host) throws UnknownHostException, SocketException {

        PORT = port;
        HOSTv4 = (Inet4Address) Inet4Address.getByName(host);
        sock = new DatagramSocket();
    }



    public boolean Auth()  {
        try {
            Random r = new Random();
            //Send Server SYN(Syn is just a random number)
            int synnum = r.nextInt(500);
            System.out.println("NumSent:"+synnum);
            ByteBuffer buff = ByteBuffer.allocate(4);// 4 bytes is one int
            buff.putInt(synnum);

            byte[] b = buff.array();
            DatagramPacket send = new DatagramPacket(b, b.length, HOSTv4, PORT);
            buff.flip();
            sock.send(send);
            System.out.println("Packet Sent!");



            return true;

        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }



    public void Send(File f){




    }


    public void Receive(){







    }


    public static void main(String[] args) throws UnknownHostException, SocketException {
        Client c = new Client(2770,"localhost");

        c.Auth();
    }
}
