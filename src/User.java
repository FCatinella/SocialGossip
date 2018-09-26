import java.util.*;

public class User {
	String Username = "";
	String lingua = "";
	String password = "";
	private Integer online = 0;
	ArrayList<User> amici= new ArrayList<User>();
	
	
	
	public User(String nome, String pass,String lang) {
		this.Username=nome;
		this.password=pass;
		this.lingua=lang;
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
		if(param.equals(password)) return true;
				return false;
	}
	
	@Override
	public boolean equals (Object user) {
		User realuser= (User) user;
		System.out.println("confronto "+this.Username+" con "+realuser.getUsername());
		if (this.Username.equals(realuser.getUsername())) {
			
			return true;
		}
		return false;
	}
	
	public void setLanguage(String lingua) {
		this.lingua=lingua;
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
	
}
