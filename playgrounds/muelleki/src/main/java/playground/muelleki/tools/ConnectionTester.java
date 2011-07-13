/**
 * 
 */
package playground.muelleki.tools;

import java.sql.*;
import java.util.Properties;

import org.apache.log4j.Logger;

import playground.muelleki.util.Pgpass;

/**
 * @author Kirill
 *
 */
public class ConnectionTester {
	static Logger _log = Logger.getLogger(ConnectionTester.class);

	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) {
		String url = "jdbc:postgresql://albion/sustaincity";
		Properties props = new Properties();
		String userName = "sustaincity";
		props.setProperty("user", userName);
		String password = new Pgpass().getPgpass(url, userName);
		assert password != null : "Password not found";
		props.setProperty("password", password);
		try {
			Connection conn = DriverManager.getConnection(url, props);
			CallableStatement st = conn.prepareCall("SELECT hhnr FROM sc.vz ORDER BY 1 LIMIT 100");
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				_log.info(rs.getInt(1));
			}
			
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
