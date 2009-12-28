package playground.christoph.knowledge.container.dbtools;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

	private static MysqlConnectionPoolDataSource[] cpdsArray;
	private static MyPooledConnection[] poledConArray;
	private static int roundRobin = 0;
	private static int conCount = 50;
	private static Object monitor;
	
	public static void main(String[] args)
	{
		new DBConnectionTool().connect();
	}
	
	private Connection con;
	private MyPooledConnection myPooledCon;
	
	static
	{	
		monitor = new Object();
		
		cpdsArray = new MysqlConnectionPoolDataSource[conCount];
		poledConArray = new MyPooledConnection[conCount];
		
		for (int i = 0; i < conCount; i++)
		{
			cpdsArray[i] = new MysqlConnectionPoolDataSource();
			cpdsArray[i].setUser(user);
			cpdsArray[i].setPassword(password);
			cpdsArray[i].setURL("jdbc:mysql://localhost:3306/" + db_name);
						
			try 
			{
				poledConArray[i] = new DBConnectionTool().new MyPooledConnection();
				poledConArray[i].setPooledConnection(cpdsArray[i].getPooledConnection());
				poledConArray[i].inUse(false);
			} 
			catch (SQLException e) 
			{
				log.error("SQL Exception when trying to get Pooled Connection");
				e.printStackTrace();
			}
		}
	}
	
	private static synchronized MyPooledConnection getConnection() throws SQLException
	{	
		// searching for not used Connection
		boolean searching = true;
		
		while (searching)
		{
			synchronized(monitor)
			{
				for (int i = 0; i < conCount; i++)
				{
					roundRobin++;
					if (roundRobin >= conCount) roundRobin = 0;
					
					// if the Connection is not in Use
					if (!poledConArray[roundRobin].inUse())
					{
						searching = false;
						break;
					}
				}	// for
				
				// if no free connection was found - wait until one is released
				if (searching) 
				{
					log.warn("No free Connection was found - waiting until one is released!");
					try 
					{
						monitor.wait();
					} 
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 	// if
			}	// synchronized
		}	// searching
		
		poledConArray[roundRobin].inUse(true);
		return poledConArray[roundRobin];
	}
	
	public void connect()
	{
		try
		{
			myPooledCon = getConnection();
			con = myPooledCon.getPooledConnection().getConnection();
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
			
			synchronized(monitor)
			{
				myPooledCon.inUse(false);
				monitor.notify();
			}
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
	
	public PreparedStatement getPreparedStatement(String sql)
	{
		try 
		{
			return con.prepareStatement(sql);
		} 
		catch (SQLException e)
		{
			log.error("SQL Exception in getPreparedStatement");
			e.printStackTrace();
		}
		return null;
	}
	
	public int executeUpdate(PreparedStatement stmt)
	{
		try
		{
			int result = stmt.executeUpdate();
			
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
	
	protected class MyPooledConnection
	{
		private PooledConnection connection = null;
		private boolean inUse = false;
		
		public void setPooledConnection(PooledConnection con)
		{
			connection = con;
		}
		
		public PooledConnection getPooledConnection()
		{
			return connection;
		}
		
		public void inUse(boolean value)
		{
			this.inUse = value;
		}
		
		public boolean inUse()
		{
			return inUse;
		}
	}
}
