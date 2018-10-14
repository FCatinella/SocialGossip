import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

//task che comunica con il server
public class ClientListener extends Thread {
    private Socket sock;
    private BufferedReader reader;
    private ClientMenù ui;
    private String username;
    ArrayList<MulticastSocket> multicastSockets =null;
    ClientGroupListener cgl = null;

    public ClientListener(Socket sock,ClientMenù ui,String username){
        this.username=username;
        this.sock=sock;
        this.ui=ui;
        try {
            reader= new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void run() {
        Boolean quit = true;
        Boolean result;
        multicastSockets= new ArrayList<>();

        while (quit) {
            if(sock.isClosed()){
                //se il socket è chiuso esci
                break;
            }
            String rec;
            //leggo il messaggio
            try{
                rec = reader.readLine();
            }
            catch (IOException e){
                //se ho letto 0 byte esci
                System.out.println("Socket chiuso");
                break;
            }
            JSONParser parser = new JSONParser();
            JSONObject recJSON=null;
            //traduco la stringa in un oggetto JSON
            try{
                recJSON= (JSONObject) parser.parse(rec);
            }
            catch (NullPointerException e){
                break;
            }
            catch (ParseException e){
                e.printStackTrace();
            }
            //recupera l'esito della richiesta
            result = (Boolean) recJSON.get("RESULT");
            String errorMess = (String) recJSON.get("ERRORMESS");
            //se l'esito non è positivo
            if (!result) {
                //visualizza una finestra di errore
                visualizeError(errorMess);
            }
            else {
                // se è andato tutto liscio
                String resultOp = (String) recJSON.get("OP");
                //esegue comandi in base all'operazione richiesta in origine
                switch (resultOp) {
                    case "LISTFRIENDS":
                        //recupero la lista degli amici e la passo alla UI
                        ArrayList<String> amici = (ArrayList<String>) recJSON.get("CONTENT");
                        int i;
                        for(i=0;i<amici.size();i++){
                            String aux = amici.get(i);
                            System.out.println(aux);
                            ui.addFriendList(aux);
                        }
                        break;

                    case "LISTGROUPS":
                        //recupero la lista dei gruppi e la passo alla UI
                        ArrayList<String> gruppi = (ArrayList<String>) recJSON.get("CONTENT");
                        int j;
                        for (j = 0; j < gruppi.size(); j++) {
                            String aux = gruppi.get(j);
                            System.out.println(aux);
                            ui.addGroupList(aux);
                        }
                        ArrayList<String> multiCastAddrs = (ArrayList<String>) recJSON.get("CONTENT2");
                        //recupera gli indirizzi dalla lista inviata,
                        for (int k =0;k<multiCastAddrs.size();k++){
                            try{
                                MulticastSocket mcs = new MulticastSocket(4999);
                                //crea i socket e li aggiunge alla lista dei socket Multicast da ascoltare
                                InetAddress ia = InetAddress.getByName(multiCastAddrs.get(k));
                                mcs.joinGroup(ia);
                                multicastSockets.add(mcs);
                            }
                            catch (Exception e ){
                                e.printStackTrace();
                            }

                        }
                        //avvio il thread che asolterà dai socket multicast
                        cgl = new ClientGroupListener(ui,multicastSockets);
                        cgl.start();
                        break;

                    case "ADDFRIEND":
                        //aggiungo l'amico alla lista degli amici
                        String friendToAdd = (String) recJSON.get("FRIEND");
                        ui.addFriendList(friendToAdd);
                        break;

                    case "CHATMESSAGE":
                        //devo leggere il mesaggio e aggiornare la chat
                        String message =(String) recJSON.get("CONTENT");
                        String sender=(String) recJSON.get("USERNAME");
                        String receiver=(String) recJSON.get("RECEIVER");
                        if (sender.equals(username)|| receiver.equals(username)){
                            ui.sendToChatUI(sender,receiver,message,0);
                        }
                        break;

                    case "ADDGROUP":
                        //aggiungo il gruppo alla lista dei gruppi (UI)
                        ui.addGroupList((String)recJSON.get("GROUP"));
                        String ipAdrr = (String)recJSON.get("GROUPADDR");
                        addAdrr(ipAdrr); //aggiungo l'indirizzo multicast a quelli che devo ascoltare
                        cgl.stop();
                        cgl = new ClientGroupListener(ui,multicastSockets);
                        cgl.start();
                        break;

                    case "RECEIVEFILE-ACK":
                        //ack dopo la richiesta di invio di un file
                        //recupero il nome del file
                        String filename=(String) recJSON.get("FILENAME");
                        //lo apro
                        File file = new File(filename);
                        try{
                            //recupero l'ip e porta dal messaggio
                            String clientIp=(String) recJSON.get("IP");
                            int clientPort= ((Long)recJSON.get("PORT")).intValue();
                            //comincio ad inviare il file
                            SendFileTask sft= new SendFileTask(file,clientIp,clientPort);
                            //avvio il task in un nuovo thread
                            sft.start();
                        }
                        catch ( Exception e1){
                            e1.printStackTrace();
                        }
                        break;

                    case "RECEIVEFILE":{
                        //devo ricevere il file
                        System.out.println("Ricevo file");
                        //invio indirizzo ip e porta al mittente (tramite server)
                        JSONObject ackMess = new JSONObject();
                        ackMess.put("OP","RECEIVEFILE-ACK");
                        String senderFile= (String) recJSON.get("SENDER");
                        String fileName=(String) recJSON.get("FILENAME");
                        ackMess.put("SENDER",senderFile);
                        ackMess.put("RECEIVER",username);
                        ackMess.put("FILENAME",fileName);
                        try {
                            //inserisco il mio ip
                            ackMess.put("IP", InetAddress.getLocalHost().getHostAddress());
                        }
                        catch(UnknownHostException e){
                            e.printStackTrace();
                        }
                        int port;
                        //cerco una porta libera
                        for (port=1995;port<65536;port++){
                            if(checkPort(port)) break;
                        }
                        if(port<65536){ //c'è una porta libera
                            ackMess.put("PORT",port);
                            //inzio a ricevere sulla porta comunicata
                            ReceiveFileTask rft= new ReceiveFileTask(fileName,port);
                            rft.start();
                            try {
                                //invio al server il messaggio per il mittente
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.sock.getOutputStream()));
                                writer.write(ackMess.toJSONString());
                                writer.newLine();
                                writer.flush();

                            } catch (IOException e) {
                                //socket chiuso
                                System.out.println("Richiesta la chiusura ma server non raggiungibile");
                            }
                        }

                    }
                    break;
                }
            }
        }
    }

