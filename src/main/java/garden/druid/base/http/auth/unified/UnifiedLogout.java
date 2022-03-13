package garden.druid.base.http.auth.unified;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import garden.druid.base.http.auth.api.Authenticator;
import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.AuthenticatorCls;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.DELETE;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.POST;
import garden.druid.base.http.rest.annotation.PUT;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;

@WebServlet(value = { "/oauth/logout" })
@RestPattern(uri = "/oauth/logout")
@RequireAuthenticator()
@RequireUserLevel()
public class UnifiedLogout extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@GET
	@POST
	@PUT
	@DELETE
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public String logout(@AuthenticatorCls Authenticator authenticator, @Request HttpServletRequest request) {
		authenticator.logout(request);
		return "";
	}
}

