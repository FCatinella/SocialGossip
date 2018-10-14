import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;


//Listener che ascolta dagli indirizzi Multicast
public class ClientGroupListener extends Thread{
    /*
        Prende un array di indirizzi da cui ascoltare
        L'array deve essere aggiornato tramite metodi chiamati dal clientListener
    */
    //interfaccia utente a cui è connesso
    ClientMenù ui;
    ArrayList<MulticastSocket> multicastSockets;

    public ClientGroupListener(ClientMenù ui,ArrayList multicastSockets){
        this.ui=ui;
        //lista dei socket da ascolatare
        this.multicastSockets=multicastSockets;
    }

    @Override
    public void run() {
        while(true){
            //scorre la lista dei socket
            for (int i =0; i<multicastSockets.size();i++){
                MulticastSocket aux = multicastSockets.get(i);
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf,buf.length);
                try{
                    aux.setSoTimeout(1000);
                    //setto il ttl a 3 almeno esco dalla rete locale ma di poco
                    aux.setTimeToLive(3);
                    aux.receive(dp);
                    String str=new String(buf);
                    //rimuovo i caratteri in eccesso
                    str=str.trim();
                    //esaudisco la richiesta del messaggio
                    execute(str,i);
                }
                catch(SocketTimeoutException e){
                    //ignoro quando scatta il timer di "aux.receive(dp)"
                }
                catch (IOException  e){
                    e.printStackTrace();
                }
            }
        }
    }

    //esaudisco le richieste
    public void execute(String mess,int i){
       JSONParser parser = new JSONParser();
       try {
           //traduco il messaggio
           JSONObject jsonMess = (JSONObject) parser.parse(mess);
           String op = (String) jsonMess.get("OP");
           //sono solo due le possibilità : o DeleteGroup o Messaggio normale
           if(op.equals("DELETEGROUP")){
               //tolgo il socket da quelli da ascoltare
               multicastSockets.remove(i);
               //visualizzo un messaggio che avvisa che il gruppo è stato eliminato
               String groupName = (String) jsonMess.get("GROUP");
               ui.sendToChatUI("SISTEMA",groupName,"GRUPPO ELIMINATO",1);
               //rimuovo il gruppo dalla lista dei gruppi dell'utente
               ui.removeGroupList(groupName);
           }
           else {
               //è un messaggio normale
               String sender =(String) jsonMess.get("USERNAME");
               String groupReceiver = (String) jsonMess.get("RECEIVER");
               //passo il messaggio all'interfaccia utente
               String text= (String) jsonMess.get("CONTENT");
               ui.sendToChatUI(sender,groupReceiver,text,1);
           }
       }
       catch (Exception e){
           e.printStackTrace();
       }

    }
}
