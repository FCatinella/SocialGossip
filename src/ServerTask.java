import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.text.html.HTMLDocument;

//Task che il server deve eseguire
public class ServerTask implements Runnable{
	JSONObject rec;
	ConcurrentHashMap <String,User> tabellaUtenti;
    ConcurrentHashMap <String,UserGroup> tabellaGruppi;
	RMIServerImp RmiServer;
	BufferedWriter writer;
	MySocket mSock;
	Vector<Boolean> tO;
	Vector<Boolean> qO;

	//prende socket,tabella hash e oggetto rmi dal main
	public ServerTask(JSONObject jsonMess,MySocket msock, ConcurrentHashMap<String,User> tabellaUtentiArg, RMIServerImp RmiServer,ConcurrentHashMap<String,UserGroup> tabellaGruppi,Vector terzoOtt,Vector quartoOtt) {
		this.rec=jsonMess;
		this.tabellaGruppi=tabellaGruppi;
		this.tabellaUtenti=tabellaUtentiArg;
		this.RmiServer=RmiServer;
		this.mSock=msock;
		this.writer=msock.writer;
		tO=terzoOtt;
		qO=quartoOtt;
	}
	@Override
	public void run() {
		JSONParser parser = new JSONParser();
		//preparo la risposta di esito da mandare al client che ha richiesto l'operazionee
		JSONObject risp=new JSONObject();

		//estraggo le informazioni dal messaggio ricevuto
		String op = (String) rec.get("OP");
		String username= (String) rec.get("USERNAME");
		String password= (String) rec.get("PASSWORD");
		String language= (String) rec.get("LANGUAGE");

		Boolean result = true; //risultato della richiesta (true di default)
		Boolean quit = false; //devo uscire prima di inviare l'esito (no di default)

		//in base all'operazione richiesta, faccio cose diverse
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
							aux.updateSock(mSock.socket);
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

			//invio la lista degli amici
            case "LISTFRIENDS":{
                User aux = tabellaUtenti.get(username);
                risp.put("CONTENT",aux.getFriendList());
                }
                break;

		    //invio la lista dei gruppi
            case "LISTGROUPS":{
                User aux = tabellaUtenti.get(username);
                //nomi
                risp.put("CONTENT",aux.getGroupsList());
                //indirizzi multicast (ogni gruppo ne ha uno diverso)
                risp.put("CONTENT2",aux.getGroupsListAddr());
            }
            break;

			case "DISCONNECT":
				//disconnessione di un utente
				try {
				    //l'utente esiste lo disconetto
					if(username!=null && tabellaUtenti.containsKey(username)) {
						User aux = tabellaUtenti.get(username);
						//invio la notifica RMI
						RmiServer.goOffline(aux);
						aux.disconnect();
						}
				} catch (RemoteException e) {
					System.out.println("Invio notifica fallito (Disconnect)");
					e.printStackTrace();
				}
                try {
                    writer.close();
                    System.out.println("Task Terminato");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                quit=true; //non devo inviare l'esito
				break;

			case "ADDFRIEND":
			    //recupero il nome dal messaggio ricevuto
				String friendToAdd = (String) rec.get("FRIEND");
				//Cerco l'amico nella tabella
				if ( tabellaUtenti.containsKey(friendToAdd) ) {
					User friendSender = tabellaUtenti.get(username);
					User friend = tabellaUtenti.get(friendToAdd);
					risp.put("FRIEND",friendToAdd);
					//controllo che non stia provando a diventare amico di se stesso
					if (username.equals(friendToAdd)) {
						result=false;
						risp.put("ERRORMESS", "Non puoi diventare amico di te stesso.");
						break;
					}
					//controllo che non siano già amici
					if(friend.isFriend(friendSender)){
						result=false;
						risp.put("ERRORMESS", "Utente già amico o inesistente");
					}
					else {
					    //diventano amici (bilateralmente)
						friend.addFriend(friendSender);
						friendSender.addFriend(friend);
						try {
						    //invio notifica RMI
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

            case "CHATMESSAGE": {
                //recupero il ricevente
                String receiver = (String) rec.get("RECEIVER");
                User friend = tabellaUtenti.get(receiver);
                User sender = tabellaUtenti.get(username);
                String lang = sender.getLingua();
                //controllo che sia online
                if (friend.isOnline() == 1) {
                    rec.put("RESULT", result);
                    //estraggo il messaggio originale
                    String originalMess = (String) rec.get("CONTENT");
                    //prendo la lingua
                    String fLang = friend.getLingua();
                    String readMess;
                    String translateMess = originalMess;
                    //se i due utenti hanno lingue diverse
                    if (!fLang.equals(lang)) {
                        String encodedMess = null;
                        try {
                            //trasformo gli spazi in "%" in modo da potere usare la stringa nella richiesta REST
                            encodedMess = URLEncoder.encode(originalMess, "UTF-8");
                        } catch (UnsupportedEncodingException ignored) {
                            // UTF-8 è sempre supportato
                        }
                        try {
                            //faccio la richiesta REST
                            URL url = new URL("https://api.mymemory.translated.net/get?q=" + encodedMess + "&langpair=" + lang + "|" + fLang);
                            URLConnection uc = url.openConnection();
                            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                            StringBuffer sb = new StringBuffer();
                            while ((readMess = in.readLine()) != null) {
                                //scrivo il messaggio tradotto
                                sb.append(readMess);
                            }
                            System.out.println(sb.toString());
                            //traduco il messaggio in un oggetto JSON
                            JSONObject translatedJSONMess = (JSONObject) parser.parse(sb.toString());
                            //estraggo il messaggio tradotto
                            JSONObject aux = (JSONObject) translatedJSONMess.get("responseData");
                            translateMess = (String) aux.get("translatedText");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //invio il messaggio tradotto al destinatario e l'originale al mittente
                    risp = (JSONObject) rec.clone();
                    rec.put("CONTENT", translateMess);
                    sendToUser(rec, friend);
                } else {
                    result = false;
                    risp.put("ERRORMESS", receiver + " non è online");
                }
            }
            break;

            case "ADDGROUP": {
                String groupToAdd = (String) rec.get("GROUP");
                User user = tabellaUtenti.get(username);
                //controllo che il gruppo esista
                if (tabellaGruppi.containsKey(groupToAdd)) {
                    //controllo che l'utente non sia già nel gruppo
                    UserGroup gruppo = tabellaGruppi.get(groupToAdd);
                    if (!gruppo.isMember(username)) {
                        //lo aggiungo
                        gruppo.addMember(username);
                        //aggiungo il gruppo alla lista dell'utente
                        user.addGroup(groupToAdd);
                        user.addGroupAddr(gruppo.getIpAddr());
                        risp.put("GROUPADDR", gruppo.getIpAddr());
                        risp.put("GROUP",groupToAdd);
                    }
                    //l'utente è già membro
                    else {
                        result = false;
                        risp.put("ERRORMESS", "Sei già membro di questo gruppo");
                    }
                }
                //il gruppo non esiste
                else {
                    result=false;
                    risp.put("ERRORMESS", "Gruppo non esistente");
                }
            }
            break;

            case "DELETEGROUP":{
                //devo rimuovere il gruppo e avvisare tutti i membri di esso
                String groupToRem = (String) rec.get("GROUP");
                //recupero il gruppo
                UserGroup group = tabellaGruppi.get(groupToRem);
                if(group!=null){
                    //prendo l'indirizzo multicast
                    String gpAdd= group.getIpAddr();
                    try{
                        //invio il pacchetto in multicast
                        InetAddress ia = InetAddress.getByName(gpAdd);
                        byte [] data ;
                        data = rec.toJSONString().getBytes();
                        DatagramPacket dp = new DatagramPacket(data,data.length,ia,4999);
                        MulticastSocket ms = new MulticastSocket(4999);
                        System.out.println("Multicast: invio su "+gpAdd);
                        ms.send(dp);
                        ms.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    //reimposto gli indici degli ottetti
                    resetIndex(group.getIpAddr());
                    //elimino il gruppo da tutti gli utenti partecipanti
                    ArrayList<String> userList = group.getPartecipanti();
                    Iterator<String> iterator = userList.iterator();
                    while(iterator.hasNext()){
                        String userAux = iterator.next();
                        User gettedAux = tabellaUtenti.get(userAux);
                        gettedAux.removeGroup(groupToRem);
                        gettedAux.removeGroupAddr(group.getIpAddr());
                    }
                    tabellaGruppi.remove(groupToRem);
                }
                quit=true;
            }
            break;

			case "CREATEGROUP":{
                String groupToAdd = (String) rec.get("GROUP");
                User user = tabellaUtenti.get(username);
                //controllo che il gruppo non esista già
                if(tabellaGruppi.containsKey(groupToAdd)){
                    result = false;
                    risp.put("ERRORMESS", "Gruppo già esistente");
                    break;
                }
                //devo trovare un ip da assegnare al gruppo
                //trasformo il nome del gruppo in un indice
                int nameI = nameToIndex(groupToAdd);
                //trovo l'ip
                String ipChosen = findIp(nameI);
                if (ipChosen.equals("INDIRIZZO NON TROVATO")) {
                    //se sono finiti gli indirizzi multicast
                    result = false;
                    risp.put("ERRORMESS", "Numero di gruppi massimo raggiunto");
                } else {
                    //creo il gruppo assegnandogli l'indirizzo multicast
                    UserGroup aux = new UserGroup(ipChosen);
                    aux.addMember(username);
                    //inserisco il gruppo nella tabella dei gruppo
                    tabellaGruppi.put(groupToAdd, aux);
                    //inserisco l'utente che l'ha creato
                    user.addGroup(groupToAdd);
                    user.addGroupAddr(ipChosen);
                    risp.put("GROUPADDR", ipChosen);
                }
                op="ADDGROUP";
                risp.put("GROUP", groupToAdd);

			}
            break;

            case "GROUPMESSAGE":{
                String receiver = (String) rec.get("RECEIVER");
                UserGroup group = tabellaGruppi.get(receiver);
                if(group!=null){
                    String gpAdd= group.getIpAddr();
                    try{
                        //inoltro il messaggio a tutti i partecipanti
                        InetAddress ia = InetAddress.getByName(gpAdd);
                        byte [] data ;
                        data = rec.toJSONString().getBytes();
                        DatagramPacket dp = new DatagramPacket(data,data.length,ia,4999);
                        MulticastSocket ms = new MulticastSocket(4999);
                        System.out.println("Multicast: invio su "+gpAdd);
                        ms.send(dp);
                        ms.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                }
                //non voglio riscontro
                quit=true;

            }
            break;

            case "SENDFILE":{
                String receiver = (String) rec.get("FRIEND");
                User fR = tabellaUtenti.get(receiver);
                //l'utente ricevente deve essere online
                if(fR!=null && fR.isOnline()==1){
                    // avviso il client ricevente che sta per arrivare un file
                    JSONObject warnMess = new JSONObject();
                    warnMess.put("OP","RECEIVEFILE");
                    warnMess.put("RESULT",true);
                    warnMess.put("SENDER",username);
                    String filename=(String) rec.get("FILENAME");
                    warnMess.put("FILENAME",filename);
                    //invio l'avviso
                    sendToUser(warnMess,fR);
                    quit=true;
                }
                else{
                    risp.put("ERRORMESS","L'utente non è online");
                    result=false;
                }
            }
            break;

            case "RECEIVEFILE-ACK":{
                //inoltro il messaggio al mittente
                //dentro ci sono Ip e porta del destinatario
                risp=rec;
                risp.put("RESULT",true);
                String senderName=(String) rec.get("SENDER");
                User sender = tabellaUtenti.get(senderName);
                sendToUser(risp,sender);
                quit=true;
                break;

            }
            default:
			    quit=true;

		}
		//devo inviare un esito
		if(!quit){
			risp.put("OP",op);
			risp.put("RESULT", result);

			System.out.println("Invio esito al client"+risp.toString());
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

	}


	//funzione che invia il messaggio ad un'altro utente diverso da quello che ha richiesto un'operazione
	private void sendToUser(JSONObject mess,User friend){
        MySocket friendSock = friend.getUserSock();
        BufferedWriter fWriter = friendSock.writer;
        //qui traduco il messaggio
        try {
            fWriter.write(mess.toJSONString());
            fWriter.newLine();
            fWriter.flush();

        } catch (IOException e) {
            //socket chiuso
            System.out.println("Richiesta la chiusura ma server non raggiungibile");
        }

    }



    //converto il nome in un indice per gli array
    int nameToIndex(String nome){
	    int indice=0;
	    for (int i=0; i<nome.length();i++){
            char character = nome.charAt(0);
            int ascii = (int) character;
            indice+=ascii;
        }
	    return indice%256;
    }

    private String findIp(int index){
	    Boolean quit=false;
        int ind3 = (index/256)%256;
        int ind4 = index%256;
	    while(!quit){
            ind3 = (index/256)%256;
            ind4 = index%256;
	        if(tO.get(ind3) && qO.get(ind4)){
	            //entrambi gli indici devono essere liberi
	            quit=true;
	            tO.set(ind3,false);
	            qO.set(ind4,false);
            }
            //ho guardato tutti gli indirizzi possibili
	        if(ind3==255 && ind4==255){
	            return ("INDIRIZZO NON TROVATO");
            }
	        index++;
        }
        System.out.println("Trovato: "+"239.1."+ind3+"."+ind4);
        return ("239.1."+ind3+"."+ind4);
    }

    //ripristino gli indirizzi usati
    void resetIndex(String ipAddr){
        String sub = ipAddr.substring(6);
        int pointIdx= sub.lastIndexOf(".");
        //estragggo i due indirizzi
        int ind3 = Integer.parseInt(sub.substring(0,pointIdx));
        int ind4 = Integer.parseInt(sub.substring(pointIdx+1));
        tO.set(ind3,true);
        qO.set(ind4,true);
    }

}
