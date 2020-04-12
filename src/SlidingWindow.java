import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;


public class SlidingWindow {
    int null_counter = 0;
    int size;
    ArrayList<DatagramPacket> Data_Buffer;
    DatagramPacket [] Data_Window;
    long [] Data_Timer;
    DatagramPacket [] Ack_Window;
    long [] ACK_Timer;
    int file_length;
    PacketService ps;

    public SlidingWindow (int size,int m,PacketService packetService) throws IOException {
        size = this.size;

        Data_Window =  new DatagramPacket[size];
        Data_Timer = new long[size];
        Ack_Window = new DatagramPacket[size];
        ACK_Timer = new long[size];

        ps = packetService;

         file_length = (int) ps.getFileLength() / 512;

        Data_Buffer = new ArrayList<DatagramPacket>(file_length);

        if (ps.MODE == 1) {
            for (int i = 0; i < file_length; i++) {

                Data_Buffer.add(ps.Fill_Data((short) i));

            }
            for (int j = 0; j < this.size; j++){
                add_Data(Data_Buffer.get(j));
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
    public void Fill_Data_Array() throws IOException {

        for (int j = 0; j < this.size; j++){
            add_Data(Data_Buffer.get(j+null_counter));
        }
    }


    public void add_Timer(int index){

        Data_Timer[index] = System.nanoTime();


    }

    public boolean add_ACK(DatagramPacket p){
        //True in meaning successful
        for (int i = size-1; i>-1 ; i--) {

            if (Ack_Window[i] == null){

                Ack_Window[i] = p;
                return true;
            }

        }

        return false;
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
    public short getData(short blocknum) throws IOException {
        for (int i=0;i<size;i++){
            DatagramPacket p = Data_Window[i];
            if (blocknum == ps.getBlockNumber(p)){
                return blocknum;
            }

        }
        return -1;
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
        Data_Timer[index] = 0;
        null_counter++;
    }


    public boolean isFull(DatagramPacket [] p){

        return p[size] != null;
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
