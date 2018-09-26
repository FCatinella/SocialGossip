import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerSocketChecker {
	CopyOnWriteArrayList<Socket> listaSock = null;
	private ThreadPoolExecutor threadpool;

	
	public ServerSocketChecker(CopyOnWriteArrayList<Socket> listaSocket) {
		listaSock=listaSocket;
		threadpool=(ThreadPoolExecutor) Executors.newCachedThreadPool();
		boolean stop = false;
		while(!stop) {
			int i=0;
			for(i=0;i<listaSock.size();i++) {
				Socket iSocket=listaSock.get(i);
				//fai tutto
			}
		}
	}
	
	
}
