import java.rmi.*;
public interface ServerInterface extends Remote{
	/* registrazione per la callback */
	public void registerForCallback (RemoteNotiInterf ClientInterface) throws RemoteException;
	/* cancella registrazione per la callback */
	public void unregisterForCallback (RemoteNotiInterf ClientInterface) throws RemoteException;
}