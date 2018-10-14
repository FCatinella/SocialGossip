import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;


public class ServerMain  {

	public static void main(String[] args) throws IOException {

	    //visualizzo una finestra che avvisa che il server è partito
        JFrame isRunningFrame = new JFrame("Social Gossip - Server");
        isRunningFrame.setResizable(false);
        //quando chiuso la finestra interrompo il server
        isRunningFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(0);
            }
        });
        isRunningFrame.setSize(400,100);
        JLabel jl = new JLabel("Server in esecuzione su: "+InetAddress.getLocalHost().getHostAddress());
        jl.setHorizontalAlignment(0);
        isRunningFrame.add(jl);
        isRunningFrame.setAlwaysOnTop(true);
        isRunningFrame.show();
		System.out.println("Server Partito!");

		//creo la tabella degli utenti e dei gruppi registrati al servizio (concorrente)
		ConcurrentHashMap<String,User> tabellaUtenti = new ConcurrentHashMap<>();
		ConcurrentHashMap<String,UserGroup> tabellaGruppi = new ConcurrentHashMap<>();
		//creo l'oggeto remoto in cui saranno registrate le callback dei client
		RMIServerImp RmiServer= new RMIServerImp(tabellaUtenti);

		//RMI-----
		try {
			//esporto l'oggetto
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(RmiServer,0);
			String name = "Server";
			//creo il registry sulla porta 5000
			LocateRegistry.createRegistry(5000);
			Registry registry= LocateRegistry.getRegistry(5000);
			//registro l'oggetto nel registry
			registry.bind(name, stub);
		}
		catch(Exception e) {
			System.out.println("Eccezione RMI server");
		}
		//------

		//questo è il threadpool che esaudirà le richieste dei vari client
        ServerListenerTask listener = new ServerListenerTask(tabellaUtenti,RmiServer,tabellaGruppi);
		System.out.println(InetAddress.getLocalHost().getHostAddress());
		//creo il socket di welcome sulla porta 1994
        ServerSocket serverSocket=null;
        try{
            serverSocket = new ServerSocket (1994);
        }
        catch (BindException e){
            System.exit(1);
        }
		Thread th = new Thread(listener);
		th.start();
        while(true) {
			//accetto le connessioni sulla porta di welcome (bloccante)
            Socket sock = serverSocket.accept();
			//creo il task da eseguire
            listener.addSocket(sock);
            //lo passo al threadpool
		}
	}
}
