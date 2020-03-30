import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class SlidingWindow {

    int mode;
    int size;
    ArrayList<DatagramPacket> send;
    ArrayList<DatagramPacket> recieve;



    public SlidingWindow (int size,int mode) throws IOException {
        size = this.size;
        send =  new ArrayList<DatagramPacket>(size);
        recieve =  new ArrayList<DatagramPacket>(size);

        mode = this.mode;


    }

    public void add(){

    }
    public static <T> List<T> shift(List<T> aL, int shift) {
        List<T> newValues = new ArrayList<>(aL);
        Collections.rotate(newValues, shift);

        return newValues;
    }
    public static void main(String[] args) {

        String [] strs = { null, null, "C",
                "D", "E", "F", "G" };
        ArrayList<String> stringL = new ArrayList<String>(Arrays.asList(strs));

        System.out.println(shift(stringL,5));

    }



}
