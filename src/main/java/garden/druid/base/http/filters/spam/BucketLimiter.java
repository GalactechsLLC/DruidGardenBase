package garden.druid.base.http.filters.spam;

import java.time.Instant;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import garden.druid.base.cache.Cache;

public class BucketLimiter {

	protected Cache<String, Bucket> buckets = new Cache<>(-1);
	protected Cache<String, Instant> lastSeen = new Cache<>(-1);
	private final long initialValue, max, min, fillAmount;
	private final int fillPeriod;
	private final TimeUnit fillTimeUnit;
	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private final long garbageCollectinterval = 60;
	private final long bucketRemovalinterval = 600;
	
	public BucketLimiter(long initialValue, long min, long max, long fillAmount, int fillPeriod, TimeUnit fillTimeUnit) {
		this.initialValue = initialValue;
		this.min = min;
		this.max = max;
		this.fillPeriod = fillPeriod;
		this.fillAmount = fillAmount;
		this.fillTimeUnit = fillTimeUnit;
		service.scheduleAtFixedRate( () -> {
			if(lastSeen.size() > 0) {
				for(Entry<String, Instant> entry : lastSeen.entrySet()) {
					if(Instant.now().getEpochSecond() - entry.getValue().getEpochSecond() >= bucketRemovalinterval) {
						buckets.get(entry.getKey()).destroy();
						lastSeen.remove(entry.getKey());
						buckets.remove(entry.getKey());
					}
				}
			}
		}, garbageCollectinterval, garbageCollectinterval, TimeUnit.SECONDS);
	}
	
	public Bucket getBucket(String key) {
		lastSeen.put(key, Instant.now());
		Bucket bucket = buckets.get(key);
		if(bucket == null) {
			buckets.put(key, new Bucket(initialValue, min, max, fillAmount, fillPeriod, fillTimeUnit));
		}
		return bucket;
	}
}
