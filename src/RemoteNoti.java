import java.rmi.*;
import java.rmi.server.RemoteObject;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;

public class RemoteNoti extends RemoteObject implements RemoteNotiInterf{
	JTextArea notiList = null;
	DefaultListModel friendModel = null;
	String name= null;

	public RemoteNoti(JTextArea notiList,DefaultListModel friendModel,String name) throws RemoteException{
		super();
		this.notiList=notiList;
		this.friendModel=friendModel;
		this.name=name;
	}
	
	public void changedStatNotiON(String utente) throws RemoteException {
		this.notiList.append(utente+" è Online\n");
	}
	
	
	public void changedStatNotiOFF(String utente) throws RemoteException {
		this.notiList.append(utente+" è Offline\n");		

	}
	
	public String getName() throws RemoteException {
		return this.name;
	}
	
	public void newFriendNoti(String utente) throws RemoteException{
		this.notiList.append(utente+" è diventato tuo amico\n");
		//adesso si dovrebbe aggiungere nella lista degli amici
		this.friendModel.addElement(utente);
		
	}
}