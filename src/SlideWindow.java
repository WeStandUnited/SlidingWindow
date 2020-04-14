import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SlideWindow{
    int null_counter = 0;
    DatagramPacket[] Data_Array ;
    DatagramPacket [] Ack_Array;
    long [] Data_Timer;
    long [] ACK_Timer;
    ArrayList<DatagramPacket> DataBuffer;
    File file;
    InetAddress host;
    int port;
    int XOR;
    int size;

    public SlideWindow(int size,int Buffer_Size,int hasher)
    {
        size = this.size;
        DataBuffer=new ArrayList<>(Buffer_Size/512);
        Data_Array=new DatagramPacket[size];
        Ack_Array=new DatagramPacket[size];
        Data_Timer=new long[size];
        XOR = hasher;
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
            output[i] = (byte)(input[i] ^ XOR);
        }
        return output;
    }
    public void setFile(File f){
        file=f;
    }
    public void add_Timer(int index){

        Data_Timer[index] = System.nanoTime();


    }
    public void Fill_Data() throws IOException {

        DataBuffer = new ArrayList<>((int)file.length());

        RandomAccessFile ra = new RandomAccessFile(file, "r");

        byte[] data = new byte[512];

        for (long i=0;i<file.length();i++) {
            ra.seek(i);

            ra.read(data);

            ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 512);

            buffer.putShort((short) i);

            buffer.put(data);

            buffer.flip();

            DatagramPacket packet;

            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length,host, port);

            DataBuffer.add(packet);

        }
        ra.close();

    } // Use when sending file
    public short getBlockNumber(DatagramPacket p){

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this



        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();

        buff.getShort();

        return buff.getShort();
    }
    public short getData(short blocknum) throws IOException {
        for (int i=0;i<size;i++){
            DatagramPacket p = Data_Array[i];
            if (blocknum == getBlockNumber(p)){
                return blocknum;
            }

        }
        return -1;
    }
    public void add_Data(DatagramPacket p){// adds data into data window

        for (int i = size-1; i>-1 ; i--) {

            if (Data_Array[i] == null){

                Data_Array[i] = p;
            }

        }


    }

    public int checkTimers(){
        for (int i =0;i<Data_Timer.length;i++){
            // if window.Data_Timer[i] >= 3 seconds resend
            if (System.nanoTime() - Data_Timer[i]>= 3000561965L) {
                return i;
            }


        }
        return -1;
    }

    public void Fill_Data_Array() throws IOException {//fills the data Window with Packets from the Data Buffer

        for (int j = 0; j < this.size; j++){
            add_Data(DataBuffer.get(j+null_counter));
        }
    }

    public void remove(int index) {
        Data_Array[index] = null;
        Data_Timer[index] = 0;
        null_counter++;
    }
    public void clearAcks(){
        for (int i=0;i<size;i++){
            Ack_Array[i] = null;
        }
    }
    public boolean add_ACK(DatagramPacket p){
        //True in meaning successful
        for (int i = size-1; i>-1 ; i--) {

            if (Ack_Array[i] == null){

                Ack_Array[i] = p;
                return true;
            }

        }

        return false;
    }


    public boolean isFull(DatagramPacket [] p){

        for (int x=0; x < p.length; x++)
            if (x == p.length - 1){
                return true;
            }


        return false;
    }
    public void shift(int offset){


        DatagramPacket [] copy_array = new DatagramPacket[size];
        DatagramPacket [] copy_array2 = new DatagramPacket[size];

        System.arraycopy(Data_Array,offset,copy_array,0,size-offset);
        System.arraycopy(Ack_Array,offset,copy_array2,0,size-offset);

        Data_Array = copy_array;
        Ack_Array = copy_array2;

    }

}