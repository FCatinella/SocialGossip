import java.rmi.*;
import java.rmi.server.RemoteObject;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;

//RMI
// Oggetto remoto che aggiornerà le componenti grafiche
public class RemoteNoti extends RemoteObject implements RemoteNotiInterf{

    //variabili globali
	private JTextArea notiList;
	private DefaultListModel friendModel;
	private String name;

	public RemoteNoti(JTextArea notiList,DefaultListModel friendModel,String name) throws RemoteException{
		super();
		this.notiList=notiList;
		this.friendModel=friendModel;
		this.name=name;
	}

	//visualizza la notifica dell'utente connesso
	public void changedStatNotiON(String utente) throws RemoteException {
		this.notiList.append(utente+" è Online\n");
	}

    //visualizza la notifica dell'utente disconnesso
    public void changedStatNotiOFF(String utente) throws RemoteException {
		this.notiList.append(utente+" è Offline\n");		

	}

	//recupero il nome di chi ha creato l'oggetto
	public String getName() throws RemoteException {
		return this.name;
	}

	//notifica l'aggiunta di un nuovo amico
	public void newFriendNoti(String utente) throws RemoteException{
		this.notiList.append(utente+" è diventato tuo amico\n");
		//adesso si dovrebbe aggiungere nella lista degli amici
		this.friendModel.addElement(utente);
		
	}
}