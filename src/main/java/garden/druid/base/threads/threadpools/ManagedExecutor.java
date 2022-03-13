package garden.druid.base.threads.threadpools;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import garden.druid.base.threads.interfaces.ManagedTask;
import garden.druid.base.threads.interfaces.ManagedThreadPool;

public class ManagedExecutor extends ThreadPoolExecutor implements ManagedThreadPool {

	private final String name;
	private final String uuid;
	private boolean isPaused = false;
    private transient final ReentrantLock pauseLock = new ReentrantLock();
    private transient final Condition unpaused = pauseLock.newCondition();
	private transient final ConcurrentHashMap<String, ManagedTask> managedThreads = new ConcurrentHashMap<>();
	
	protected ManagedExecutor(String name) {
		this(name, 1, 1, 600, TimeUnit.SECONDS);
	}
	
	protected ManagedExecutor(String name, int size) {
		this(name, size, size, 600, TimeUnit.SECONDS);
	}
	
	protected ManagedExecutor(String name, int core_size, int max_size) {
		this(name, core_size, max_size, 600, TimeUnit.SECONDS);
	}
	
	protected ManagedExecutor(String name, int core_size, int max_size, long keepAliveTime, TimeUnit unit) {
		super(core_size, max_size, keepAliveTime, unit, new LinkedBlockingQueue<>());
		this.name = name;
		this.uuid = UUID.randomUUID().toString();
	}
	
	public <T> Future<T> submit(ManagedCallable<T> task) {
		managedThreads.put(task.getUUID(), task);
		return super.submit(task);
	}
	
	public Future<?> submit(ManagedRunnable task) {
		managedThreads.put(task.getUUID(), task);
		return super.submit(task);
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
		if (t == null && r instanceof Future<?>) {
			try {
				((Future<?>) r).get();
			} catch (CancellationException ce) {
				t = ce;
			} catch (ExecutionException ee) {
				t = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt(); // ignore/reset
			}
		}
		if(r instanceof ManagedTask) {
			ManagedTask clbl = (ManagedTask) r;
			managedThreads.remove(clbl.getUUID());
		}
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
		rtn.put("status", isPaused ? "paused" : this.getActiveCount() == 0 ? "waiting" : "running");
		return rtn;
	}
	
	public String getName() {
		return this.name;
	}

	public String getUUID() {
		return this.uuid;
	}

	@Override
	public ConcurrentHashMap<String, ManagedTask> getManagedThreads() {
		return this.managedThreads;
	}
}
