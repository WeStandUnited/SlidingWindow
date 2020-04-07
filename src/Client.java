import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;
import java.util.Scanner;

public class Client {


    DatagramSocket sock =null;
    DataInputStream in = null;
    long file_length;
    SlidingWindow window;

    static int WINDOW_SIZE;

    boolean V6;
    static int PORT;
    static Inet4Address HOSTv4 = null;
    static Inet6Address HOSTv6 = null;

    public Client(int port,String host,boolean uploading,boolean V6) throws IOException {

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
        ByteBuffer buffer = ByteBuffer.allocate(8);
        DatagramPacket FileLength_ACK = new DatagramPacket(buffer.array(),buffer.array().length);
        Client c = new Client(2770,"localhost",true,true);
        System.out.println("[Authing]");
        PacketService ps = new PacketService(c.sock,c.V6,false,HOSTv4,PORT,c.Auth());
        if (c.V6){
            //print V6

            System.out.println("Host:"+ps.getHostV6().getHostAddress());

        }else {
            System.out.println("Host:"+ps.getHostV4().getHostAddress());

            //print V4
        }
        System.out.println("PORT:"+ps.getPORT());
        System.out.println("Done!");
        Scanner scan = new Scanner(System.in);
        System.out.println("Downloading from Server or Uploading to Server?:");
        String updown= scan.nextLine();
        String file_name = "file.txt";//NOTE THIS IS TEMP
        ps.setWindowSize((short)10);
        //ps.windowSize = (short)10;// we change this to input later

        //Mode 1 : I AM READING FROM HOST
        //Mode 2 : I AM BEING READ FROM
        if (updown.equalsIgnoreCase("downloading")){
            //String file_name = scan.nextLine();
            ps.setFile(new File(file_name));
            ps.setmode((short) 2);
            ps.PacketUtil_R_Request();




        }else if(updown.equalsIgnoreCase("uploading")){
            //String file_name = scan.nextLine();
            File f =new File(file_name);
            ps.setFile(f);
            ps.setFile_length(f.length());
            ps.PacketUtil_W_Request();
            ps.setmode((short) 1);

        }

        ps.PacketUtilRecieve(FileLength_ACK);


        System.out.println("Mode:"+ps.getmode());

        SlidingWindow window = new SlidingWindow(ps.getWindowSize(),ps.getmode(),ps);//Size must be from Client


       // while (window.null_counter != ps.file.length()){

            // TODO send data and recieve here




        //}


    }
}
