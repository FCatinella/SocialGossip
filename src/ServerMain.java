import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ServerMain  {	
	
	public static void main(String[] args) throws IOException,BindException, InterruptedException {
		System.out.println("Server Partito!");
		//creo la tabella degli utenti registrati al servizio (concorrente)
		ConcurrentHashMap<String,User> tabellaUtenti = new ConcurrentHashMap<String,User>();
		
		RMIServerImp RmiServer= new RMIServerImp(tabellaUtenti);
		//RMI-----
		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(RmiServer,0);
			String name = "Server";
			LocateRegistry.createRegistry(5000);
			Registry registry= LocateRegistry.getRegistry(5000);
			registry.bind(name, stub);
		}
		catch(Exception e) {
			System.out.println("Eccezione RMI sever");
		}
		//------
		//questo è il threadpool che esaudirà le richieste dei vari client
		ThreadPoolServer threadpool = new ThreadPoolServer();
		System.out.println(InetAddress.getLocalHost().getHostAddress());
		//creo il socket di welcome
		ServerSocket serverSocket = new ServerSocket (1994);
		Boolean stop = false;
		while(!stop) {
			//accetto le connessioni sulla porta di welcome (bloccante)
			Socket sock = serverSocket.accept();
			ServerThread thread = new ServerThread(sock,tabellaUtenti,RmiServer);
			threadpool.executeTask(thread);
		}
		serverSocket.close();
		System.out.println("FINE");

	}


}
