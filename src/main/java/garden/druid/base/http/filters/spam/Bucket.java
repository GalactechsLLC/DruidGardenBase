package garden.druid.base.http.filters.spam;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bucket {
	
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private final ReadWriteLock readWriteLock;
	private long tokens;
	private final long max, min;
	
	public Bucket(long initialValue, long min, long max, long fillAmount, int fillPeriod, TimeUnit fillTimeUnit) {
		this.tokens = initialValue;
		this.min = min;
		this.max = max;
		service.scheduleAtFixedRate( () -> this.fill(fillAmount), 0, fillPeriod, fillTimeUnit);
		readWriteLock = new ReentrantReadWriteLock();
	}
	
	private void fill(long fill) {
		try {
			readWriteLock.writeLock().lock();
			tokens=Math.max(tokens+fill, max);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	public boolean spend(long cost) {
		try {
			readWriteLock.writeLock().lock();
			if(cost <= tokens) {
				tokens=Math.max(tokens-cost, min);
				return true;
			} else {
				return false;
			}
		} catch(Exception e){
			return false;
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	public void destroy() {
		service.shutdown();
		service = null;
	}
}
