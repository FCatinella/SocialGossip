import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
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
            try {
                //legge il messaggio
                String rec = reader.readLine();
                JSONParser parser = new JSONParser();
                JSONObject recJSON = (JSONObject) parser.parse(rec);

                System.out.println("Ricevuto: "+recJSON.toJSONString());
                //recupera l'esito della richiesta
                result = (Boolean) recJSON.get("RESULT");
                String errorMess = (String) recJSON.get("ERRORMESS");
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
                            //recupero la lista degli amici e la visualizzo
                            ArrayList<String> amici = (ArrayList<String>) recJSON.get("CONTENT");
                            int i;
                            for(i=0;i<amici.size();i++){
                                String aux = amici.get(i);
                                System.out.println(aux);
                                ui.addFriendList(aux);
                            }
                            break;

                        case "LISTGROUPS":
                            //recupero la lista degli amici e la visualizzo
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
                                    //crea i socket e li aggiunge alla lista
                                    InetAddress ia = InetAddress.getByName(multiCastAddrs.get(k));
                                    mcs.joinGroup(ia);
                                    multicastSockets.add(mcs);
                                }
                                catch (Exception e ){
                                    e.printStackTrace();
                                }

                            }
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
                            addAdrr(ipAdrr);
                            cgl.stop();
                            cgl = new ClientGroupListener(ui,multicastSockets);
                            cgl.start();
                            break;

                    }
                }
            } catch (Exception e) {


            }
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


    //visualizza "errorMess" in una finestra
    private void visualizeError(String errorMess){
        JFrame infoWind = new JFrame();
        infoWind.setSize(320,150);
        infoWind.setLocation(250, 250);
        infoWind.setLayout(null);
        JLabel errorMessage = new JLabel (errorMess);
        errorMessage.setBounds(20, 40, 280, 30);
        infoWind.add(errorMessage);
        infoWind.setResizable(false);
        infoWind.setVisible(true);
    }
}
