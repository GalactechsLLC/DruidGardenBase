package garden.druid.base.threads.threadpools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import garden.druid.base.threads.interfaces.ManagedThreadPool;

public class ThreadPoolManager {
	
	private static final ConcurrentHashMap<String, ManagedThreadPool> threadPools = new ConcurrentHashMap<>();
	
	public static ManagedExecutor newThreadPool(String name) {
		ManagedThreadPool tp = threadPools.get(name);
		if(tp instanceof ManagedExecutor) {
			return (ManagedExecutor)tp;
		} else {
			ManagedExecutor tp2 = new ManagedExecutor(name); 
			threadPools.put(name, tp2);
			return tp2;
		}
	}
	
	public static ManagedExecutor newThreadPool(String name, int coreSize) {
		ManagedThreadPool tp = threadPools.get(name);
		if(tp instanceof ManagedExecutor) {
			return (ManagedExecutor)tp;
		} else {
			ManagedExecutor tp2 = new ManagedExecutor(name, coreSize);
			threadPools.put(name, tp2);
			return tp2;
		}
	}
	
	public static ManagedExecutor newThreadPool(String name, int coreSize, int maxSize) {
		ManagedThreadPool tp = threadPools.get(name);
		if(tp instanceof ManagedExecutor) {
			return (ManagedExecutor)tp;
		} else {
			ManagedExecutor tp2 = new ManagedExecutor(name, coreSize, maxSize);
			threadPools.put(name, tp2);
			return tp2;
		}
	}
	
	public static ManagedExecutor newThreadPool(String name, int coreSize, int maxSize, long keepAliveTime, TimeUnit unit) {
		ManagedThreadPool tp = threadPools.get(name);
		if(tp instanceof ManagedExecutor) {
			return (ManagedExecutor)tp;
		} else {
			ManagedExecutor tp2 = new ManagedExecutor(name, coreSize, maxSize, keepAliveTime, unit);
			threadPools.put(name, tp2);
			return tp2;
		}
	}
	
	public static ManagedScheduler newScheduler(String name, int corePoolSize) {
		ManagedThreadPool tp = threadPools.get(name);
		if(tp instanceof ManagedScheduler) {
			return (ManagedScheduler)tp;
		} else {
			ManagedScheduler tp2 = new ManagedScheduler(name, corePoolSize);
			threadPools.put(name, tp2);
			return tp2;
		}
	}
	
	public static Collection<String> getThreadPoolNames() {
		return threadPools.keySet();
	}
	
	public static ConcurrentHashMap<String, ManagedThreadPool> getManagedThreadPools(){
		return threadPools;
	}
	
	public static ManagedThreadPool getManagedThreadPool(String name) {
		return threadPools.get(name);
	}
	
	public static HashMap<String, HashMap<String,String>> getStatus(){
		HashMap<String, HashMap<String,String>> rtn = new HashMap<>();
		for(Entry<String, ManagedThreadPool> execs : threadPools.entrySet()) {
			rtn.put(execs.getKey(), execs.getValue().getStatus());
		}
		return rtn;
	}
}
