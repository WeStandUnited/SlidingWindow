import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;
import java.util.Scanner;

public class Client {


    DatagramSocket sock =null;
    DataInputStream in = null;
    DataOutputStream out = null;

    SlidingWindow window;

    static int WINDOW_SIZE;
    static int STATE;
    // STATE TABLE:
    //0 = READ
    //1 = WRITE
    //2 = TERMINATE


    static int PORT;
    static Inet4Address HOSTv4;
    static Inet6Address HOSTv6;
    private int xor_key;

    public Client(int port,String host,int size,boolean uploading,String file_name,boolean V6orV4) throws IOException {

        PORT = port;

        HOSTv4 = (Inet4Address) Inet4Address.getByName(host);
        HOSTv6 = (Inet6Address) Inet6Address.getByName(host);
        sock = new DatagramSocket();

        WINDOW_SIZE = size;

        window = new SlidingWindow(WINDOW_SIZE,file_name,uploading);










        //window = new SlidingWindow(WINDOW_SIZE);



    }



    public int Auth() throws IOException {
        Random r = new Random();

        int mod_num = NumberGen.getPrime(600);//Public Modulo
        int base= r.nextInt(600);//Public base
        int secret_num = r.nextInt(600);//Secret number
        int A = (int) Math.pow(base,secret_num) % mod_num;

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


    private byte [] encrpyt(byte [] b){
        //xor_key is the var that AUTH gets

        return b;//TODO wrtie XOR encrpytor
    }


    public void Send(File f){




    }


    public void Receive(){







    }


    public static void main(String[] args) throws IOException {


                     //Client(int port,String host,int size,boolean uploading,String file_name,boolean V6orV4)
        Client c = new Client(Integer.parseInt(args[0]),args[1],Integer.parseInt(args[2]),Boolean.parseBoolean(args[3]),args[4],Boolean.parseBoolean(args[5]));

        //eg command: java Client.class 2770 localhost 100 true me.txt true true

        //c.xor_key = c.Auth();


    }
}
