package garden.druid.base.logging;

public class Logger {
	
	private static final java.util.logging.Logger instance = java.util.logging.Logger.getLogger(Logger.class.getName());
	
	public static java.util.logging.Logger getInstance(){
		return instance;
	}
}
