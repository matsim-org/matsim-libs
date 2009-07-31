package playground.christoph.knowledge.container;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;

/*
 * Reads and Writes a Person's known Nodes to a MySQL Database.
 */
public class MapKnowledgeDB extends MapKnowledge implements DBStorage{
	
	private final static Logger log = Logger.getLogger(MapKnowledgeDB.class);
	
	private boolean localKnowledge;
	private DBConnectionTool dbConnectionTool = new DBConnectionTool();
	
	private static String separator = "@";
	//private static String tableName = "MapKnowledge";
	private static String tableName = "BatchTable2_0";
	
	public MapKnowledgeDB()
	{
		super();
		localKnowledge = false;
	}
/*
	public MapKnowledgeDB(Map<Id, NodeImpl> nodes)
	{
		super(nodes);
		localKnowledge = true;
	}
*/	
	public static void setTableName(String name)
	{
		tableName = name;
	}
	
	@Override
	public boolean knowsNode(NodeImpl node)
	{
		readFromDB();
		
		return super.knowsNode(node);
	}
	
	
	@Override
	public boolean knowsLink(LinkImpl link)
	{
		readFromDB();
		
		return super.knowsLink(link);
	}
	
	
	@Override
	public Map<Id, NodeImpl> getKnownNodes()
	{
		readFromDB();
		
		return super.getKnownNodes();
	}
	
	@Override
	public void setKnownNodes(Map<Id, NodeImpl> nodes)
	{
		super.setKnownNodes(nodes);
		
		this.writeToDB();
	}
	
	public synchronized void readFromDB()
	{	
		// If Knowledge is currently not in Memory, get it from the DataBase
		if (!localKnowledge)
		{	
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
					boolean listType = Boolean.valueOf(rs.getString("WhiteList"));
					this.isWhiteList = listType;
					
					String[] nodeIds = rs.getString("NodeIds").split(separator);
			
					for (String id : nodeIds)
					{								
						//NodeImpl node = this.network.getNode(new IdImpl(id));
						NodeImpl node = this.network.getNodes().get(new IdImpl(id));
						super.getKnownNodes().put(node.getId(), node);
					}
				}
			}
			catch (SQLException e)
			{		
				e.printStackTrace();
			}
			
			localKnowledge = true;
		}
	}

	
	public synchronized void writeToDB()
	{		
		Map<Id, NodeImpl> nodes = super.getKnownNodes();
		
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

	
	public void clearLocalKnowledge()
	{
		localKnowledge = false;
		super.getKnownNodes().clear();
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
	
	private String createNodesString(Map<Id, NodeImpl> nodes)
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
	
/*	
	private Map<Id, Node> createNodesMap(String string)
	{
		Map<Id, Node> nodesMap = new HashMap<Id, Node>();
		
		String[] nodeIds = string.split(this.separator);
		
		for (String id : nodeIds)
		{
			Node node = this.network.getNode(new IdImpl(id));
			this.getKnownNodes().put(node.getId(), node);
		}
		
		return nodesMap;
	}
*/
}