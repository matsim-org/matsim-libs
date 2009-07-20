package playground.christoph.knowledge.container.dbtools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.PooledConnection;

import org.apache.log4j.Logger;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class DBConnectionTool {
	
	private static final Logger log = Logger.getLogger(DBConnectionTool.class);
	
	private static String user = "MATSim";
	private static String password ="MATSim";
	private static String db_name = "MATSimKnowledge";
/*	
	private static MysqlConnectionPoolDataSource cpds;
	private static PooledConnection poledCon;
*/	
	private static MysqlConnectionPoolDataSource[] cpdsArray;
	private static PooledConnection[] poledConArray;
	private static int roundRobin = 0;
	private static int conCount = 4;
	
//	private int max_connections = 10;
//	private int inactivity_timeout = 30;
	
	public static void main(String[] args)
	{
		new DBConnectionTool().connect();
	}
	
	private Connection con;
	
	static
	{
		/*
		cpds = new MysqlConnectionPoolDataSource();
		cpds.setUser(user);
		cpds.setPassword(password);
		cpds.setURL("jdbc:mysql://localhost:3306/" + db_name);
		
		try {
			poledCon = cpds.getPooledConnection();
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception when trying to get Pooled Connection");
			e.printStackTrace();
		}
		 */
		
		cpdsArray = new MysqlConnectionPoolDataSource[conCount];
		poledConArray = new PooledConnection[conCount];
		
		for (int i = 0; i < conCount; i++)
		{
			cpdsArray[i] = new MysqlConnectionPoolDataSource();
			cpdsArray[i].setUser(user);
			cpdsArray[i].setPassword(password);
			cpdsArray[i].setURL("jdbc:mysql://localhost:3306/" + db_name);
			
			try {
				poledConArray[i] = cpdsArray[i].getPooledConnection();
			} 
			catch (SQLException e) 
			{
				log.error("SQL Exception when trying to get Pooled Connection");
				e.printStackTrace();
			}
		}
	}
	
	public static synchronized Connection getConnection() throws SQLException
	{	
		//return poledCon.getConnection();
		roundRobin++;
		if (roundRobin >= conCount) roundRobin = 0;
		
		return poledConArray[roundRobin].getConnection();
	}
	
	public void connect()
	{
		try {
			con = getConnection();
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in connect");
			e.printStackTrace();
		}		
	}
	
	public void disconnect()
	{
		try 
		{
			con.close();
			con = null;
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in disconnect");
			e.printStackTrace();
		}
		catch (NullPointerException npe)
		{
			log.error("Connection Object is null!");
		}
	}
	
	public ResultSet executeQuery(String query)
	{
		try
		{
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			
			return rs;
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in executeQuery");
			e.printStackTrace();
		}
		return null;
	}
	
	public int executeUpdate(String query)
	{
		try
		{
			Statement stmt = con.createStatement();
			int result = stmt.executeUpdate(query);
			
			return result;
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in executeQuery");
			e.printStackTrace();
		}
		catch (NullPointerException npe)
		{
			log.error("Connection Object is null!");
		}
		return 0;
	}
	
	public int[] executeBatch(String[] queries)
	{
		try
		{
			Statement stmt = con.createStatement();
			
			for (String query : queries)
			{
				stmt.addBatch(query);
			}
		
			int[] result = stmt.executeBatch();
			
			return result;
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in executeBatch");
			e.printStackTrace();
		}
		catch (NullPointerException npe)
		{
			log.error("Connection Object is null!");
		}
		return new int[0];
	}
	
	public void clearTable(String table)
	{
		try
		{
			Statement stmt = con.createStatement();
		
			String sql = "DELETE FROM " + table;
			
			stmt.executeQuery(sql);
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in clearTable");
			e.printStackTrace();
		}
		catch (NullPointerException npe)
		{
			log.error("Connection Object is null!");
		}
	}
}
