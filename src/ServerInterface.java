import java.rmi.*;
//interfaccia server remoto
public interface ServerInterface extends Remote{
	/* registrazione per la callback */
	void registerForCallback (RemoteNotiInterf ClientInterface) throws RemoteException;
	/* cancella registrazione per la callback */
	void unregisterForCallback (RemoteNotiInterf ClientInterface) throws RemoteException;
}