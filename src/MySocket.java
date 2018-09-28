import java.io.*;
import java.net.Socket;

public class MySocket{
    BufferedReader reader;
    BufferedWriter writer;
    Socket socket;

    public MySocket(Socket sock){
         InputStream in = null;
         OutputStream out= null;
         try {
             in = sock.getInputStream();
             out = sock.getOutputStream();
         } catch (IOException e1) {
             e1.printStackTrace();
         }
         socket=sock;
         reader = new BufferedReader(new InputStreamReader(in));
         writer = new BufferedWriter(new OutputStreamWriter(out));
     }
}
