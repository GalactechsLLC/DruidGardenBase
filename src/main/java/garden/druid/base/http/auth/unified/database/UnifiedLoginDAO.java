package garden.druid.base.http.auth.unified.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import garden.druid.base.database.BaseDAO;
import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.auth.unified.UnifiedUser;
import garden.druid.base.logging.Logger;

public class UnifiedLoginDAO extends BaseDAO{
	
	public static int getGoogleUserID(GoogleIdToken googleToken) { 
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("SELECT user_id FROM `OAuth`.`google` WHERE `email`=?");
			stmt.setString(1, googleToken.getPayload().getEmail().toLowerCase());
			rs = stmt.executeQuery();
			if(rs.next()) {
				return rs.getInt("user_id");
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.getGoogleUserID", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception ignored) {}
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
		return -1;
	}

	public static UnifiedUser createUser() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("INSERT INTO `OAuth`.users(`id`, `uuid`, `userLevel`) VALUES(null, UUID_TO_BIN(uuid()), ?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, UserLevel.USER);
			stmt.executeUpdate();
			rs = stmt.getGeneratedKeys();
			if(rs.next()) {
				int user_id = rs.getInt(1);
				return loadUser(user_id);
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.createUser", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception ignored) {}
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
		return null;
	}
	
	public static int getChiaUserID(String launcherID) { 
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("SELECT user_id FROM `OAuth`.`chia` WHERE `launcher_id`=?");
			stmt.setString(1, launcherID.toLowerCase());
			rs = stmt.executeQuery();
			if(rs.next()) {
				return rs.getInt("user_id");
			}
		} catch (Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.getChiaUserID", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception ignored) {}
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
		return -1;
	}
	
	public static UnifiedUser loadUser(int userID) { 
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		UnifiedUser user = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("SELECT u.id, BIN_TO_UUID(u.uuid) as `uuid`, u.userLevel as `userLevel`, c.launcher_id, g.email, a.is_admin "
					+ "FROM `OAuth`.`users` u "
					+ "left join `OAuth`.`chia` c on u.id=c.user_id "
					+ "left join `OAuth`.`google` g on u.id=g.user_id "
					+ "left join `OAuth`.`admin` a on u.id=a.user_id "
					+ "where u.id=?");
			stmt.setInt(1, userID);
			rs = stmt.executeQuery();
			if(rs.next()) {
				user = new UnifiedUser();
				user.setId(rs.getInt("id"));
				user.setUserLevel(rs.getInt("userLevel"));
				user.setUuid(rs.getString("uuid"));
				String launcher_id = rs.getString("launcher_id");
				if(launcher_id != null) {
					user.getData().put("chia", rs.getString("launcher_id"));
				}
				String email = rs.getString("email");
				if(email != null) {
					user.getData().put("google", rs.getString("email"));
				}
				String isAdmin = rs.getString("is_admin");
				if(isAdmin != null) {
					user.getData().put("admin", Boolean.parseBoolean(rs.getString("is_admin")));
				}
				return user;
			}
		} catch (Exception e) {
			user = null;
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.loadUser", e);
		} finally {
			if(rs != null) try {rs.close();}catch(Exception ignored) {}
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
		return user;
	}

	public static void linkChia(int userID, String launcherID) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("INSERT INTO `OAuth`.`chia` (`user_id`, `launcher`) VALUES (?, ?)");
			stmt.setInt(1, userID);
			stmt.setString(2, launcherID);
			stmt.executeUpdate();
		} catch (SQLException ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.linkChia", ex);
		} finally {
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
	}

	public static void linkGoogle(int userID, GoogleIdToken googleToken) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection("OAuth");
			stmt = conn.prepareStatement("INSERT INTO `OAuth`.`google` (`user_id`, `email`) VALUES (?, ?)");
			stmt.setInt(1, userID);
			stmt.setString(2, googleToken.getPayload().getEmail());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in UnifiedLoginDAO.linkGoogle", ex);
		} finally {
			if(stmt != null) try {stmt.close();}catch(Exception ignored) {}
			if(conn != null) try {conn.close();}catch(Exception ignored) {}
		}
	}
}
