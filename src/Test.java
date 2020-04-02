import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.RandomAccess;

public class Test {

    public static void main(String[] args) {
        int size = 10;
        int p = 111;
        int [] Data_Window = new int[size];
        for (int i = 0; i < size ; i++) {
            Data_Window[i]=i;
            System.out.println(Data_Window[i]);
        }

        for (int i = size-1; i>-1 ; i--) {
            System.out.println(Data_Window[i]);
        }


    }
}
