import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNotiInterf extends Remote {
	//funzione da chiamare quando un utente si connette
	public void changedStatNotiON(String utente) throws RemoteException;
	public void changedStatNotiOFF(String utente) throws RemoteException;
	public void newFriendNoti(String utente) throws RemoteException;
	public String getName()throws RemoteException;
	
}
