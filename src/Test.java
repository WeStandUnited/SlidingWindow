import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.RandomAccess;
import java.util.concurrent.TimeUnit;

public class Test {
    static byte [] encryptDecrpyt(byte [] input){
        // Define XOR key
        // Any character value will work
        int xorKey = 7;

        // Define String to store encrypted/decrypted String
        byte[] output = new byte[input.length];

        // calculate length of input string
        int len = input.length;

        // perform XOR operation of key
        // with every caracter in string
        for (int i = 0; i < len; i++) {
            output[i] = (byte)(input[i] ^ xorKey);
        }
        return output;
    }

    public static void main(String[] args) throws InterruptedException {

        long startTime = System.nanoTime();
        System.out.println(startTime);
        TimeUnit.SECONDS.sleep(3);

        long estimatedTime = System.nanoTime() - startTime;
        System.out.println(estimatedTime);


    }
}
