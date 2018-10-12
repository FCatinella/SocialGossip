import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.print.DocFlavor;

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
		//legge le richieste dei client

		//converto la stringa in un oggetto JSON
		JSONObject risp=new JSONObject();

		//estraggo le informazioni dal messaggio ricevuto
		String op = (String) rec.get("OP");
		String username= (String) rec.get("USERNAME");
		String password= (String) rec.get("PASSWORD");
		String language= (String) rec.get("LANGUAGE");
		Boolean result = true;
		Boolean quit = false;
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
							aux.updateSock(mSock.socket);
							String ip = (String) rec.get("IP");
							aux.updateIp(ip);
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

            case "LISTFRIENDS":{
                User aux = tabellaUtenti.get(username);
                risp.put("CONTENT",aux.getFriendList());
                }
                break;

            case "LISTGROUPS":{
                User aux = tabellaUtenti.get(username);
                risp.put("CONTENT",aux.getGroupsList());
                risp.put("CONTENT2",aux.getGroupsListAddr());
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
                try {
                    writer.close();
                    System.out.println("Task Terminato");
                } catch (IOException e) {
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
					risp.put("FRIEND",friendToAdd);
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

            case "CHATMESSAGE": {
                String receiver = (String) rec.get("RECEIVER");
                User friend = tabellaUtenti.get(receiver);
                User sender = tabellaUtenti.get(username);
                String lang = sender.getLingua();
                if (friend.isOnline() == 1) {
                    rec.put("RESULT", result);
                    String originalMess = (String) rec.get("CONTENT");
                    String fLang = friend.getLingua();
                    String readMess = null;
                    String translateMess = originalMess;
                    if (!fLang.equals(lang)) {
                        String encodedMess = null;
                        try {
                            encodedMess = URLEncoder.encode(originalMess, "UTF-8");
                        } catch (UnsupportedEncodingException ignored) {
                            // Can be safely ignored because UTF-8 is always supported
                        }
                        try {
                            URL url = new URL("https://api.mymemory.translated.net/get?q=" + encodedMess + "&langpair=" + lang + "|" + fLang);
                            URLConnection uc = url.openConnection();
                            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                            StringBuffer sb = new StringBuffer();
                            while ((readMess = in.readLine()) != null) {
                                sb.append(readMess);
                            }
                            System.out.println(sb.toString());
                            JSONObject translatedJSONMess = (JSONObject) parser.parse(sb.toString());
                            JSONObject aux = (JSONObject) translatedJSONMess.get("responseData");
                            translateMess = (String) aux.get("translatedText");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
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
                    if (!gruppo.isMember(user)) {
                        //lo aggiungo
                        gruppo.addMember(user);
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
                UserGroup group = tabellaGruppi.get(groupToRem);
                if(group!=null){
                    String gpAdd= group.getIpAddr();
                    try{
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
                    resetIndex(group.getIpAddr());
                    User user = tabellaUtenti.get(username);
                    user.removeGroup(groupToRem);
                    user.removeGroupAddr(group.getIpAddr());
                    tabellaGruppi.remove(groupToRem);
                }
            }
            break;
			case "CREATEGROUP":{
                String groupToAdd = (String) rec.get("GROUP");
                User user = tabellaUtenti.get(username);
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
                    UserGroup aux = new UserGroup(ipChosen, 4999);
                    aux.addMember(user);
                    tabellaGruppi.put(groupToAdd, aux);
                    user.addGroup(groupToAdd);
                    user.addGroupAddr(ipChosen);
                    risp.put("GROUPADDR", ipChosen);
                }
                op="ADDGROUP";
                risp.put("GROUP", groupToAdd);

			}
            break;
            case "GROUPMESSAGE":{
            	//
                String receiver = (String) rec.get("RECEIVER");
                UserGroup group = tabellaGruppi.get(receiver);
                if(group!=null){
                    String gpAdd= group.getIpAddr();try{
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

            }
            break;
            case "SENDFILE":{
                String receiver = (String) rec.get("FRIEND");
                User fR = tabellaUtenti.get(receiver);
                if(fR!=null && fR.isOnline()==1){
                    // avvisare il client ricevente che sta per arrivare un file
                    JSONObject warnMess = new JSONObject();
                    warnMess.put("OP","RECEIVEFILE");
                    warnMess.put("RESULT",true);
                    warnMess.put("SENDER",username);
                    String filename=(String) rec.get("FILENAME");
                    warnMess.put("FILENAME",filename);
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
            char character = nome.charAt(0); // This gives the character 'a'
            int ascii = (int) character;
            indice+=ascii;
        }
	    return indice%256;
    }

    String findIp(int index){
	    Boolean quit=false;
        int ind3 = (index/256)%256;
        int ind4 = index%256;
	    while(!quit){
            ind3 = (index/256)%256;
            ind4 = index%256;
	        if(tO.get(ind3) && qO.get(ind4)){
	            quit=true;
	            tO.set(ind3,false);
	            qO.set(ind4,false);
            }
	        if(ind3==255 && ind4==255){
	            return ("INDIRIZZO NON TROVATO");
            }
	        index++;
        }
        System.out.println("Trovato: "+"239.1."+ind3+"."+ind4);
        return ("239.1."+ind3+"."+ind4);
    }

    void resetIndex(String ipAddr){
        String sub = ipAddr.substring(6);
        int pointIdx= sub.lastIndexOf(".");
        int ind3 = Integer.parseInt(sub.substring(0,pointIdx));
        int ind4 = Integer.parseInt(sub.substring(pointIdx+1));
        tO.set(ind3,true);
        qO.set(ind4,true);
    }

}
