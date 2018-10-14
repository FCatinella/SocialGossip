import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class UserGroup {
    ArrayList<String> partecipanti; //lista dei membri del gruppo
    InetAddress multiCastAddr;
    String ipAddr; //indirizzo ip

    public UserGroup(String ip){
        partecipanti = new ArrayList<>();
        ipAddr=ip;
        try {
            multiCastAddr = InetAddress.getByName(ip);
        }
        catch (UnknownHostException e){
            System.out.println("Non riesco a connettermi all'indirizzo multicast");
        }
    }

    public void addMember(String user){
        partecipanti.add(user);
    }

    public ArrayList<String> getPartecipanti() {
        return partecipanti;
    }

    public Boolean isMember(String user){
        return partecipanti.contains(user);
    }


    public String getIpAddr() {
        return ipAddr;
    }

}
