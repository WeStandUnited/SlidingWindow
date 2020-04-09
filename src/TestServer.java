import java.io.IOException;
import java.net.*;

public class TestServer {


    public static void main(String[] args) throws IOException {
        DatagramSocket ss = new DatagramSocket(2771);
        PacketService pss = new PacketService(ss,false,false, InetAddress.getByName("192.168.1.6"),2771,0);
        byte[] b = new byte[8];
        DatagramPacket p = new DatagramPacket(b,b.length,InetAddress.getByName("192.168.1.6"),2771);
        ss.send(p);
        pss.setFile_length(100L);
        pss.PacketUtilSendFileLength();
    }
}
