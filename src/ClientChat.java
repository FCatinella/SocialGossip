import javax.swing.*;
public class ClientChat {
    //variabili
    String username=""; //nome utente
    String friend=""; //nome amico con cui si sta chattando
    JFrame finestra = null;
    JTextArea chatArea;
    JTextField textField;

    public ClientChat(String username, String friend){
        this.username=username;
        this.friend=friend;
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
        textField.setBounds(100,410,399,40);
        finestra.add(textField);

        finestra.add(notiScrollPane);
        finestra.show();
    }

}
