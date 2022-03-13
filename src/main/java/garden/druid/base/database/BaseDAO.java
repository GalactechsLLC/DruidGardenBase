package garden.druid.base.database;

import java.sql.Connection;
import java.util.logging.Level;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import garden.druid.base.logging.Logger;

public class BaseDAO {

	protected static Gson gson = new GsonBuilder().serializeNulls().create();
	
	protected static Connection getConnection(String connectionName) {
		Connection conn = null;
		try {
            Context initContext = new InitialContext();
            Context envContext = (Context)initContext.lookup("java:comp/env");
            DataSource ds = (DataSource)envContext.lookup("jdbc/" + connectionName);
            if(ds == null) {
            	Logger.getInstance().log(Level.SEVERE, "Failed to connect to database");
            	return null;
            }
            else {
                conn = ds.getConnection();
            }
		} catch (Exception ex) {
			Logger.getInstance().log(Level.SEVERE, "Error in BaseDAO.getConnection", ex);
		}
		return conn;
	}
}
