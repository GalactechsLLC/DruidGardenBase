package garden.druid.base.http.auth.api;

import javax.servlet.http.HttpServletRequest;

public interface Authenticator {
	boolean requireLogin(HttpServletRequest request);
	void logout(HttpServletRequest request);
	boolean authenticate(HttpServletRequest request, int requiredLevel);
	User getUser(HttpServletRequest request);
}
