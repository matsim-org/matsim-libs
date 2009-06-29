package playground.christoph.knowledge.container.dbtools;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class DBConnectionTool {
	
	private static final Logger log = Logger.getLogger(DBConnectionTool.class);
	
	private static String user = "MATSim";
	private static String password ="MATSim";
	private static String db_name = "MATSimKnowledge";
	
	private static MysqlConnectionPoolDataSource cpds;
	
//	private int max_connections = 10;
//	private int inactivity_timeout = 30;
	
	private Connection con;
	
	static
	{
		cpds = new MysqlConnectionPoolDataSource();
		cpds.setUser(user);
		cpds.setPassword(password);
		cpds.setURL("jdbc:mysql://localhost:3306/" + db_name);
	}
	
	public static synchronized Connection getConnection() throws SQLException
	{
		return cpds.getConnection();
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
		/*
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch (ClassNotFoundException cnf)
		{
			log.error("Class not Found Exception");
			cnf.printStackTrace();
		}
		
		try 
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + db_name, user, password);
		} 
		catch (SQLException e) 
		{
			log.error("SQL Exception in connect");
			e.printStackTrace();
		}
		*/		
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
