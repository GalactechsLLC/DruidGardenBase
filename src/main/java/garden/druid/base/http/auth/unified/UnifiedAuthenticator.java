package garden.druid.base.http.auth.unified;

import javax.servlet.http.HttpServletRequest;

import garden.druid.base.http.auth.api.Authenticator;
import garden.druid.base.http.auth.api.UserLevel;

public class UnifiedAuthenticator implements Authenticator {
	
	@Override
	public UnifiedUser getUser(HttpServletRequest request) {
		Object oUser = request.getSession().getAttribute("user");
		if(oUser instanceof UnifiedUser) {
			return (UnifiedUser) oUser;
		} else {
			return null;
		}
	}

	@Override
	public boolean requireLogin(HttpServletRequest request) {
		UnifiedUser user = getUser(request);
		return user != null;
	}

	@Override
	public boolean authenticate(HttpServletRequest request, int requiredLevel) {
		if(request == null) {
			return false;
		}
		UnifiedUser user = getUser(request);
		if(user == null && UserLevel.NONE == requiredLevel) {
			return true;
		} else if(user == null) {
			return false;
		} else {
			return user.getUserLevel() >= requiredLevel;
		}
	}

	@Override
	public void logout(HttpServletRequest request) {
		request.getSession().removeAttribute("user");
		request.getSession().invalidate();
	}
}
