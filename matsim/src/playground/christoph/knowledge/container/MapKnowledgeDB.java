package playground.christoph.knowledge.container;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;

/*
 * Reads and Writes a Person's known Nodes to a MySQL Database.
 */
public class MapKnowledgeDB extends MapKnowledge implements DBStorage{
	
	private final static Logger log = Logger.getLogger(MapKnowledgeDB.class);
	
	public static int DBReadCounter = 0;
	
	private boolean localKnowledge;
	private DBConnectionTool dbConnectionTool = new DBConnectionTool();
	
	private static String separator = "@";
	public static String baseTableName = "MapKnowledge";
	private String tableName = "BatchTable1_25";
		
	private Lock lock = new ReentrantLock();
	
	public MapKnowledgeDB()
	{
		super();
		localKnowledge = false;
	}

	public void setTableName(String name)
	{
		tableName = name;
	}
	
	@Override
	public boolean knowsNode(Node node)
	{
		readFromDB();
		
		return super.knowsNode(node);
	}
	
	
	@Override
	public boolean knowsLink(Link link)
	{
		readFromDB();
		
		return super.knowsLink(link);
	}
	
	
	@Override
	public Map<Id, Node> getKnownNodes()
	{
		readFromDB();
		
		return super.getKnownNodes();
	}
	
	@Override
	public void setKnownNodes(Map<Id, Node> nodes)
	{
		super.setKnownNodes(nodes);
		
		this.writeToDB();
	}
	
	public boolean tryReadFromDB()
	{
		if (lock.tryLock())
		{
			lock.unlock();
			readFromDB();
			return true;
		}
		return false;
	}
	
	public synchronized void readFromDB()
	{	
		lock.lock();
		// If Knowledge is currently not in Memory, get it from the DataBase
		if (!localKnowledge)
		{	DBReadCounter++;
			ResultSet rs;
			synchronized(dbConnectionTool)
			{
				dbConnectionTool.connect();
				rs = dbConnectionTool.executeQuery("SELECT * FROM " + tableName + " WHERE PersonId='" + this.getPerson().getId() + "'");
				dbConnectionTool.disconnect();
			}
			
			try 
			{			
				while (rs.next())
				{	
					/*
					 *  Initially we say it is a WhiteList.
					 *  Now we can add all Nodes from the DB directly to the
					 *  KnowledgeMap. If the KnowledgeMap Object would think
					 *  it is a BlackList, it would invert all added Nodes...
					 */
					this.isWhiteList = true;
					
					String[] nodeIds = rs.getString("NodeIds").split(separator);
			
					for (String id : nodeIds)
					{
						//NodeImpl node = this.network.getNode(new IdImpl(id));
						Node node = this.network.getNodes().get(new IdImpl(id));
						super.addNode(node);
					}
					
					// Finally we set The ListType
					
					String listType = rs.getString("WhiteList");
					this.isWhiteList = listType.equals("1") || listType.equalsIgnoreCase("true") || listType.equalsIgnoreCase("yes");
				}
			}
			catch (SQLException e)
			{		
				e.printStackTrace();
			}
			
			localKnowledge = true;
		}
		lock.unlock();
	}
	
	public synchronized void writeToDB()
	{		
		/*
		 *  We want the Nodes "as they are" -
		 *  as White- or as BlackList.
		 */
		Map<Id, Node> nodes = super.getNodes();
		
		String nodesString = createNodesString(nodes);
//		Insert Into MapKnowledge SET NodeId='2', PersonId='12'
		String query = "INSERT INTO " + tableName + " SET PersonId='"+ person.getId() + "', WhiteList=" + isWhiteList + ", NodeIds='" + nodesString + "'";
		
		synchronized(dbConnectionTool)
		{
			dbConnectionTool.connect();
			dbConnectionTool.executeUpdate(query);
			dbConnectionTool.disconnect();
		}
	}
	
	public synchronized void clearLocalKnowledge()
	{
		super.clearKnowledge();
		localKnowledge = false;
	}
	
	public void clearTable()
	{
		if (tableExists()) 
		{			
			String clearTable = "DELETE FROM " + tableName;
			
			synchronized(dbConnectionTool)
			{
				dbConnectionTool.connect();
				dbConnectionTool.executeUpdate(clearTable);
				dbConnectionTool.disconnect();
			}
		}
	}
	
	
	public void createTable()
	{
		String createTable = "CREATE TABLE " + tableName + " (PersonId INTEGER, WhiteList BOOLEAN, NodeIds LONGTEXT, PRIMARY KEY (PersonId))";
		//CREATE TABLE Customer (SID integer, Last_Name varchar(30), First_Name varchar(30), PRIMARY KEY (SID));
		//String createTable = "CREATE TABLE " + tableName + " (NodeId integer, PRIMARY KEY (NodeId))";
		
		// If the Table doesn't exist -> create it.
		if (!tableExists())
		{
//			DBConnectionTool dbConnectionTool = new DBConnectionTool();
			
			synchronized(dbConnectionTool)
			{
				dbConnectionTool.connect();
				dbConnectionTool.executeUpdate(createTable);
				dbConnectionTool.disconnect();
			}
		}
	
     /*       
	        String table = "CREATE TABLE java_DataTypes2("+ "typ_boolean BOOL, "              
	                            + "typ_byte          TINYINT, "          
	                            + "typ_short         SMALLINT, "          
	                            + "typ_int           INTEGER, "           
	                            + "typ_long          BIGINT, "            
	                            + "typ_float         FLOAT, "             
	                            + "typ_double        DOUBLE PRECISION, "  
	                            + "typ_bigdecimal    DECIMAL(13,0), "    
	                            + "typ_string        VARCHAR(254), "      
	                            + "typ_date          DATE, "              
	                            + "typ_time          TIME, "              
	                            + "typ_timestamp     TIMESTAMP, "         
	                            + "typ_asciistream   TEXT, "              
	                            + "typ_binarystream  LONGBLOB, "          
	                            + "typ_blob          BLOB)";
	 */
	}
	
	private boolean tableExists()
	{
		String tableExists = "SHOW TABLES LIKE '" + tableName + "'";
		
		synchronized(dbConnectionTool)
		{
			dbConnectionTool.connect();
			ResultSet rs = dbConnectionTool.executeQuery(tableExists);
			
			try 
			{	
				// If the Table doesn't exist -> create it.
				if (rs.next()) return true;
			} 
			catch (SQLException e)
			{		
				e.printStackTrace();
			}
			dbConnectionTool.disconnect();
		}
		return false;
	}
	
	private String createNodesString(Map<Id, Node> nodes)
	{
		// if no Nodes are known -> just return a separator
		if (nodes.values().size() == 0) return separator;
		
		String string = "";
		
		for (Node node : nodes.values())
		{
			string = string + node.getId() + separator;
		}
			
		return string;
	}

}