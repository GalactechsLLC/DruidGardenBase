package garden.druid.base.http.auth.api;

public class UserLevel {
	public static final int NONE = Integer.MIN_VALUE;
	public static final int USER = 100;
	public static final int VIEWER = 200;
	public static final int MODERATOR = 300;
	public static final int ADMIN = 400;
	public static final int SUPERUSER = Integer.MAX_VALUE;
}
