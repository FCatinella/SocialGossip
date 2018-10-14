import java.util.concurrent.*;

public class ThreadPoolServer {
	private ThreadPoolExecutor threadpool;
	
	public ThreadPoolServer() {
		threadpool=(ThreadPoolExecutor) Executors.newCachedThreadPool();
	}
	//esegue il task
	public void executeTask (Runnable task) {
		threadpool.execute(task);
	}

}
