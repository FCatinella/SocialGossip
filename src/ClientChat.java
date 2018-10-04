import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;

public class ClientChat implements ActionListener{
    //variabili
    String username=""; //nome utente
    String friend=""; //nome amico con cui si sta chattando
    JFrame finestra = null;
    JTextArea chatArea;
    JTextField textField;
    private BufferedWriter writer;


    public ClientChat(String username, String friend,BufferedWriter writer){
        this.username=username;
        this.friend=friend;
        this.writer=writer;
        createWindow();

    }

    private void createWindow(){
        finestra = new JFrame (username+": Chat con " +friend+" - Social Gossip");
        finestra.setSize(370,500);
        finestra.setLayout(null);
        finestra.setResizable(false);

        chatArea = new JTextArea();
        chatArea.setSize(369,399);
        JScrollPane notiScrollPane= new JScrollPane(chatArea);
        notiScrollPane.setBounds(1,1,369,399);
        chatArea.setEditable(false);

        textField= new JTextField();
        textField.setBounds(100,410,259,40);
        textField.addKeyListener(new KeyListener());
        finestra.add(textField);

        finestra.add(notiScrollPane);
        finestra.show();
    }

    @Override
    public void actionPerformed(ActionEvent e){
    }

    class KeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            if(e.getKeyCode()==10){
                System.out.println("Invio il messaggio");
                JSONObject mess = createMess();
                sendToServer(mess);
            }
        }
    }


    private JSONObject createMess(){
        JSONObject mess = new JSONObject();
        mess.put("OP","CHATMESSAGE");
        mess.put("USERNAME",username);
        mess.put("RECEIVER",friend);
        mess.put("CONTENT",textField.getText());
        textField.setText("");
        return mess;
    }

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
