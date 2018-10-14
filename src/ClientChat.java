import org.json.simple.JSONObject;
import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/*
    Intefaccia della chat sia tra utenti che tra gruppi
 */

public class ClientChat{
    //variabili
    String username; //nome utente
    String friend; //nome amico con cui si sta chattando
    JFrame finestra = null;
    JTextArea chatArea;
    JTextField textField;
    int mode ; // modalità della chat (0 = tra utenti, 1 = chat di gruppo)
    private BufferedWriter writer; // canale per comunicare con il server
    ConcurrentHashMap<String,ClientChat> chatAperte; // hashmap che contiene le chat che sono aperte


    public ClientChat(String username, String friend, BufferedWriter writer, int mode, ConcurrentHashMap<String,ClientChat> chatAperte){
        this.username=username;
        this.friend=friend;
        this.writer=writer;
        this.mode=mode;
        this.chatAperte=chatAperte;
        //creo la finestra
        createWindow(mode);

    }

    public void setPosition(int x,int y){
        finestra.setLocation(x,y);
    }

    public void close(){
        finestra.setVisible(false);
    }

    private void createWindow(int mode){
        finestra = new JFrame (username+": Chat con " +friend+" - Social Gossip");
        finestra.setSize(370,500);
        finestra.setLayout(null);
        finestra.setResizable(false);

        //quando la finestra viene chiusa la rimuove dalle chat aperte
        finestra.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                chatAperte.remove(username+friend);
            }
        });

        //area dedicata ai messaggi in chat
        chatArea = new JTextArea();
        chatArea.setSize(369,399);
        JScrollPane notiScrollPane= new JScrollPane(chatArea);
        notiScrollPane.setBounds(1,1,369,399);
        chatArea.setEditable(false);

        //modalità chat tra utenti
        if(mode==0){
            //campo di testo
            textField= new JTextField();
            textField.setBounds(100,410,259,40);
            textField.addKeyListener(new KeyListener()); //quando premo "invio",invia il messaggio
            finestra.add(textField);
            //pulsante allega file
            JButton sendFileB = new JButton("File");
            sendFileB.setBounds(10,410,80,40);
            sendFileB.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    JSONObject mess = new JSONObject();
                    //seleziono il file che voglio inviare
                    JFileChooser jfc= new JFileChooser();
                    jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    int n = jfc.showOpenDialog(new JFrame());
                    if(n==JFileChooser.APPROVE_OPTION) { // se ho scelto un file
                        String filename = jfc.getSelectedFile().getAbsolutePath(); //recupero il path+nome
                        //informo il server che dovrà informare il client ricevente
                        mess.put("OP", "SENDFILE");
                        mess.put("FILENAME",filename);
                        mess.put("USERNAME", username);
                        mess.put("FRIEND", friend);
                        sendToServer(mess);
                    }
                }
            });
            finestra.add(sendFileB);
        }
        //modalità chat di gruppo
        else {
            //campo di testo
            textField= new JTextField();
            textField.setBounds(100,410,259,40);
            textField.addKeyListener(new KeyListener());
            finestra.add(textField);
            //pulsante per eliminare il gruppo
            JButton delG = new JButton("Elimina");
            delG.setBounds(10,410,80,40);
            delG.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    JSONObject mess = new JSONObject();
                    //informo il server che ho chiuso il gruppo
                    // il server informerà tutti i partecipanti
                    mess.put("OP","DELETEGROUP");
                    mess.put("USERNAME",username);
                    mess.put("GROUP",friend);
                    sendToServer(mess);
                    finestra.show(false);
                }
            });
            finestra.add(delG);
        }
        finestra.add(notiScrollPane);
        finestra.show();
    }


    //Quando premo "Invio", invia il messaggio
    class KeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            if(e.getKeyCode()==10){
                System.out.println("Invio il messaggio");
                //crea il messaggio e lo invia
                JSONObject mess = createMess();
                sendToServer(mess);
            }
        }
    }


    //funzione che crea il messaggio JSON
    private JSONObject createMess(){
        JSONObject mess = new JSONObject();
        //se è un messaggio per una chat
        if(mode==0){
            mess.put("OP","CHATMESSAGE");
        }
        //è un messaggio di gruppo
        else {
            mess.put("OP","GROUPMESSAGE");
        }
        mess.put("USERNAME",username);
        mess.put("RECEIVER",friend);
        mess.put("CONTENT",textField.getText());
        textField.setText("");
        return mess;
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

}
