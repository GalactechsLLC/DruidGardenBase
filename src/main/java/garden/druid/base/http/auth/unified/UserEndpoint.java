package garden.druid.base.http.auth.unified;


import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import garden.druid.base.http.auth.api.Authenticator;
import garden.druid.base.http.auth.api.User;
import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.AuthenticatorCls;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;

@WebServlet(value = { "/me" })
@RestPattern( uri = "/me")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.USER)
public class UserEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public User getUser(@Request HttpServletRequest request, @AuthenticatorCls Authenticator auth) {
		return auth.getUser(request);
	}
	
}

