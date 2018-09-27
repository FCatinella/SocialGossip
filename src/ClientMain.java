import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javax.swing.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientMain implements Runnable,ActionListener{

	//elementi interfaccia
	private JFrame finestra = null; //finestra del client
    private JTextArea notiList = null; // lista degli amici (UI)
    private JFrame addWind = null; //finestra aggiunta amico o gruppo
    private DefaultListModel friendModel = null;
    private JTextArea addArea = null;


    //variabili globali
    private String username;
    private Socket sock;
	private BufferedWriter writer;
	private BufferedReader reader;


	//variabili per RMI
	private ServerInterface server =null;
	private RemoteNotiInterf stub = null;
	

	//costruttore
	public  ClientMain(Socket sock,String username) {
		this.username=username;
		this.sock=sock;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(this.sock.getOutputStream()));
			reader= new BufferedReader(new InputStreamReader (this.sock.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	private void createWind() {
		//creo la finestra della chat
		int windwX=550;
		int windwY=500;
		finestra = new JFrame (username+" - Social Gossip");
		finestra.setSize(windwX,windwY);
		finestra.setLocation(400, 100);
		finestra.setLayout(null);
		
		//nome utente
		JLabel userTitolo = new JLabel(username);
		userTitolo.setFont(new Font ("Arial",Font.BOLD,17));
		userTitolo.setBounds(50,400,200,50);
		finestra.add(userTitolo);
		
		//lista amici
		JLabel friendTitolo = new JLabel("Amici");
		friendTitolo.setFont(new Font ("Arial",Font.PLAIN,14));
		friendTitolo.setBounds(50,10,200,50);
		finestra.add(friendTitolo);
		friendModel= new DefaultListModel();
		JList friendList = new JList(friendModel);
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
		DefaultListModel groupModel = new DefaultListModel();
		JList groupList = new JList(groupModel);
		groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupList.setLayoutOrientation(JList.VERTICAL_WRAP);
		JScrollPane groupScrollPane= new JScrollPane(groupList);
		groupScrollPane.setBounds(300,50,200,150);
		finestra.add(groupScrollPane);
		
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
	
	
	
	

	@Override
	public void run() {
		//creo la finestra
		createWind();
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
			System.out.println("Parte RMI Client");
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
	}

	
	//LISTENER PULSANTI
	@Override
	public void actionPerformed(ActionEvent e) {
		String pressed = e.getActionCommand();
		System.out.println(pressed);
		switch(pressed) {
			case "Esci":
				//Richiede la disconnessione dal server e chiude
                try {
                    server.unregisterForCallback(stub);
                } catch (RemoteException e1) {
                    // TODO Auto-generated catch block
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
				addWind.setLocation(500, 200);
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
				
			case "Aggiungi amico":
				String friendToAdd = addArea.getText();
				JSONObject mess = new JSONObject();
				mess.put("OP","ADDFRIEND");
				mess.put("USERNAME",username);
				mess.put("FRIEND", friendToAdd);
				//invio richiesta
				sendToServer(mess);
				//aspetto la richiesta e se è andata a buon fine visualizzo l'amico nuovo nella lista degli amici
				if(afterSend(friendToAdd)) {
					addWind.setVisible(false);
				}
				System.out.println(mess);
				break;	
		
		}
		
	}
	
	
	//Funzione che aspetta la risposta del server dopo aver fatto una richiesta
	private boolean afterSend(String receiver) {
		Boolean result = true;
		try {
		    //legge il messaggio
			String rec =reader.readLine();
			JSONParser parser = new JSONParser();
			JSONObject recJSON = (JSONObject) parser.parse(rec);
			//recupera l'esito della richiesta
			result = (Boolean) recJSON.get("RESULT");
			String errorMess = (String) recJSON.get("ERRORMESS");
			if(!result) {
			    //visualizza una finestra di errore
				visualizeError(errorMess);
			}
			else {
			    // se è andato tutto liscio
				String resultOp = (String) recJSON.get("OP");
				//esegue comandi in base all'operazione richiesta in origine
                switch (resultOp) {
                    case "ADDFRIEND":
                        //aggiungo l'amico alla lista degli amici
                        friendModel.addElement(receiver);
                }
			}
		}
		catch (Exception e ) {
			e.getStackTrace();
		}
		return result;
		
	}

	//invia messaggio al server
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
	
	//funzione per disconnettersi
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
