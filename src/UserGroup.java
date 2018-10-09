import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class UserGroup {
    ArrayList<User> partecipanti;
    InetAddress multiCastAddr;
    int ipPort;
    String ipAddr;

    public UserGroup(String ip,int port){
        partecipanti = new ArrayList<>();
        ipPort=port;
        ipAddr=ip;
        try {
            multiCastAddr = InetAddress.getByName(ip);
        }
        catch (UnknownHostException e){
            System.out.println("Non riesco a connettermi all'indirizzo multicast");
        }
    }

    public void addMember(User user){
        partecipanti.add(user);
    }

    public Boolean isMember(User user){
        return partecipanti.contains(user);
    }

    public int getIpPort() {
        return ipPort;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public InetAddress getMultiCastAddr() {
        return multiCastAddr;
    }
}
