import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerListenerTask implements Runnable {
    private CopyOnWriteArrayList<MySocket> listaSocket;
    private ThreadPoolServer threadpool;
    private ConcurrentHashMap<String,User> tabella;
    private RMIServerImp RMI;
    private ConcurrentHashMap<String,UserGroup> tabellaGruppi;


    //task che si occupa di leggere dai socket i messaggi -> si comporta come una select ( fa un pò di attesa attiva )
    public ServerListenerTask(ConcurrentHashMap<String,User> tabellaUtentiArg, RMIServerImp RmiServer,ConcurrentHashMap<String,UserGroup> tabellaGruppi){
        //lista di socket da cui ricevere i messaggi
        listaSocket = new CopyOnWriteArrayList<MySocket>();
        //threadpool a cui fare eseguire i task
        threadpool = new ThreadPoolServer();
        tabella=tabellaUtentiArg;
        RMI=RmiServer;
        this.tabellaGruppi=tabellaGruppi;


    }

    //funzione per aggiungere i socket da ascoltare
    public void addSocket(Socket sockToAdd){
            listaSocket.add(new MySocket(sockToAdd));
    }

    public void run(){

        Vector<Boolean> tO = new Vector<>();
        Vector<Boolean> qO = new Vector<>();
        for (int i = 0; i < 256 ; i++) {
            tO.add(i,true);
            qO.add(i,true);
        }
        //leggo la richiesta dal socket e la faccio esaudire da un thread
        Boolean stop = false;
        JSONParser parser = new JSONParser();
        while (!stop){
            Iterator i = listaSocket.iterator();
            while (i.hasNext()){
                MySocket mSock = (MySocket) i.next();
                //se il socket è chiuso, viene rimosso
                if(mSock.socket.isClosed()) {
                    System.out.println("Socket rimosso");
                    listaSocket.remove(mSock);
                    break;
                }
                //leggo il messaggio se il socket è pronto ( c'è qualcosa da leggere )
                String mess=null;
                JSONObject rec=new JSONObject();
                try {
                    if(mSock.reader.ready()){
                        mess = mSock.reader.readLine();
                        System.out.println("Ho letto "+mess);
                        //converto la stringa letta in un oggetto JSON
                        rec=(JSONObject) parser.parse(mess);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //se ho letto qualcosa aggiungo il task al threadpool
                if(mess!=null){
                    ServerTask task = new ServerTask(rec,mSock,tabella,RMI,tabellaGruppi,tO,qO);
                    threadpool.executeTask(task);
                }

            }
        }

    }
}
