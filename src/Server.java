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


    public boolean Auth() throws IOException {
        Random r = new Random();

        int ACK = r.nextInt(500);//MAke ACK number

        byte[] ACK_SYN_Staging = new byte[4];

        DatagramPacket SYN = new DatagramPacket(ACK_SYN_Staging,ACK_SYN_Staging.length);

        serverSocket.receive(SYN);
        System.out.println("Packet Recieved!");


        return true;
    }

    public static void main(String[] args) throws IOException {
        Server s =new Server(2770);

            s.Auth();


    }
}
