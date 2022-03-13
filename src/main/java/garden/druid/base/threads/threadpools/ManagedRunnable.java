package garden.druid.base.threads.threadpools;

import garden.druid.base.threads.interfaces.ManagedTask;

public abstract class ManagedRunnable implements Runnable, ManagedTask  {
	private static final long serialVersionUID = 5260859099363794957L;
	
	protected String uuid = null, name = null, status = "Not Started";
	protected boolean started = false, done = false;
	
	public String getName() {
		return this.name;
	}

	public String getUUID() {
		return this.uuid;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public boolean isStarted() {
		return this.started;
	}
	
	public boolean isDone() {
		return this.done;
	}
}
