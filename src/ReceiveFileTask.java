import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ReceiveFileTask extends Thread {

    String filename;
    int port;

    public ReceiveFileTask(String nome,int port){
        filename=nome;
        this.port=port;
    }

    @Override
    public void run(){
        super.run();
        try{
            System.out.println("ServerSocket channel in ascolto su "+port);
            ServerSocketChannel s = ServerSocketChannel.open();
            s.bind(new InetSocketAddress(port));
            SocketChannel a =s.accept();
            System.out.println("ServerSocket arriva qualcosa");
            FileChannel fc =null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            String nomefile = filename.substring(filename.lastIndexOf("/")+1);
            String path = System.getProperty("user.home")+"/"+timeStamp+"-"+nomefile;

            try{
                fc = FileChannel.open(Paths.get(path),StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
            catch (Exception e){
                e.printStackTrace();

            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            int readed = a.read(buffer);
            System.out.println(readed);
            while(readed!=-1){
                readed=a.read(buffer);
                System.out.println(readed);
                buffer.flip();
                fc.write(buffer);
                buffer.clear();
            }
            //file ricevuto
            System.out.println("Salvo in:"+path);
            JFrame filemess = new JFrame("Hai ricevuto un file!");
            filemess.setSize(350,150);
            filemess.add(new JLabel("Hai ricevuto un nuovo messaggio"));
            filemess.setLocation(MouseInfo.getPointerInfo().getLocation());
            filemess.show();
            a.close();
            s.close();


        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
}
