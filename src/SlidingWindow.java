import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;


public class SlidingWindow {



    DatagramPacket [] Send;
    DatagramPacket [] Recieve;




    public SlidingWindow (int size, String file,boolean uploading) throws IOException {

        Send = new DatagramPacket[size];
        Recieve = new DatagramPacket[size];


        if (uploading){
            // I am uploading
            File f = new File(file);




        }else{
            //I am Downloading
            File f = new File(file);
            f.createNewFile();


        }





    }


    public static void main(String[] args) throws IOException {
        SlidingWindow sl = new SlidingWindow(10,"hello.txt",false);


    }




}