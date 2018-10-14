import java.net.Socket;
import java.util.*;

public class User {
	private String Username; //nome dell'utente
	private String lingua; //lingua con cui si è registrato
	private String password;
	private MySocket userSock; //il socket a cui è connesso
	private Integer online = 0; //se è online o no
	private ArrayList<User> amici= new ArrayList<>(); //lista degli amici
	private ArrayList<String> gruppi = new ArrayList<>(); //lista dei gruppi
    private ArrayList<String> gruppiAddr = new ArrayList<>(); //lista degli indirizzi dei gruppi
	
	//costruttore
	public User(String nome, String pass,String lang) {
		this.Username=nome;
		this.password=pass;
		this.lingua=lang;
	}

	//recupero il socket
	public MySocket getUserSock(){
	    return userSock;
    }

    //aggiorno il socket
	public MySocket updateSock(Socket sock){
	    userSock=new MySocket(sock);
	    return userSock;
    }


    public Integer isOnline() {
		return this.online;
	}
	
	public void connect() {
		this.online=1;
	}
	
	public void disconnect() {
		this.online=0;
	}
	
	public String getUsername() {
		return this.Username;
	}
	
	public boolean checkPass(String param) {
		return(param.equals(password));
	}
	
	@Override
	public boolean equals (Object user) {
		User realuser= (User) user;
		System.out.println("confronto "+this.Username+" con "+realuser.getUsername());
		return (this.Username.equals(realuser.getUsername()));
	}

	public boolean addFriend(User a) {
		System.out.println(a.getUsername()+" e "+this.getUsername());
		if (amici.contains(a)) {
			System.out.println("Già amici");
			return false;
		}
		amici.add(a);
		return true;
	}
	
	public boolean isFriend(User a) {
		if(this.Username.equals(a.getUsername())) return true;
		return amici.contains(a);
	}

	public ArrayList<String> getFriendList(){
	    ArrayList<String> friendNameList = new ArrayList<>();
	    Iterator <User> i = amici.iterator();
	    while(i.hasNext()){
	        User aux = i.next();
	        friendNameList.add(aux.getUsername());
        }
        return friendNameList;
    }

    public ArrayList<String> getGroupsList(){
	    return gruppi;
    }

	public ArrayList<String> getGroupsListAddr(){
		return gruppiAddr;
	}

    public void addGroup (String gruppo) {
	    gruppi.add(gruppo);
    }

    public void addGroupAddr (String gruppoAddr) {
        gruppiAddr.add(gruppoAddr);
    }

	public void removeGroup (String gruppo) {
		gruppi.remove(gruppo);
	}

	public void removeGroupAddr (String gruppoAddr) {
		gruppiAddr.remove(gruppoAddr);
	}

    public String getLingua(){
	    return lingua;
    }
}
