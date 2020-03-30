import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;




public class SlidingWindow {




    int mode;
    int size;
    ArrayList<DatagramPacket> Data_Buffer;
    DatagramPacket [] send;
    DatagramPacket [] recieve;
    PacketService ps;

    public SlidingWindow (int size,int m,PacketService packetService) throws IOException {
        size = this.size;

        send =  new DatagramPacket[size];

        recieve =   new DatagramPacket[size];

        mode = m;

        ps = packetService;

        int file_length = (int) ps.file.length() / 512;

        Data_Buffer = new ArrayList<DatagramPacket>(file_length);

        if (mode == 1) {
            for (int i = 0; i < file_length; i++) {

                ps.Fill_Data((short) i);
                System.out.println("Block:"+i);

            }
        }

    }

    public void movewindow(){

        for (int i=0;i<size;i++){

            send[i] = null;
            recieve[i] = null;

      }


    }


}
