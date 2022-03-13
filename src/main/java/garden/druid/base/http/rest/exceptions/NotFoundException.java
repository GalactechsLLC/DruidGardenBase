package garden.druid.base.http.rest.exceptions;

public class NotFoundException extends Throwable {

	private static final long serialVersionUID = 1L;
	
	public NotFoundException(String msg) {
		super(msg);
	}
}