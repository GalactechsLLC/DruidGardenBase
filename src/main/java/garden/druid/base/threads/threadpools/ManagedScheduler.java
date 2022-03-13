package garden.druid.base.threads.threadpools;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import garden.druid.base.threads.interfaces.ManagedTask;
import garden.druid.base.threads.interfaces.ManagedThreadPool;

public class ManagedScheduler extends ScheduledThreadPoolExecutor implements ManagedThreadPool{

	private final String name;
	private final String uuid;
	private boolean isPaused = false;
    private final transient ReentrantLock pauseLock = new ReentrantLock();
    private final transient Condition unpaused = pauseLock.newCondition();
	private final transient ConcurrentHashMap<String, ManagedTask> managedThreads = new ConcurrentHashMap<>();
	
	public ManagedScheduler(String name, int corePoolSize) {
		super(corePoolSize);
		this.name = name;
		this.uuid = UUID.randomUUID().toString();
	}

	public ManagedScheduler(String name, int corePoolSize, RejectedExecutionHandler handler) {
		super(corePoolSize, handler);
		this.name = name;
		this.uuid = UUID.randomUUID().toString();
	}

	public ManagedScheduler(String name, int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
		this.name = name;
		this.uuid = UUID.randomUUID().toString();
	}

	public ManagedScheduler(String name, int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, threadFactory, handler);
		this.name = name;
		this.uuid = UUID.randomUUID().toString();
	}
	
	public <T> Future<T> submit(ManagedCallable<T> task) {
		managedThreads.put(task.getUUID(), task);
		return super.submit(task);
	}
	
	public <T> Future<T> schedule(ManagedCallable<T> task, long delay, TimeUnit unit) {
		managedThreads.put(task.getUUID(), task);
		return super.schedule(task, delay, unit);
	}
	
	public <T> ScheduledFuture<?> scheduleAtInterval(ManagedRunnable task, long delay, long period, TimeUnit unit) {
		managedThreads.put(task.getUUID(), task);
		return this.scheduleAtFixedRate(task, delay, period, unit);
	}
	
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (isPaused) unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}
	
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (t != null) {
			t.printStackTrace();
		} 
	}

	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
	
	public boolean isPaused() {
		return this.isPaused;
	}
	
	public void setSize(int size) {
		setCorePoolSize(size);
		setMaximumPoolSize(size);
	}
	
	public String getName() {
		return this.name;
	}

	public String getUUID() {
		return this.uuid;
	}
	
	public HashMap<String, String> getStatus(){			
		HashMap<String, String> rtn = new HashMap<>();
		rtn.put("name", name);
		rtn.put("uuid", uuid);
		rtn.put("activeCount", getActiveCount()+"");
		rtn.put("completedTaskCount", getCompletedTaskCount()+"");
		rtn.put("corePoolSize", getCorePoolSize()+"");
		rtn.put("largestPoolSize", getLargestPoolSize()+"");
		rtn.put("maximumPoolSize", getMaximumPoolSize()+"");
		rtn.put("poolSize", getPoolSize()+"");
		rtn.put("queueSize", getQueue().size()+"");
		rtn.put("taskCount", getTaskCount()+"");
		rtn.put("keepAliveTime", getKeepAliveTime(TimeUnit.SECONDS)+"");
		return rtn;
	}

	@Override
	public ConcurrentHashMap<String, ManagedTask> getManagedThreads() {
		return this.managedThreads;
	}
}