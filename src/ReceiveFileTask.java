import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/*
    Task per ricevere un file da un altro client
 */

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
            //apro il server socket a cui dovrà connettersi il mittente del file
            ServerSocketChannel s = ServerSocketChannel.open();
            //lo attacco alla porta comunicata in precedenza
            s.bind(new InetSocketAddress(port));
            //accetto connessioni
            SocketChannel a =s.accept();
            System.out.println("ServerSocket arriva qualcosa");
            //creo il file channel
            FileChannel fc =null;
            //recupero la data attuale
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            //e recupero il nome del file
            String nomefile = filename.substring(filename.lastIndexOf("/")+1);
            //path della home dell'utente
            String path = System.getProperty("user.home")+"/"+timeStamp+"-"+nomefile;

            try{
                //apro il filechannel
                fc = FileChannel.open(Paths.get(path),StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
            catch (Exception e){
                e.printStackTrace();

            }
            //alloco un buffer di 1024 byte
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            //inzio a leggere
            int readed = a.read(buffer);
            System.out.println(readed);
            while(readed!=-1){
                readed=a.read(buffer);
                System.out.println(readed);
                //una volta letto, flippo il buffer e comincio a scrivere nel file
                buffer.flip();
                fc.write(buffer);
                //ripristino il buffer
                buffer.clear();
            }
            //file ricevuto
            System.out.println("Salvo in:"+path);
            JFrame filemess = new JFrame();
            filemess.setSize(350,150);
            JLabel jl = new JLabel("Hai ricevuto un file!");
            jl.setHorizontalAlignment(JLabel.CENTER);
            filemess.add(jl);
            filemess.setResizable(false);
            Dimension screendim = Toolkit.getDefaultToolkit().getScreenSize();
            filemess.setLocation(screendim.width/2-175,screendim.height/2-75);
            //visualizzo il messaggio
            filemess.show();
            //chiudo socketchannel e il serversocketchannel
            a.close();
            s.close();


        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
}
