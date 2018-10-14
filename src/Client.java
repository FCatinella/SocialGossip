import java.net.Socket;

/*
	Task che spawna i due principali componenti del client:
	- L'interfaccia utente
	- Il listener che riceve i messaggi
 */
public class Client implements Runnable{

    //variabili globali
    private String username;
    private Socket sock;

	//costruttore
	public Client(Socket sock, String username) {
		this.username=username;
		this.sock=sock;
		
	}
	@Override
	public void run() {
		//avvio thread ui
	    ClientMenù clientUi= new ClientMenù(username,sock);
	    //avvio il thread listener
	    ClientListener cl = new ClientListener(sock,clientUi,username);
	    cl.start();
	}

}
