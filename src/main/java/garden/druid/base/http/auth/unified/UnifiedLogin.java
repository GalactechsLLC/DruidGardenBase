package garden.druid.base.http.auth.unified;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import garden.druid.base.http.auth.unified.database.UnifiedLoginDAO;

public class UnifiedLogin {

	public static UnifiedUser loginWithChia(String launcherID) {
		int userID = UnifiedLoginDAO.getChiaUserID(launcherID);
		if(userID > 0) {
			return UnifiedLoginDAO.loadUser(userID);
		} else {
			UnifiedUser user =  UnifiedLoginDAO.createUser();
			if(user != null) {
				UnifiedLoginDAO.linkChia(user.getId(), launcherID);
			}
			return user;
		}
	}
	
	public static UnifiedUser loginWithGoogle(GoogleIdToken googleToken) {
		int userID = UnifiedLoginDAO.getGoogleUserID(googleToken);
		if(userID > 0) {
			return UnifiedLoginDAO.loadUser(userID);
		} else {
			UnifiedUser user =  UnifiedLoginDAO.createUser();
			if(user != null) {
				UnifiedLoginDAO.linkGoogle(user.getId(), googleToken);
			}
			return user;
		}
	}
	
	public static UnifiedUser linkWithChia(UnifiedUser user, String launcherID) {
		if(user == null) {
			return null;
		}
		int userID = UnifiedLoginDAO.getChiaUserID(launcherID);
		if(user.getId() == userID) { //They are already linked
			return user;
		} else if (userID == -1) { //No Farmer account was created yet
			 UnifiedLoginDAO.linkChia(userID, launcherID);
		} else { //There is an existing farmer account, link it, multiple farmers per google is ok
			 UnifiedLoginDAO.linkChia(userID, launcherID);
		}
		return UnifiedLoginDAO.loadUser(user.getId());
	}
	
	public static UnifiedUser linkWithGoogle(UnifiedUser user, GoogleIdToken googleToken) {
		if(user == null) {
			return null;
		}
		int userID = UnifiedLoginDAO.getGoogleUserID(googleToken);
		if(user.getId() == userID) { //They are already linked
			return user;
		} else if (userID == -1) { //No google account was created yet
			 UnifiedLoginDAO.linkGoogle(userID, googleToken);
		} else { //There is an existing Google account, return null, 1 google account per user
			 return null;
		}
		return UnifiedLoginDAO.loadUser(user.getId());
	}
}
