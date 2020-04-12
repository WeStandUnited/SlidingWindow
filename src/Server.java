import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class Server {



    DataOutputStream out = null;
    DataInputStream in = null;
    DatagramSocket serverSocket = null;
    boolean V6 = false;

    InetAddress address = null;
    static int PORT = 0;
    int encrpytNum = 0;


    public Server(int port,boolean V6) throws IOException {

        PORT = port;
        serverSocket = new DatagramSocket(port);



    }
    public byte [] hash(byte [] input){
        // Define XOR key
        // Any character value will work

        // Define String to store encrypted/decrypted String
        byte[] output = new byte[input.length];

        // calculate length of input string
        int len = input.length;

        // perform XOR operation of key
        // with every caracter in string
        for (int i = 0; i < len; i++) {
            output[i] = (byte)(input[i] ^ encrpytNum);
        }
        return output;
    }
    public void PacketUtilSendFileLength(File file) throws IOException {
        DatagramPacket packet = null;
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(file.length());
        buffer.flip();




            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length, address, PORT);




        serverSocket.send(packet);

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
        int auth = s.Auth();
        PacketService ps = new PacketService(s.serverSocket,s.V6,false,s.address,PORT,auth);
        if (s.V6){
            //print V6

            System.out.println("Host:"+ps.getHostV6().getHostAddress());

        }else {
            System.out.println("Host:"+ps.getHostV4().getHostAddress());

            //print V4
        }

        System.out.println("Done!");
        ByteBuffer buffer = ByteBuffer.allocate(269);

        DatagramPacket p = new DatagramPacket(buffer.array(),buffer.array().length);
        ps.Unpack_Request(ps.PacketUtilRecieve(p));
        TimeUnit.SECONDS.sleep(1);
        ByteBuffer buffer1 = ByteBuffer.allocate(8);
        buffer1.putLong(ps.getFile().length());
        buffer1.flip();



        //ps.Handler(ps.PacketUtilRecieve(p));
        //ps.MODE =  1 // READ
        //ps.MODE = 2 // WRITE
        //Mode 1 : I AM READING FROM HOST
        //Mode 2 : I AM BEING READ FROM
        System.out.println("Mode:"+ps.getmode());

        if (ps.getmode() == 2){
            System.out.println("[SET-MODE]: Downloading from Client");
            System.out.println("[File Length]"+ps.getFileLength());
        }else if (ps.getmode() == 1){
            System.out.println("[SET-MODE]: Downloading to Client");
            DatagramSocket ssock = new DatagramSocket(PORT+1);
            if (s.V6){
                PacketService pp = new PacketService(ssock,false,false, ps.getHostV6(),PORT+1,auth);
                pp.setFile(ps.getFile());
                pp.setFile_length(ps.getFileLength());
                s.serverSocket.send(pp.PacketUtilSendFileLength());
            }else {
                PacketService pp = new PacketService(ssock,false,false, ps.getHostV4(),PORT+1,auth);
                pp.setFile(ps.getFile());
                pp.setFile_length(ps.getFileLength());
                s.serverSocket.send(pp.PacketUtilSendFileLength());
            }

            ssock.close();
        }
        ps.setWindowSize((short)10);


        SlidingWindow window = new SlidingWindow(ps.getWindowSize(),ps.getmode(),ps);//Size must be from Client


       while (window.null_counter != ps.file.length()){


           if (ps.getmode()==1){
               //INTIAL SEND OF DATA
               for (int i=0;i<ps.getWindowSize();i++){
                   s.serverSocket.send(window.Data_Window[i]);
                   window.add_Timer(i);
               }
               while(!(window.isFull(window.Ack_Window))){
                   ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                   DatagramPacket ack = new DatagramPacket(byteBuffer.array(),byteBuffer.array().length);
                   ps.PacketUtilRecieve(ack);
                   window.add_ACK(ack);
                   //iterate through timer's if any of them equall some number eg 3 seconds RESEND
                   for (int i =0;i<window.Data_Timer.length;i++){
                       // if window.Data_Timer[i] >= 3 seconds resend
                       if (System.nanoTime() - window.Data_Timer[i]>= 3000561965L) {
                           s.serverSocket.send(window.Data_Window[i]);
                       }


                   }


               }
               // Unpack Ack's

               for (DatagramPacket packet :window.Ack_Window){

                   window.remove((int)window.getData(ps.UnPack_Ack(packet)));
                   // remove DataArray's By block number

               }
               window.Fill_Data_Array();






           }else if (ps.getmode()==2){

               //recieving data
               while(!(window.isFull(window.Data_Window))) {
                   ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                   DatagramPacket data = new DatagramPacket(byteBuffer.array(),byteBuffer.array().length);
                   s.serverSocket.receive(data);
                   window.add_Data(data);
               }
               // after our data window is full we send out acks
               for (int i=0;i<window.Data_Window.length;i++){

                   s.serverSocket.send(ps.Fill_Ack((short)ps.UnPack_Data(window.Data_Window[i])));// unpacks data writes to file returns what block number it wrote and sends ack back with block number
                    window.remove(i);

               }



           }



       }





    }
}
