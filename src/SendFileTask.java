import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SendFileTask extends Thread {

    File toSend;
    SocketChannel sc;
    String ipReceiver;
    int port;

    public SendFileTask(File toSend,String ip,int port){
        this.toSend=toSend;
        ipReceiver=ip;
        this.port=port;


    }

    @Override
    public void run() {
        super.run();
        try{
            //devo collegarmi con il ricevente
            System.out.println("Provo a connettermi su: "+ipReceiver+" "+port);
            SocketAddress address = new InetSocketAddress(ipReceiver,port);
            sc=SocketChannel.open();
            sc.connect(address);

            FileChannel inFc= FileChannel.open(Paths.get(toSend.getAbsolutePath()), StandardOpenOption.READ);
            //ora devo scrivere direttamente nel channel
            System.out.println("Invio il file");
            inFc.transferTo(0,inFc.size(),sc);

        }
        catch (Exception e){
            e.printStackTrace();
        }
        try{
            sc.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }
}
