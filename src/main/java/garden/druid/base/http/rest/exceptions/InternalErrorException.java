package garden.druid.base.http.rest.exceptions;

public class InternalErrorException extends Throwable {

	private static final long serialVersionUID = 1L;
	
	public InternalErrorException(String msg) {
		super(msg);
	}
}
