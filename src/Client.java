import java.awt.Font;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javax.swing.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	    ClientMenù clientUi= new ClientMenù(username,sock);
	    ClientListener cl = new ClientListener(sock,clientUi);
	    cl.start();
	}

}
