package garden.druid.base.threads.interfaces;

import java.io.Serializable;

public interface ManagedTask extends Serializable {
	
	String getName();
	String getUUID();
	String getStatus();
	boolean isStarted();
	boolean isDone();
}
