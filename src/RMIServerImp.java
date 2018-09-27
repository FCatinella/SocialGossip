import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//ogetto remoto usato dal server per inviare callbacks ai client
public class RMIServerImp extends RemoteObject implements ServerInterface{
	private List<RemoteNotiInterf> clients;
	private ConcurrentHashMap <String,User> tabellaUtenti;
	
	public RMIServerImp(ConcurrentHashMap<String,User> tabellaUtenti){
		super();
		//devo usare l'iterfaccia come tipo perchè il server non conosce l'ogetto vero e proprio
		clients = new ArrayList<RemoteNotiInterf>();
		this.tabellaUtenti=tabellaUtenti;
	}
	
	//"registro" i client per le callback
	public void registerForCallback(RemoteNotiInterf client) {
		if(!clients.contains(client)) {
			clients.add(client);
			System.out.println("Utente CallBack registrata");
		}
		
	}

	//"deregistro" un client
	public void unregisterForCallback(RemoteNotiInterf ClientInterface){
		// TODO Auto-generated method stub
		if(clients.contains(ClientInterface)) {
			clients.remove(ClientInterface);
			System.out.println("Utente CallBack deregistrata");
		}
	}

    //informa gli amici che un utente si sta disconnettendo
	public void goOffline(User sender) throws RemoteException{
	    System.out.println(sender.getUsername()+" si sta disconnettendo");
        Iterator i = clients.iterator();
        String senderName = sender.getUsername();
        while(i.hasNext()){
            RemoteNotiInterf client = (RemoteNotiInterf) i.next();
            User clientUser = tabellaUtenti.get(client.getName());
            if(clientUser.isFriend(sender)) client.changedStatNotiOFF(senderName);
        }

    }

    //informa gli amici che un utente si sta connettendo
    public void goOnline(User sender) throws RemoteException{
        System.out.println(sender.getUsername()+" si sta connettendo");
        Iterator i = clients.iterator();
        String senderName = sender.getUsername();
        while(i.hasNext()){
            RemoteNotiInterf client = (RemoteNotiInterf) i.next();
            User clientUser = tabellaUtenti.get(client.getName());
            if(clientUser.isFriend(sender)) client.changedStatNotiON(senderName);
        }

    }

    //informa un utente che sender è diventato suo amico
	public void addFriendNoti(User sender,User toAdd) throws RemoteException{
		Iterator i = clients.iterator();
		String senderName = sender.getUsername();
		String wantedName = toAdd.getUsername();
		//scorre i client e quando trova quello giusto lo informa
		while(i.hasNext()) {
			RemoteNotiInterf client = (RemoteNotiInterf) i.next();
			String clientName = client.getName();
			if(wantedName.equals(clientName)) {
				client.newFriendNoti(senderName);
				break;
			}
		}
	}

}
