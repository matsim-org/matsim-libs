package playground.christoph.knowledge.container;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.tools.ByteArrayConverter;
import playground.christoph.tools.Zipper;

public class ReadSpanningTreeFromDB {

	/*
	 * Should the byte Arrays be compressed? Will save about 10% memory but
	 * increases the calculation effort quite a lot.
	 */
	private boolean useCompression = false;
	
	private DBConnectionTool dbConnectionTool;
	private Zipper zipper;
	
	public ReadSpanningTreeFromDB()
	{
		this.dbConnectionTool = new DBConnectionTool();
		this.zipper = new Zipper();

	}
	
	public byte[] readFromDB(Node node, String tableName)
	{	
		ResultSet rs;
		
		dbConnectionTool.connect();
		rs = dbConnectionTool.executeQuery("SELECT * FROM " + tableName + " WHERE NodeId='" + node.getId() + "'");
		dbConnectionTool.disconnect();
				
		try 
		{	
			while (rs.next())
			{					
//				// if using getBlob causes Problems, maybe 
//				Blob blob = rs.getBlob("SpanningTree");
//				byte[] bytes =  blob.getBytes((long) 1, (int) blob.length());
				
				byte[] bytes = rs.getBytes("SpanningTree");
				
				if (this.useCompression)
				{
					bytes = this.zipper.decompress(bytes);
				}
				
				return bytes;
			}
		}
		catch (Exception e)
		{		
			e.printStackTrace();
		}
		
		return null;
	}
}
