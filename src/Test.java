import jdk.swing.interop.SwingInterOpUtils;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.RandomAccess;
import java.util.concurrent.TimeUnit;

public class Test {
    public static DatagramPacket Fill_Request(short opcode,String Filename, short mode,int size) throws UnknownHostException {
        System.out.println("PACKING");
        DatagramPacket packet = null;

        ByteBuffer buff = ByteBuffer.allocate(2+2+2+8+255);//Opcode + Window Length + Name

        buff.putShort((short) opcode);//2bytes

        System.out.println("opcode:"+opcode);

        buff.putShort((short) mode);//2 bytes

        System.out.println("mode :"+mode);

        buff.putShort((short) size);//2 bytes

        System.out.println("window size :"+size);

        buff.putLong(1000);//8 bytes
        System.out.println("file_length:"+1000);

        buff.put(Filename.getBytes());//255 max bytes

        buff.flip();

        packet = new DatagramPacket(buff.array(), buff.array().length, Inet4Address.getByName("127.0.0.1"),2770);



        return packet;
    }
    public static void Unpack_Request(DatagramPacket p) throws IOException {
        System.out.println("UNPACKING");
        ByteBuffer buffer = ByteBuffer.allocate(2+2+2+8+255);
        buffer.put(p.getData());
        buffer.flip();

       // short opcode = buffer.getShort();
        System.out.println("Opcode:"+buffer.getShort());

        //short mode = buffer.getShort();
        System.out.println("Mode:"+buffer.getShort());

        System.out.println("size:"+buffer.getShort());
        //short windowSize = buffer.getShort();
        //System.out.println("Length:"+buffer.getInt());
        long file_length = buffer.getLong();
        System.out.println("File l:"+file_length);
        byte b [] = new byte[255];
        buffer.get(b);
        //System.out.println("Filename"+new String(b).trim());
        String name = new String(b).trim();
        //System.out.println(name);
        System.out.println(name);




    }

    public static void readFile(File file) throws IOException {


        RandomAccessFile ra = new RandomAccessFile(file, "r");


        for (long i = 0; i < file.length(); i += 510) {
            byte[] data = new byte[510];

            ra.seek(i);

            ra.read(data);



            //System.out.println("AddBlock_Num:" + i);

            String s = new String(data);

            System.out.println("DATA:"+s);
        }
    }

    public static boolean PacketLoss(){
        Random rand = new Random();
        int testy = 0;

        testy=rand.nextInt(1000);
        System.out.println(testy);

        return testy == 100;
    }


    public static void main(String[] args) throws InterruptedException, IOException {

//        long startTime = System.nanoTime();
//        System.out.println(startTime);
//        TimeUnit.SECONDS.sleep(3);
//
//        long estimatedTime = System.nanoTime() - startTime;
//        System.out.println(estimatedTime);


    }
}
