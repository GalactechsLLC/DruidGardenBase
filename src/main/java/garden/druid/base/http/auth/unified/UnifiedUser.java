package garden.druid.base.http.auth.unified;

import java.util.concurrent.ConcurrentHashMap;

import garden.druid.base.http.auth.api.User;
import garden.druid.base.http.auth.api.UserLevel;

public class UnifiedUser implements User{

	private String uuid;
	private int id;
	private final ConcurrentHashMap<String, Object> userData = new ConcurrentHashMap<>();
	private int level = UserLevel.NONE;
	
	public UnifiedUser() {}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ConcurrentHashMap<String, Object> getData() {
		return userData;
	}

	public void setUserLevel(int level) {
		this.level = level;
	}

	@Override
	public int getUserLevel() {
		return this.level;
	}
}