    //crea i socket multicast e li aggiunge alla lista
    public void addAdrr(String mca){
        try{
            MulticastSocket mcs = new MulticastSocket(4999);
            InetAddress ia = InetAddress.getByName(mca);
            //mi unisco al gruppo (sennò non ricevo i messaggi)
            mcs.joinGroup(ia);
            multicastSockets.add(mcs);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    //visualizza "errorMess" in una finestra
    private void visualizeError(String errorMess){
        JFrame infoWind = new JFrame();
        //recupero le dimensioni dello schermo
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        //creo la finestra e posiziono al centro il messaggio di errore
        infoWind.setSize(320,150);
        infoWind.setLocation((screensize.width/2)-160, 250);
        infoWind.setLayout(null);
        //aggiungo ill messaggio di errore
        JLabel errorMessage = new JLabel (errorMess);
        errorMessage.setBounds(20, 40, 280, 30);
        infoWind.add(errorMessage);
        infoWind.setResizable(false);
        //lo visualizzo
        infoWind.setVisible(true);
    }

    //controllo se la porta è usata oppure no
    public Boolean checkPort(int i){
        try{
            //se lancia un eccezione allora la porta è libera
            Socket aux = new Socket("localhost",i);
            aux.close();
            return false;
        }
        catch(IOException e ){
            return true;
        }
    }
}
