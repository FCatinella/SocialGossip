import com.sun.org.apache.xml.internal.resolver.readers.ExtendedXMLCatalogReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Iterator;

public class ClientGroupListener extends Thread{

    //prende un array di indirizzi da cui ascoltare
    // deve essere aggiornato  tramite metodi chiamati dal clientListener
    //aggiorna l'ui
    ClientMenù ui;
    ArrayList<MulticastSocket> multicastSockets;

    public ClientGroupListener(ClientMenù ui,ArrayList multicastSockets){
        this.ui=ui;
        this.multicastSockets=multicastSockets;
    }

    @Override
    public void run() {
        while(true){
            for (int i =0; i<multicastSockets.size();i++){
                MulticastSocket aux = multicastSockets.get(i);
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf,buf.length);
                try{
                    aux.setSoTimeout(1000);
                    aux.receive(dp);
                    String str=new String(buf);
                    str=str.trim();
                    execute(str);

                }
                catch (Exception e){
                }

            }
        }
        // li controlla e guarda quali sono pronti (attesa attiva)
    }

    public void execute(String mess){
       JSONParser parser = new JSONParser();
       try {
           //traduco il messaggio
           System.out.println("Multicast: "+mess);
           JSONObject jsonMess = (JSONObject) parser.parse(mess);
           String sender =(String) jsonMess.get("USERNAME");
           String groupReceiver = (String) jsonMess.get("RECEIVER");
           //faccio quel che devo e passo gli aggiornamenti all'interfaccia
           String text= (String) jsonMess.get("CONTENT");
           ui.sendToChatUI(sender,groupReceiver,text,1);
       }
       catch (Exception e){
           e.printStackTrace();
       }

    }


    public void addAdrr(String mca){
        try{
            MulticastSocket mcs = new MulticastSocket(4999);
            //crea i socket e li aggiunge alla lista
            InetAddress ia = InetAddress.getByName(mca);
            mcs.joinGroup(ia);
            multicastSockets.add(mcs);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
