import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMenù implements ActionListener {
    //elementi interfaccia
    private JFrame finestra = null; //finestra del client
    private JTextArea notiList = null; // lista delle notifiche
    private JFrame addWind = null; //finestra aggiunta amico o gruppo
    private JFrame addGroupWind=null; //finestra di creazione gruppo
    private DefaultListModel friendModel = null;
    private DefaultListModel groupModel;
    private JTextArea addArea = null;

    //variabili globali
    private ConcurrentHashMap<String,ClientChat> chatAperte; //hashMap chat normali aperte
    private ConcurrentHashMap<String,ClientChat> chatGruppiAperte; //hashMap chat di gruppo aperte
    private String username;

    //variabili per RMI
    private ServerInterface server =null;
    private RemoteNotiInterf stub = null;
    private BufferedWriter writer;

    public ClientMenù(String username, Socket sock){
        this.username=username;
        this.chatAperte= new ConcurrentHashMap<>();
        this.chatGruppiAperte = new ConcurrentHashMap<>();
        createWind();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //listener per il "chiudi della finestra"
        finestra.addWindowListener(new WindowAdapter( ){
            public void windowClosing(WindowEvent event) {
                System.out.println("Chiusa");
                disconnectFromServer();
                //chiudo il socket
                System.exit(0);
            }
        });

        //RMI
        try {
            //recupero il registry
            Registry registry = LocateRegistry.getRegistry(5000);
            //cerco l'oggetto remoto
            server=(ServerInterface) registry.lookup("Server");
            //si registra per la callback
            RemoteNotiInterf callbackObj = new RemoteNoti(notiList,friendModel,username);
            stub = (RemoteNotiInterf) UnicastRemoteObject.exportObject(callbackObj, 0);
            server.registerForCallback(stub);
        }
        catch ( Exception e) {
            System.err.println(e.getMessage());
        }

        //richiedo la lista degli amici del client
        JSONObject mess = new JSONObject();
        mess.put("OP","LISTFRIENDS");
        mess.put("USERNAME",username);
        //invio il messaggio e aspetto la risposta
        sendToServer(mess);
        //ora richiedo la lista dei gruppi
        mess.put("OP","LISTGROUPS");
        sendToServer(mess);
    }




    private void createWind() {
        //creo la finestra della chat
        int windwX=550;
        int windwY=500;
        finestra = new JFrame (username+" - Social Gossip");
        finestra.setSize(windwX,windwY);
        finestra.setLocation(400, 100);
        finestra.setLayout(null);

        finestra.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                //Richiede la disconnessione dal server e chiude
                try {
                    //deregistro la il client per le callback
                    server.unregisterForCallback(stub);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                //mi disconnetto dal server
                disconnectFromServer();
                System.exit(0);
            }
        });

        //nome utente
        JLabel userTitolo = new JLabel(username);
        userTitolo.setFont(new Font("Arial",Font.BOLD,17));
        userTitolo.setBounds(50,400,200,50);
        finestra.add(userTitolo);

        //lista amici
        JLabel friendTitolo = new JLabel("Amici");
        friendTitolo.setFont(new Font ("Arial",Font.PLAIN,14));
        friendTitolo.setBounds(50,10,200,50);
        finestra.add(friendTitolo);
        friendModel= new DefaultListModel();
        JList friendList = new JList(friendModel);

        //mouse listener per la Jlist
        MouseListener mouseListenerAmici = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getClickCount()==2 && friendList.getSelectedValue()!=null){
                    String amico = (String) friendList.getSelectedValue();
                    //devo far partire la finestra della chat
                    System.out.println("Sto clickando su "+amico);
                    System.out.println("combo: "+username+amico);
                    //l'intero in fondo indica la modalità della finestra ( 0 = Chat con Amico , 1 = chat con Gruppo )
                    ClientChat aux = new ClientChat(username,amico,writer,0,chatAperte);
                    //faccio apparire le finestre di chat in posizioni diverse per non farle sovrapporre
                    double offset = 500*Math.random();
                    int intOff = (int) offset;
                    aux.setPosition(intOff%800,finestra.getY());
                    if(chatAperte.containsKey(username+amico)){
                        //se c'è già una chat aperta la chiudo e ne apro una nuova
                        chatAperte.get(username+amico).close();
                        chatAperte.remove(username+amico);
                    }
                        chatAperte.put(username+amico,aux);
                }
            }
        };
        friendList.addMouseListener(mouseListenerAmici);
        //posso selezionare solo un nome della lista
        friendList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        friendList.setLayoutOrientation(JList.VERTICAL_WRAP);
        JScrollPane scrollPaneStatus = new JScrollPane(friendList);
        scrollPaneStatus.setBounds(50,50,200,330);
        finestra.add(scrollPaneStatus);

        //finestra Gruppi
        JLabel groupsTitolo = new JLabel("Gruppi");
        groupsTitolo.setFont(new Font("Arial",Font.PLAIN,14));
        groupsTitolo.setBounds(300,10,200,50);
        finestra.add(groupsTitolo);
        groupModel = new DefaultListModel();
        JList groupList = new JList(groupModel);
        MouseListener mouseListenerGruppi = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                //quando clicco due volte su un nome devo aprire una nuova finestra
                if(e.getClickCount()==2 && groupList.getSelectedValue()!=null){
                    String gruppo = (String) groupList.getSelectedValue();
                    //devo far partire la finestra della chat gruop (mode=1)
                    ClientChat aux = new ClientChat(username,gruppo,writer,1,chatAperte);
                    //faccio apparire le finestre di chat in posizioni diverse per non farle sovrapporre
                    double offset = 500*Math.random();
                    int intOff = (int) offset;
                    aux.setPosition(intOff%800,finestra.getY());
                    if(chatGruppiAperte.containsKey(gruppo)){
                        //se c'è già una chat aperta la chiudo e ne apro una nuova
                        chatGruppiAperte.get(gruppo).close();
                        chatGruppiAperte.remove(gruppo);
                    }
                    chatGruppiAperte.put(gruppo,aux);
                }
            }
        };
        groupList.addMouseListener(mouseListenerGruppi);
        //posso selezionare solo un gruppo alla volta
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setLayoutOrientation(JList.VERTICAL_WRAP);
        JScrollPane groupScrollPane= new JScrollPane(groupList);
        groupScrollPane.setBounds(300,50,200,150);
        finestra.add(groupScrollPane);

        //pulsante crea gruppo
        JButton addGroup = new JButton("+");
        addGroup.setBounds(450,25,50,20);
        addGroup.setFont(new Font("Arial",Font.PLAIN,10));
        addGroup.addActionListener(this);
        finestra.add(addGroup);



        //finestra notifiche
        JLabel notiTitolo = new JLabel ("Notifiche");
        notiTitolo.setFont(new Font ("Arial",Font.PLAIN,14));
        notiTitolo.setBounds(300,144,200,150);
        finestra.add(notiTitolo);
        notiList = new JTextArea();
        JScrollPane notiScrollPane= new JScrollPane(notiList);
        notiScrollPane.setBounds(300,230,200,150);
        notiList.setEditable(false);
        finestra.add(notiScrollPane);

        //pulsante disconnetti
        JButton discButton = new JButton("Esci");
        discButton.setBounds(windwX-120, 410, 70, 30);
        discButton.addActionListener(this);
        finestra.add(discButton);

        //pulsante aggiungi
        JButton addButton = new JButton("Aggiungi");
        addButton.setBounds(300, 410, 100, 30);
        addButton.addActionListener(this);
        finestra.add(addButton);
        finestra.setResizable(false);
        finestra.setVisible(true);

    }

    //LISTENER PULSANTI
    @Override
    public void actionPerformed(ActionEvent e) {
        String pressed = e.getActionCommand();
        switch(pressed) {
            case "Esci":
                //Richiede la disconnessione dal server e chiude
                try {
                    //mi deregistro dalle callback
                    server.unregisterForCallback(stub);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                disconnectFromServer();
                System.exit(0);
                break;
            case "Aggiungi":
                //crea la nuova finestra con due pulsanti (aggiungi amico, aggiungiti ad un gruppo)
                // e un campo di testo dove inserire il nome del gruppo o dell'amico
                addWind = new JFrame("Aggiungi");
                addWind.setSize(380, 220);
                addWind.setLocation(finestra.getX()+100, finestra.getY()+100);
                addWind.setLayout(null);

                JLabel addLabel= new JLabel("Nome amico o gruppo");
                addLabel.setBounds(30,20,320,20);
                addWind.add(addLabel);

                addArea = new JTextArea();
                addArea.setBounds(30,40,320,20);
                addWind.add(addArea);

                JButton addFriend = new JButton("Aggiungi amico");
                addFriend.setBounds(30,80,320,40);
                addFriend.addActionListener(this);
                addWind.add(addFriend);
                JButton addGroup = new JButton("Aggiungiti ad un gruppo");
                addGroup.setBounds(30,130,320,40);
                addGroup.addActionListener(this);
                addWind.add(addGroup);

                addWind.setResizable(false);
                addWind.setVisible(true);
                break;

            case "Aggiungi amico":{
                String friendToAdd = addArea.getText();
                JSONObject mess = new JSONObject();
                mess.put("OP","ADDFRIEND");
                mess.put("USERNAME",username);
                mess.put("FRIEND", friendToAdd);
                //invio richiesta
                sendToServer(mess);
                }
                break;

            case "Aggiungiti ad un gruppo": {
                String groupToAdd = addArea.getText();
                JSONObject mess = new JSONObject();
                mess.put("OP", "ADDGROUP");
                mess.put("USERNAME", username);
                mess.put("GROUP", groupToAdd);
                //invio richiesta
                sendToServer(mess);
                break;
            }
            case "+":{
                addGroupWind = new JFrame("Crea Gruppo");
                addGroupWind.setSize(350, 130);
                addGroupWind.setLocation(finestra.getX()+100, finestra.getY()+100);
                addGroupWind.setLayout(null);
                JTextArea groupArea = new JTextArea();
                groupArea.setBounds(10,10,330,20);
                JButton cB= new JButton("Crea Gruppo");
                cB.setBounds(110,50,125,30);
                cB.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        String groupToAdd = groupArea.getText();
                        JSONObject mess = new JSONObject();
                        mess.put("OP", "CREATEGROUP");
                        mess.put("USERNAME", username);
                        mess.put("GROUP", groupToAdd);
                        //invio richiesta
                        sendToServer(mess);
                    }
                });
                addGroupWind.setResizable(false);
                addGroupWind.add(cB);
                addGroupWind.add(groupArea);
                addGroupWind.show();


            }
        }

    }

    //invia il messaggio al server
    private void sendToServer(JSONObject mess) {
        try {
            writer.write(mess.toJSONString());
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            //socket chiuso
            System.out.println("Richiesta la chiusura ma server non raggiungibile");
        }
    }

    //invia messaggio di disconnessione al server
    private void disconnectFromServer() {
        JSONObject mess = new JSONObject();
        mess.put("OP","DISCONNECT");
        mess.put("USERNAME",username);
        try {
            writer.write(mess.toJSONString());
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            //socket chiuso
            System.out.println("Richiesta la chiusura ma server non raggiungibile");
        }

    }

    //funzioni per modificare : lista amici e lista gruppi dell'interfaccia utente
    public void addFriendList(String friend){
        friendModel.addElement(friend);
    }
    public void addGroupList(String group){
        groupModel.addElement(group);
    }
    public void removeGroupList(String group){
        groupModel.removeElement(group);
    }


    //invia il messaggio all'Ui di una chat
    public void sendToChatUI(String sender,String receiver, String message,int mode){
        //prendo le due combinazioni possibili di chiave che identificano la chat
        String ver1 = sender+receiver;
        String ver2 = receiver+sender;
        if(mode==0){
            if(chatAperte.containsKey(ver1)){
                ClientChat cc = chatAperte.get(ver1);
                cc.chatArea.append(sender+": "+message+"\n");
            }
            else if(chatAperte.containsKey(ver2)){
                ClientChat cc = chatAperte.get(ver2);
                cc.chatArea.append(sender+": "+message+"\n");
            }
            else{
                //non c'è nessuna chat aperta
                //ne apro una nuova e la aggiungo a quelle aperte
                ClientChat aux = new ClientChat(receiver,sender,writer,0,chatAperte);
                double offset = 10*Math.random();
                int intOff = (int) offset;
                aux.setPosition(intOff%100,finestra.getY());
                chatAperte.put(receiver+sender,aux);
                //scrivo il messaggio nel box della chat
                aux.chatArea.append(sender+":"+message+"\n");
            }
        }
        else {
            if(chatGruppiAperte.containsKey(receiver)){
                ClientChat cc = chatGruppiAperte.get(receiver);
                cc.chatArea.append(sender+": "+message+"\n");
            }
        }
    }



}
