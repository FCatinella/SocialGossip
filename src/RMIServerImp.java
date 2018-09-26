import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServerImp extends RemoteObject implements ServerInterface{
	private List<RemoteNotiInterf> clients;
	private ConcurrentHashMap <String,User> tabellaUtenti;
	
	public RMIServerImp(ConcurrentHashMap<String,User> tabellaUtenti)throws RemoteException{
		super();
		//devo usare l'iterfaccia come tipo perchè il server non conosce l'ogetto vero e proprio
		clients = new ArrayList<RemoteNotiInterf>();
		this.tabellaUtenti=tabellaUtenti;
	}
	
	
	public void registerForCallback(RemoteNotiInterf ClientInterface) throws RemoteException {
		// TODO Auto-generated method stub
		if(!clients.contains(ClientInterface)) {
			clients.add(ClientInterface);
			System.out.println("Utente CallBack registrata");
		}
		
	}

	
	public void unregisterForCallback(RemoteNotiInterf ClientInterface) throws RemoteException {
		// TODO Auto-generated method stub
		if(clients.contains(ClientInterface)) {
			clients.remove(ClientInterface);
			System.out.println("Utente CallBack deregistrata");
		}
	}
	
	public void update(User sender,int stat) throws RemoteException{
		System.out.println("Inzio a mandare le notifiche");
		Iterator i = clients.iterator();
		String name = sender.Username;
		while(i.hasNext()) {
			RemoteNotiInterf client = (RemoteNotiInterf) i.next();
			User receiver = tabellaUtenti.get(client.getName());
			if(stat==3 && !(receiver.isFriend(sender))) {
				client.newFriendNoti(name);
				break;
			}
			if(receiver.isFriend(sender)) {
				if(stat==1) client.changedStatNotiON(name);
				if(stat==0) client.changedStatNotiOFF(name);
			}
		}
		System.out.println("Notifiche finite");
	}

}
