import javax.swing.*;
import java.io.BufferedWriter;

public class ClientChatGroup {
    //variabili
    String username=""; //nome utente
    String friend=""; //nome amico con cui si sta chattando
    JFrame finestra = null;
    JTextArea chatArea;
    JTextField textField;
    private BufferedWriter writer;


    public ClientChatGroup(String username, String friend,BufferedWriter writer) {
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
      //  textField.addKeyListener(new ClientChat.KeyListener());
        finestra.add(textField);

        finestra.add(notiScrollPane);
        finestra.show();
    }




}
