import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

//task che comunica con il server
public class ClientListener extends Thread {
    private Socket sock;
    private BufferedReader reader;
    private ClientMenù ui;
    private String username;

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
        Boolean result = true;
        while (quit) {
            try {
                //legge il messaggio
                String rec = reader.readLine();
                JSONParser parser = new JSONParser();
                JSONObject recJSON = (JSONObject) parser.parse(rec);
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
                                ui.sendToChatUI(sender,receiver,message);
                            }

                    }
                }
            } catch (Exception e) {

            }
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
