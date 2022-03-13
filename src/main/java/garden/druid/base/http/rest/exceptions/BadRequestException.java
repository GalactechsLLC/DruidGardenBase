package garden.druid.base.http.rest.exceptions;

public class BadRequestException extends Throwable {

	private static final long serialVersionUID = 1L;

	public BadRequestException(String msg) {
		super(msg);
	}
}