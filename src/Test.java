import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.RandomAccess;

public class Test {

    public static void Fill_Data(short BlockNum) throws IOException {

        RandomAccessFile ra = new RandomAccessFile(new File("file.txt"),"r");

        byte [] data = new byte[512];
        ra.seek((long)BlockNum);
        ra.read(data);
        ByteBuffer buffer = ByteBuffer.allocate(2+2+512);

        buffer.putShort(BlockNum);
        buffer.put(data);
        buffer.flip();
        System.out.println(new String(buffer.array()));
    }

    public static void main(String[] args) throws IOException {

        Fill_Data((short)3);

    }
}
