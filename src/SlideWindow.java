import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SlideWindow{
    int null_counter = 0;
    DatagramPacket[] Data_Array = null;
    DatagramPacket [] Ack_Array= null;
    long [] Data_Timer = null;
    ArrayList<DatagramPacket> DataBuffer;
    File file;
    InetAddress host;
    int port;
    int XOR;
    int size = 0;
    int ACK_Bookkeeping;
    int Data_Bookkeeping;

    public SlideWindow(int s,int Buffer_Size,int hasher)
    {
        size = s;
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


        for (long i = 0; i < file.length(); i +=510) {//changed i+= 510 -> i++
            byte[] data = new byte[510];

            ra.seek(i);//changed from i -> i*510

            ra.read(data);

            ByteBuffer buffer = ByteBuffer.allocate(512);


            System.out.println("AddBlock_Num:"+i);
            buffer.putLong(i);

            String s = new String(data);

            // System.out.println("[Placing]"+s);
            //System.out.println(s.length());
            buffer.put(data);

            buffer.flip();

            DatagramPacket packet;

            packet = new DatagramPacket(hash(buffer.array()), buffer.array().length,host, port);
            System.out.println("Data Packet:"+packet.getLength());
            DataBuffer.add(packet);
            // System.out.println("[Data Buffer]Add:"+i);
            // System.out.println(new String(hash(packet.getData())));

        }
        ra.close();

    } // Use when sending file
    public long getBlockNumber(DatagramPacket p){

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this



        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();

        //buff.getShort();

        return buff.getLong();
    }
    public Long getACKBlockNumber(DatagramPacket p){

        int data_size = p.getLength();

        byte[] raw_packet = hash(p.getData());// NOTE : Unpack Functions have no need to unhash because of this



        //switch statement telling the bytes to

        ByteBuffer buff = ByteBuffer.allocate(data_size);
        buff.put(raw_packet);
        buff.flip();

        buff.getShort();

        return buff.getLong();
    }
    public long getData(long blocknum) throws IOException {
        for (int i=0;i<size;i++){
            DatagramPacket p = Data_Array[i];
            if (blocknum == getBlockNumber(p)){
                return blocknum;
            }

        }
        return -1;
    }
    public void add_Data(DatagramPacket p,int index){// adds data into data window

        //for (int i = size-1; i>-1 ; i--) {
        // System.out.println("[Data Array]:Add"+index);
        Data_Array[index] = p;
        Data_Bookkeeping++;


    }
    public void add_Data(DatagramPacket p){// adds data into data window
        //for (int i = size-1; i>=0 ; i--)
        for (int i = 0; i<size ; i++)
        {

            if (Data_Array[i] == null){
                //     System.out.println("[Data Array]:Add"+i);

                Data_Array[i] = p;
                Data_Bookkeeping++;

            }

        }

    }
    public int checkTimers(){
        System.out.print("Timers:[");
        for (int i =0;i<Data_Timer.length;i++){
            // if window.Data_Timer[i] >= 3 seconds resend
            System.out.print(System.nanoTime() - Data_Timer[i]+",");
            if (System.nanoTime() - Data_Timer[i]>= 6000561965L) {
                return i;
            }


        }
        System.out.print("]\n");

        return -1;
    }

    public void Fill_Data_Array() throws IOException {//if fill is not even then may produce INDEX OUT OF BOUNDS
        try {
            int lastvalue =0;
            for (int j = 0; j < Data_Array.length; j++) {
                lastvalue = j;
                add_Data(DataBuffer.get(j + null_counter), j);
            }
        } catch (IndexOutOfBoundsException a){
            return;
        }
    }

    public void remove(int index) {
        Data_Array[index] = null;
        Data_Timer[index] = 0;
        null_counter++;
        Data_Bookkeeping--;
        ACK_Bookkeeping--;
        if (Data_Bookkeeping < 0)Data_Bookkeeping =0;
        if (ACK_Bookkeeping < 0)ACK_Bookkeeping =0;

    }
    public void removeByBlockNum(long blockNum) {

        for (int i = 0; i <Data_Array.length ; i++) {

            if (Data_Array[i] != null) {
                if (blockNum == getBlockNumber(Data_Array[i])) {
                    Data_Array[i] = null;
                    null_counter++;
                }

            }
        }


    }


    public void removeData(int index) {//TODO instead of removing by index remove by matching block number

        //  if (index-null_counter < 0) {

        //  }else {
        Data_Array[index - null_counter] = null;
        Data_Timer[index - null_counter] = 0;
        null_counter++;
        Data_Bookkeeping--;
        if (Data_Bookkeeping < 0) Data_Bookkeeping = 0;
        // }
    }
    public void clearAcks(){
        for (int i=0;i<size;i++){
            Ack_Array[i] = null;
        }
        ACK_Bookkeeping =0;
    }
    public boolean add_ACK(DatagramPacket p){
        //True in meaning successful
        for (int i = size-1; i>-1 ; i--) {

            if (Ack_Array[i] == null){

                Ack_Array[i] = p;
                ACK_Bookkeeping++;
                return true;
            }

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