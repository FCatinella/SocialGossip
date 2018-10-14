import java.rmi.Remote;
import java.rmi.RemoteException;

//interfaccia oggetto remoto che aggiornerà l'interfaccia
public interface RemoteNotiInterf extends Remote {

	//visualizza la notifica dell'utente connesso
	void changedStatNotiON(String utente) throws RemoteException;
    //visualizza la notifica dell'utente disconnesso
    void changedStatNotiOFF(String utente) throws RemoteException;
    //notifica l'aggiunta di un nuovo amico
    void newFriendNoti(String utente) throws RemoteException;
    //recupero il nome di chi ha creato l'oggetto
    String getName()throws RemoteException;
	
}
