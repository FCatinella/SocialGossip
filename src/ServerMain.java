import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;


public class ServerMain  {	

	public static void main(String[] args) throws IOException,BindException, InterruptedException {
		System.out.println("Server Partito!");
		//creo la tabella degli utenti registrati al servizio (concorrente)
		ConcurrentHashMap<String,User> tabellaUtenti = new ConcurrentHashMap<String,User>();
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
			System.out.println("Eccezione RMI sever");
		}
		//------

		//questo è il threadpool che esaudirà le richieste dei vari client
        ServerListenerTask listener = new ServerListenerTask(tabellaUtenti,RmiServer);
		System.out.println(InetAddress.getLocalHost().getHostAddress());
		//creo il socket di welcome sulla porta 1994
		ServerSocket serverSocket = new ServerSocket (1994);
		Boolean stop = false;
		Thread th = new Thread(listener);
		th.start();
        while(!stop) {
			//accetto le connessioni sulla porta di welcome (bloccante)
            Socket sock = serverSocket.accept();
			//creo il task da eseguire
            listener.addSocket(sock);
            //lo passo al threadpool
		}
		//chiudo il socket e termino il server
		serverSocket.close();
		System.out.println("FINE");

	}


}
