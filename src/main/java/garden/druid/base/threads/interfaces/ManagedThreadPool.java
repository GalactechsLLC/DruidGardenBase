package garden.druid.base.threads.interfaces;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public interface ManagedThreadPool {
	String getName();
	String getUUID();
	void setSize(int i);
	void pause();
	void resume();
	boolean isPaused();
	HashMap<String, String> getStatus();
	ConcurrentHashMap<String, ManagedTask> getManagedThreads(); 
}
