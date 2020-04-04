import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;


public class SlidingWindow {
    int null_counter = 0;
    int size;
    ArrayList<DatagramPacket> Data_Buffer;
    DatagramPacket [] Data_Window;
    DatagramPacket [] Ack_Window;
    PacketService ps;

    public SlidingWindow (int size,int m,PacketService packetService) throws IOException {
        size = this.size;

        Data_Window =  new DatagramPacket[size];

        Ack_Window = new DatagramPacket[size];

        ps = packetService;

        int file_length = (int) ps.file.length() / 512;

        Data_Buffer = new ArrayList<DatagramPacket>(file_length);

        if (ps.MODE == 2) {
            for (int i = 0; i < file_length; i++) {

                ps.Fill_Data((short) i);

            }
        }

    }



    public void add_Data(DatagramPacket p){

        for (int i = size-1; i>-1 ; i--) {

            if (Data_Window[i] == null){

                Data_Window[i] = p;
            }

        }


    }

    public void add_ACK(DatagramPacket p){

        for (int i = size-1; i>-1 ; i--) {

            if (Ack_Window[i] == null){

                Ack_Window[i] = p;
            }

        }


    }

    public boolean contains(short blocknum) throws IOException {
        for (int i=0;i<size;i++){
            DatagramPacket p = Data_Window[i];
            if (blocknum == ps.getBlockNumber(p)){
                return true;
            }

        }
        return false;
    }

    public int indexof(short blocknum){
        for (int i=0;i<size;i++){
            DatagramPacket p = Data_Window[i];
            if (blocknum == ps.getBlockNumber(p)){
                return i;
            }
        }
        return -1;
    }


    public void remove(int index){
        Data_Window[index] = null;
        Ack_Window[index] = null;
        null_counter++;
    }


    public boolean isFull(){

        return Data_Window[size] != null;
    }


    public void shift(int offset){


        DatagramPacket [] copy_array = new DatagramPacket[size];
        DatagramPacket [] copy_array2 = new DatagramPacket[size];

        System.arraycopy(Data_Window,offset,copy_array,0,size-offset);
        System.arraycopy(Ack_Window,offset,copy_array2,0,size-offset);

        Data_Window = copy_array;
        Ack_Window = copy_array2;

    }




}
