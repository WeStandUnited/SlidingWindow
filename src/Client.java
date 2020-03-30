import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;
import java.util.Scanner;

public class Client {


    DatagramSocket sock =null;
    DataInputStream in = null;

    SlidingWindow window;

    static int WINDOW_SIZE;

    boolean V6;
    static int PORT;
    static Inet4Address HOSTv4 = null;
    static Inet6Address HOSTv6 = null;

    public Client(int port,String host,int size,boolean uploading,boolean V6) throws IOException {

        PORT = port;
        V6 = this.V6;

        if (V6){
            HOSTv6 = (Inet6Address) Inet6Address.getByName(host);
        }else {
            HOSTv4 = (Inet4Address) Inet6Address.getByName(host);

        }
        sock = new DatagramSocket();
    }




    public int Auth() throws IOException {
        Random r = new Random();

        int mod_num = NumberGen.getPrime(600);//Public Modulo
        int base= r.nextInt(600);//Public base
        int secret_num = r.nextInt(600);//Secret number
        int A = (int) Math.pow(base,secret_num) % mod_num;

        ByteBuffer opcodeBuff = ByteBuffer.allocate(1);
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


        DatagramPacket SYN  = new DatagramPacket(buff.array(),buff.array().length,HOSTv4,PORT);
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


    public static void main(String[] args) throws IOException {



        Client c = new Client(2770,"localhost",100,true,true);
        System.out.println("[Authing]");
        PacketService ps = new PacketService(c.sock,c.V6,false,HOSTv4,PORT,c.Auth());
        ps.file = new File("file.txt");
        System.out.println("Done!");
        Scanner scan = new Scanner(System.in);
        System.out.println("Uploading or Downloading?:");
        String updown= scan.nextLine();

        if (updown.equalsIgnoreCase("uploading")){

            ps.PacketUtil_W_Request();
            ps.MODE = 2;

        }else if(updown.equalsIgnoreCase("downloading")){

            ps.PacketUtil_R_Request();
            ps.MODE = 1;

        }

        ps.windowSize = 10;// we change this to input later


        SlidingWindow window = new SlidingWindow(10,ps.MODE,ps);//Size must be from Client




    }
}
