import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;

public class ClientMain extends JFrame implements ActionListener{
	
	private Socket socket;
	
	
	//variabili
	private InetAddress local=null;
	private BufferedWriter writer = null;
	private BufferedReader reader = null;
	private String rec = null;
	private JSONObject recJSON=null;
	private JFrame finestra = null;
	private String lang= "it";
	
	//componenti dell'interfaccia
	private JLabel usernameLabel;
	private JTextField usernameArea;
	private JPasswordField passArea;
	//lingue disponibili
	private String[] langStrings = { "Italiano", "Francaise", "English", "Espanol", "Deutsch"};
	private JComboBox langList = new JComboBox(langStrings);
	
	private ClientMain()  {
		//Interfaccia grafica
		
		//creo la finestra
		int windwX=350;
		int windwY=500;
		finestra = new JFrame ("Benvenuto su Social Gossip");
		finestra.setSize(windwX,windwY);
		finestra.setLocation(100, 100);
		finestra.setLayout(null);
		
		//titolo
		JLabel titolo = new JLabel("Social Gossip",JLabel.CENTER);
		titolo.setFont(new Font ("Arial",Font.PLAIN,20));
		titolo.setBounds((windwX-200)/2,0,200,50);
		
		//label Username
		usernameLabel = new JLabel("Username",JLabel.CENTER);
		usernameLabel.setFont(new Font ("Arial",Font.PLAIN,13));
		usernameLabel.setBounds((windwX-200)/2,50,200,50);
		//TextField (Username)
		usernameArea = new JTextField("",30);
		usernameArea.setEditable(true);
		usernameArea.setBounds((windwX-200)/2,90,200,20);
		
		//label Password + PasswordField (sottoclasse di JFieldText)
		JLabel passLabel = new JLabel("Password",JLabel.CENTER);
		passLabel.setFont(new Font ("Arial",Font.PLAIN,13));
		passLabel.setBounds((windwX-200)/2,120,200,50);
		passArea = new JPasswordField("",30);
		passArea.setEditable(true);
		passArea.setBounds((windwX-200)/2,160,200,20);
		
		//Pulsanti
		JButton loginButt = new JButton("Login");
		loginButt.setBounds(75, 200, 80, 30);
		loginButt.addActionListener(this);
		JButton regisButt = new JButton("Registrati");
		regisButt.setBounds(windwX-185, 200, 110, 30);
		regisButt.addActionListener(this);

		//Label lingua+Lista lingue
		JLabel langLabel = new JLabel ("Lingua");
		langLabel.setBounds(100, 398, 90, 30);		
		langList.setBounds(180, 400, 90, 30);
		langList.addActionListener(this);
		
		
		
		//aggiungo il tutto alla finestra
		finestra.add(titolo);
		finestra.add(usernameLabel);
		finestra.add(usernameArea);
		finestra.add(passLabel);
		finestra.add(passArea);
		finestra.add(loginButt);
		finestra.add(regisButt);
		finestra.add(langList);
		finestra.add(langLabel);
		finestra.setResizable(false); //non si può ridimensionare
		finestra.setVisible(true);
	}
	
	
	public static void main (String args[]){
		ClientMain client = new ClientMain();
		//controllo che il server sia raggiungibile
		if(!client.connectToServer()) {
			System.out.println("Server non raggiungibile");
			//termino se non lo è
			System.exit(ABORT);
		}
		//aggiungo il listener del pulsante chiudi della finestra
		client.finestra.addWindowListener(new WindowAdapter( ){
			public void windowClosing(WindowEvent event) {
				System.out.println("Chiusa");
				//chiudo il socket
				try {
					//si disconnette dal server (invia una DISCONNECT)
					client.disconnectFromServer();
					client.writer.close();
					System.exit(NORMAL);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

	}
	
	
	@Override
	//Listener dei pulsanti
	public void actionPerformed(ActionEvent evt)  {
		//controllo quale tasto ha chiamato l'evento
		String pressed = evt.getActionCommand();
		//box della lingua
		if (pressed.equals("comboBoxChanged")) {
			//in base alla lingua scelta setto il parametro lingua da inviare durante la CONNECT
			String selectedLang = langList.getSelectedItem().toString();
			switch (selectedLang) {
				case ("Italiano"):
					lang="it";
					break;
				case ("English"):
					lang="en";
					break;
				case ("Francaise"):
					lang="fr";
					break;
				case ("Espanol"):
					lang="es";
					break;
				case ("Deutsch"):
					lang="de";
					break;
					
			}
			return;
		}
		//preparo la richiesta da inviare al server
		JSONObject mess = new JSONObject();
		// inserisco l'operazione da richiedere in base a cosa è stato premuto
		switch(pressed) {
			case "Registrati":
				mess.put("OP","REGISTER");
				break;
			case "Login":
				mess.put("OP","CONNECT");
				break;
		}
		
		//in ogni caso devo inserire questi dati
		String userDigit=usernameArea.getText();
		String passDigit=String.valueOf(passArea.getPassword());
		mess.put("USERNAME",userDigit);
		mess.put("PASSWORD",passDigit);
		mess.put("LANGUAGE",lang);
		//invio il messaggio
		try {
			writer.write(mess.toJSONString());
			writer.newLine();
			writer.flush();
		    //aspetto la risposta
			rec = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		JSONParser parser = new JSONParser();
		try {
			//parso la stringa che ho ricevuto 
			recJSON = (JSONObject) parser.parse(rec);
		} catch (ParseException e1) {
			// Non è arrivata una stringa in formato JSON
			e1.printStackTrace();
		}
		
		//controllo il risultato della richiesta fatta, recupero risultato dell'operazione e
		// messaggio di errore da visualizzare
		Boolean result = (Boolean) recJSON.get("RESULT");
		String errorMess = (String) recJSON.get("ERRORMESS");
		// e nel caso visualizzo una finestra di errore
		if(!result) {
			JFrame infoWind = new JFrame();
			infoWind.setSize(220,150);
			infoWind.setLocation(150, 150);
			infoWind.setLayout(null);
			JLabel errorMessage = new JLabel (errorMess);
			errorMessage.setBounds(20, 40, 180, 30);
			infoWind.add(errorMessage);
			infoWind.setResizable(false);
			infoWind.setVisible(true);
		}
		else {
			//tutto bene -> result == true
			String resultOp = (String) recJSON.get("OP");
			switch (resultOp) {
				case "REGISTER":
					//visualizzo messaggio di avvenuta registrazione
					JFrame infoWind = new JFrame();
					infoWind.setSize(220,150);
					infoWind.setLocation(150, 150);
					infoWind.setLayout(null);
					JLabel errorMessage = new JLabel (errorMess);
					errorMessage.setBounds(20, 40, 180, 30);
					infoWind.add(errorMessage);
					infoWind.setResizable(false);
					infoWind.setVisible(true);
					break;
				case "CONNECT":
					//partenza di tutto il client (gli passo il nome che l'utente ha digitato)
					Client cm = new Client(socket,userDigit);
					//chiudo la finestra di login
					finestra.setVisible(false);
					cm.run();
			}
		}
		
		
	}


	//Funzione per connettersi al server
	private Boolean connectToServer() {
		try {
			//Ottengo l'indirizzo IP (localhost al momento)
			local = InetAddress.getByName("127.0.0.1");
		} 
		catch (UnknownHostException e) {
			//Indirizzo sconosciuto
			e.printStackTrace();
			return false;
		}
		
		//Mi connetto sulla porta di benvenuto e ottengo un socket su cui continuare la connessione
		try {
			socket = new Socket(local,1994);
			//recupero i due buffer: writer e reader dove il socket legge e scrive
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader= new BufferedReader(new InputStreamReader (socket.getInputStream()));
		} 
		catch (IOException e) {
			//qualcosa va storto
			e.printStackTrace();
			return false;
		}
		return true;
	}
		

	//funzione per disconnettersi dal server
	private void disconnectFromServer() {
		//creo un messaggio di disconnect e lo invio
		JSONObject mess = new JSONObject();
		mess.put("OP","DISCONNECT");
		try {
			writer.write(mess.toJSONString());
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
