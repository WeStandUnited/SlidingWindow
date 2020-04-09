import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TestClient {


    public static void main(String[] args) throws IOException {
        DatagramSocket ssock = new DatagramSocket(2770);

        PacketService s = new PacketService(ssock,false,false, InetAddress.getByName("localhost"),2771,0);
        s.PacketUtilRecieveFileLength();
    }
}
