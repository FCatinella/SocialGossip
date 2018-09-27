import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//Task che il server deve eseguire
public class ServerTask implements Runnable{
	Socket sock;
	ConcurrentHashMap <String,User> tabellaUtenti;
	RMIServerImp RmiServer;

	//prende socket,tabella hash e oggetto rmi dal main
	public ServerTask(Socket sock, ConcurrentHashMap<String,User> tabellaUtentiArg, RMIServerImp RmiServer) {
		this.sock=sock;
		this.tabellaUtenti=tabellaUtentiArg;
		this.RmiServer=RmiServer;
	}
	@Override
	public void run() {
		JSONParser parser = new JSONParser();
		//si attacca alla porta che verrà usata per la comunicazione
		InputStream in = null;
		OutputStream out= null;
		try {
			in = sock.getInputStream();
			out = sock.getOutputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//connesione riuscita
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		String prova =null;
		System.out.println("Sono connesso alla porta "+sock.getPort());
		Boolean quit = false;
		//legge le richieste dei client
		while(!quit) {
			try {
				prova = reader.readLine();
				System.out.println("Richiesta letta");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//converto la stringa in un oggetto JSON
			JSONObject rec=new JSONObject();
			JSONObject risp=new JSONObject();
			try {
				rec=(JSONObject) parser.parse(prova);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			//estraggo le informazioni dal messaggio ricevuto
			String op = (String) rec.get("OP");
			String username= (String) rec.get("USERNAME");
			String password= (String) rec.get("PASSWORD");
			String language= (String) rec.get("LANGUAGE");
			Boolean result = true;
			System.out.println("Ricevuto: "+rec);
			//in base all'operazione richiesta, fa cose diverse
			switch(op) {
				case "REGISTER":
					//utente nuovo (non presente nella hash)
					if(!tabellaUtenti.containsKey(username)) {
						//aggiungo l'utente alla tabella ( con tanto di lingua )
						User nuovo = new User(username,password,language);
						tabellaUtenti.put(username, nuovo);
						risp.put("ERRORMESS", username+" registrato!");
					}
					else{
						System.out.println("Utente già presente");
						risp.put("ERRORMESS", "Utente già presente");
						result=false;
					}
					break;
					
				case "CONNECT":
					//connessione di utente già registrato
					if(tabellaUtenti.containsKey(username)) {
						User aux = tabellaUtenti.get(username);
						//controllo la password
						if(aux.checkPass(password)) {
							//non ci posso essere più client per un utente
							if(aux.isOnline()==0){
								aux.connect();
							}
							else {
								risp.put("ERRORMESS", "Utente già connesso");
								result= false;
								break;
							}

							//RMI
							try {
								//invio la notifica di connessione agli amici
								RmiServer.goOnline(aux);
							} catch (RemoteException e) {
								System.out.println("Invio notifica fallito (Connect)");
							}
						}
						else {
							System.out.println("Password errata");
							risp.put("ERRORMESS", "Utente o Password Errati");
							result= false;
						}
					}
					else {
						System.out.println("Utente non esistente");
						risp.put("ERRORMESS", "Utente o Password Errati");
						result= false;
					}
					break;


				case "DISCONNECT":
					//disconnessione di un utente
					try {
						if(username!=null && tabellaUtenti.containsKey(username)) {
							User aux = tabellaUtenti.get(username);
							RmiServer.goOffline(aux);
							aux.disconnect();
							}
					} catch (RemoteException e) {
						System.out.println("Invio notifica fallito (Disconnect)");
						e.printStackTrace();
					}
					quit=true;
					break;
					
				case "ADDFRIEND":
					/* Cerco l'amico nella tabella
					 * se non sono già amici
					 * Aggiungo la connessione in entrambi i lati
					 * Avverto il mittente della riuscita operazione
					 * Avverto il ricevente della aggiunta
					 * */
					String friendToAdd = (String) rec.get("FRIEND");
					if ( tabellaUtenti.containsKey(friendToAdd) ) {
						User friendSender = tabellaUtenti.get(username);
						User friend = tabellaUtenti.get(friendToAdd);
						if (username.equals(friendToAdd)) {
							result=false;
							risp.put("ERRORMESS", "Non puoi diventare amico di te stesso.");
							break;
						}
						if(friend.isFriend(friendSender)){
							result=false;
							risp.put("ERRORMESS", "Utente già amico o inesistente");
						}
						else {
							friend.addFriend(friendSender);
							friendSender.addFriend(friend);
							try {
								RmiServer.addFriendNoti(friendSender,friend);
							}
							catch (Exception e) {}
						}
					}
					//l'amico non esiste
					else {
						result=false;
						risp.put("ERRORMESS", "Utente già amico o inesistente");
					}
					break;
					
			}
			if(quit) break;
			risp.put("OP",op);
			risp.put("RESULT", result);

			//invio l'esito
			try {
				risp.writeJSONString(writer);
				writer.newLine(); 
				writer.flush();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		//termino il task
		try {
			writer.close();
			System.out.println("Task Terminato");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
