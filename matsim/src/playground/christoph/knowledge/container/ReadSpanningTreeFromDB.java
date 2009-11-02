package playground.christoph.knowledge.container;

import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

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
	
	/*
	 * Should float precision instead of double precision for the calculation
	 * of the trip costs be used? Saves half of the memory and should not 
	 * change the results significantly.
	 */
	private boolean useFloatPrecision = true;
	
	private Network network;
	private DBConnectionTool dbConnectionTool;
	private ByteArrayConverter bac;
	private Zipper zipper;
	private String forwardTableName = "forwardTable";
	private String backwardTableName = "backwardTable"; 
	
	public ReadSpanningTreeFromDB(Network network)
	{
		this.network = network;
		this.dbConnectionTool = new DBConnectionTool();
		this.bac = new ByteArrayConverter();
		this.zipper = new Zipper();
	}
	
	public Map<Node, Double> getForwardSpanningTree(Node node)
	{
		byte[] bytes = readFromDB(node, forwardTableName);
		
		Map<Node, Double> map = null;
		if (bytes != null)
		{
			map = createMapFromByteArray(bytes);
		}

		return map;
	}
	
	public Map<Node, Double> getBackwardSpanningTree(Node node)
	{
		byte[] bytes = readFromDB(node, backwardTableName);
		
		Map<Node, Double> map = null;
		if (bytes != null)
		{
			map = createMapFromByteArray(bytes);
		}

		return map;
	}
	
	public Map<Node, Double> getTotalSpanningTree(Node startNode, Node endNode)
	{
		double[] forwardCosts = getDoubleArrayFromBytes(readFromDB(startNode, forwardTableName));
		double[] backwardCosts = getDoubleArrayFromBytes(readFromDB(startNode, backwardTableName));
		
		Map<Node, Double> map = new TreeMap<Node, Double>();
		
		int i = 0;
		for (Node node : network.getNodes().values())
		{
			map.put(node, forwardCosts[i] + backwardCosts[i]);
			i++;
		}

		return map;	
	}
	
	private Map<Node, Double> createMapFromByteArray(byte[] bytes)
	{		
		double[] costs = getDoubleArrayFromBytes(bytes);
		
		Map<Node, Double> map = new TreeMap<Node, Double>();
		
		int i = 0;
		for (Node node : network.getNodes().values())
		{
			map.put(node, costs[i]);
			i++;
		}
				
		return map;
	}
	
	private double[] getDoubleArrayFromBytes(byte[] bytes)
	{
		double[] costs;
		
		if (this.useFloatPrecision)
		{
			float[] floatCosts = bac.toFloatArray(bytes);
			costs = new double[floatCosts.length];
			
			int i = 0;
			for (float floatCost : floatCosts)
			{
				costs[i] = (double)floatCost;
				i++;
			}
		}
		else
		{
			costs = bac.toDoubleArray(bytes);
		}
		
		return costs;
	}
	
	private byte[] readFromDB(Node node, String tableName)
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
