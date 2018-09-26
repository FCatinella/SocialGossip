import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
public class ServerThread implements Runnable{
	Socket sock;
	ConcurrentHashMap <String,User> tabellaUtenti;
	RMIServerImp RmiServer;
	
	public ServerThread(Socket sock,ConcurrentHashMap<String,User> tabellaUtentiArg, RMIServerImp RmiServer) {
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
		while(!quit) {
			try {
				prova = reader.readLine();
				System.out.println("Richiesta letta");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
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
			String op = (String) rec.get("OP");
			String username= (String) rec.get("USERNAME");
			String password= (String) rec.get("PASSWORD");
			String language= (String) rec.get("LANGUAGE");
			Boolean result = true;
			System.out.println(rec);
			switch(op) {
				case "REGISTER":
					//utente nuovo (non presente nella hash)
					if(!tabellaUtenti.containsKey(username)) {
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
					if(tabellaUtenti.containsKey(username)) {
						User aux = tabellaUtenti.get(username);
						if(aux.checkPass(password)) {
							aux.connect();
							try {
								RmiServer.update(aux,1);
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
					try {
						if(username!=null && tabellaUtenti.containsKey(username)) {
							User aux = tabellaUtenti.get(username);
							RmiServer.update(aux,0);
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
								RmiServer.update(friendSender,3);
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
			
			try {
				risp.writeJSONString(writer);
				writer.newLine(); 
				writer.flush();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			writer.close();
			System.out.println("Thread Terminato");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
