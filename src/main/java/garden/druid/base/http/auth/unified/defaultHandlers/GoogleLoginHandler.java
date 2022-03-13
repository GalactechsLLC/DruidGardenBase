package garden.druid.base.http.auth.unified.defaultHandlers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import garden.druid.base.http.auth.api.User;
import garden.druid.base.http.auth.unified.UnifiedLogin;
import garden.druid.base.http.auth.unified.UnifiedUser;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Body;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.POST;
import garden.druid.base.http.rest.annotation.Request;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;

@WebServlet(value = { "/oauth/google" })
@RestPattern( uri = "/oauth/google")
public class GoogleLoginHandler extends RestEndpoint {

	private static final long serialVersionUID = 1L;

	private static final String CLIENT_ID = "CLIENT_ID_HERE";
	
	@POST
	@Consumes(consumer = ConsumerTypes.BODY)
	@ReturnType(type=ReturnTypes.JSON)
	public User login(@Request HttpServletRequest request, @Body String body) {
		GoogleIdToken accessToken = null;
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),GsonFactory.getDefaultInstance()).setAudience(Collections.singletonList(CLIENT_ID)).build();
			accessToken =  verifier.verify(body);
		} catch (IOException | GeneralSecurityException ignored) {}
		UnifiedUser user = null;
		if(accessToken != null) {
			Object oUser = request.getSession().getAttribute("user");
			user = oUser instanceof UnifiedUser ? (UnifiedUser) oUser : null;
			if(user != null) { //Already logged in, linking google account?
				if(user.getData().get("google") == null) { //no google data saved for this account, save it
					user = UnifiedLogin.linkWithGoogle(user, accessToken);
				} else {
					String googleEmail = (String) user.getData().get("google");
					if(!googleEmail.equalsIgnoreCase(accessToken.getPayload().getEmail())) {
						//trying to link a second google account, i dont want to allow this for now.... TODO
					}
				}
			} else { //Not Logged in, Load/Create user and put the data into the session
				user = UnifiedLogin.loginWithGoogle(accessToken);
			}
			if(user != null) {
				request.getSession().setAttribute("user", user);
			}
		}
		return user;
	}

}
