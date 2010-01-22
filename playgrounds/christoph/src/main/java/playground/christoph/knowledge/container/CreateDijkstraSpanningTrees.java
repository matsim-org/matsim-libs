/* *********************************************************************** *
 * project: org.matsim.*
 * CreateDijkstraSpanningTrees.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.knowledge.container;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelCost;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.knowledge.nodeselection.DijkstraForSelectNodes;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.tools.ByteArrayConverter;
import playground.christoph.tools.Zipper;

/*
 * Creates two Maps for each Node of a given Network:
 * - one containing the travel costs to all nodes traveling forwards
 * - one containing the travel costs to all nodes traveling backwards 
 */
public class CreateDijkstraSpanningTrees {

	private final static Logger log = Logger.getLogger(CreateDijkstraSpanningTrees.class);
	
	private NetworkLayer network;

	private int numOfThreads = 8;

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
	
	/*
	 * Dijkstra based Node Selector
	 * The selected Nodes depend on the used Cost Calculator
	 */
	// null as Argument: -> no TimeCalculator -> use FreeSpeedTravelTime
	private TravelCost costCalculator = new OnlyTimeDependentTravelCostCalculator(null);
	
	private Map<Id, byte[]> forwardExecution;
	private Map<Id, byte[]> backwardExecution;
	
	public static void main(String[] args)
	{
//		new DatabaseConfig().clearTable("forwardTable");
//		new DatabaseConfig().clearTable("backwardTable");

		new DatabaseConfig().createTable("forwardTable");
		new DatabaseConfig().createTable("backwardTable");
		
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile("mysimulations/switzerland/input/network.xml");
				
		log.info("Loading Network ... done");
		
//		new CreateDijkstraSpanningTrees(network);
	}
	
	public CreateDijkstraSpanningTrees(NetworkLayer network)
	{	
		this.network = network;
		
		this.forwardExecution = new HashMap<Id, byte[]>();
		this.backwardExecution = new HashMap<Id, byte[]>();
		
		run();
	}
	
	private void run()
	{			
		Thread[] threads = new Thread[this.numOfThreads];
		CreateMapThread[] creationThreads = new CreateMapThread[numOfThreads];
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{
			DijkstraForSelectNodes dijkstra = new DijkstraForSelectNodes(this.network);
			dijkstra.setCostCalculator(costCalculator);

			CreateMapThread createKnowledgeThread = new CreateMapThread(i, dijkstra, this.useFloatPrecision, this.useCompression);
						
			creationThreads[i] = createKnowledgeThread;
			
			Thread thread = new Thread(createKnowledgeThread, "Thread#" + i);
			threads[i] = thread;
		}
		
		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Node node : this.network.getNodes().values())
		{
			creationThreads[i % numOfThreads].handleNode(node);
			i++;
		}
		
		// start the threads
		for (Thread thread : threads) 
		{
			thread.start();
		}
		
		// wait for the threads to finish
		try {
			for (Thread thread : threads) 
			{
				thread.join();
			}
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
		
		for (CreateMapThread creationThread : creationThreads) 
		{
			this.forwardExecution.putAll(creationThread.getForwardMap());
			this.backwardExecution.putAll(creationThread.getBackwardMap());
		}
		
	}
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class CreateMapThread implements Runnable 
	{
		public final int threadId;
		private DijkstraForSelectNodes dijkstra;
		private boolean useFloatPrecision;
		private boolean useCompression;
		
		private List<Node> nodes;
		private Map<Id, byte[]> forwardExecution;
		private Map<Id, byte[]> backwardExecution;
		
		private ByteArrayConverter bac;
		private Zipper zipper;
		private DBConnectionTool dbConnectionTool;
		
//		private String baseTableName = "MapKnowledge";
		private String forwardTableName = "forwardTable";
		private String backwardTableName = "backwardTable";
		
		public CreateMapThread(final int i, final DijkstraForSelectNodes dijkstra, boolean useFloatPrecision, boolean useCompression)
		{
			this.threadId = i;
			this.dijkstra = dijkstra;
			this.useFloatPrecision = useFloatPrecision;
			this.useCompression = useCompression;
			
			this.nodes = new LinkedList<Node>();
			this.forwardExecution = new HashMap<Id, byte[]>();
			this.backwardExecution = new HashMap<Id, byte[]>();
			
			this.bac = new ByteArrayConverter();
			this.zipper = new Zipper();
			this.dbConnectionTool = new DBConnectionTool();
		}
				
		public void handleNode(final Node node)
		{
			this.nodes.add(node);
		}

		public Map<Id, byte[]> getForwardMap()
		{
			return this.forwardExecution;
		}
		
		public Map<Id, byte[]> getBackwardMap()
		{
			return this.backwardExecution;
		}
		
		public void run()
		{
			int numRuns = 0;
			byte[] byteArray;
			
			for (Node node : this.nodes)
			{					
				this.dijkstra.executeForwardNetwork(node);			
				byteArray = createByteArray(node);
//				this.forwardExecution.put(node.getId(), byteArray);
				writeToDB(node, byteArray, this.forwardTableName);
				
				this.dijkstra.executeBackwardNetwork(node);
				byteArray = createByteArray(node);
//				this.backwardExecution.put(node.getId(), byteArray);
				writeToDB(node, byteArray, this.backwardTableName);
				
				numRuns++;
				if (numRuns % 500 == 0) log.info("created DijkstraMaps for " + numRuns + " nodes in thread " + threadId);
			
			}
			log.info("Thread " + threadId + " done.");		
		}	// run
		
		private byte[] createByteArray(Node node)
		{
			int networkSize = dijkstra.getMinDistances().size();	
			
			byte[] byteArray;
			if (useFloatPrecision)
			{
				int i = 0;
				float[] floatArray = new float[networkSize];
				for (double cost : dijkstra.getMinDistances().values())
				{
					floatArray[i] = (float)cost;
					i++;
				}
				byteArray = bac.toByteArray(floatArray);		
			}
			// else: we use double precision
			else
			{
				int i = 0;
				double[] doubleArray = new double[networkSize];
				for (double cost : dijkstra.getMinDistances().values())
				{
					doubleArray[i] = cost;
					i++;
				}
				byteArray = bac.toByteArray(doubleArray);
			}
			
			try 
			{
				
		        if (this.useCompression)
		        {
		        	byte[] compressedByteArray = zipper.compress(byteArray);
		        	return compressedByteArray;
		        }
		        else
		        {
		        	return byteArray;
		        }
			} 
			catch (Exception e) 
			{
				log.error(e.getMessage());
			}
			return null;
		}
		
		private void writeToDB(Node node, byte[] byteArray, String tableName)
		{	
//			String query = "INSERT INTO " + tableName + " SET NodeId='"+ node.getId() + "', SpanningTree='" + byteArray + "'";
//			
//			dbConnectionTool.connect();
//			dbConnectionTool.executeUpdate(query);
//			dbConnectionTool.disconnect();
			
			String query = "INSERT INTO " + tableName + " (NodeId, SpanningTree) VALUES (?, ?)";
			
			dbConnectionTool.connect();
			PreparedStatement pstmt = dbConnectionTool.getPreparedStatement(query);
	        try 
	        {
				pstmt.setString(1, node.getId().toString());
				pstmt.setBytes(2, byteArray);
		        pstmt.execute();
			} 
	        catch (SQLException e) 
	        {
	        	log.error("SQL Exception in writeToDB");
				e.printStackTrace();
			}
	        
	        dbConnectionTool.disconnect();
		}
		
	}	// CreateKnowledgeThread
	
	private static class DatabaseConfig 
	{
		private DBConnectionTool dbConnectionTool;
		private String forwardTableName = "forwardTable";
		private String backwardTableName = "backwardTable";
		
		public DatabaseConfig()
		{
			dbConnectionTool = new DBConnectionTool();
		}
		
		public void clearTable(String tableName)
		{
			if (tableExists(tableName)) 
			{			
				String clearTable = "DELETE FROM " + tableName;

				dbConnectionTool.connect();
				dbConnectionTool.executeUpdate(clearTable);
				dbConnectionTool.disconnect();
			}
		}
		
		public void createTable(String tableName)
		{			
			// If the Table doesn't exist -> create it.
			if (!tableExists(tableName))
			{
				String createTable = "CREATE TABLE " + tableName + " (NodeId VARCHAR(255), SpanningTree MEDIUMBLOB, PRIMARY KEY (NodeId))";
				
				dbConnectionTool.connect();
				dbConnectionTool.executeUpdate(createTable);
				dbConnectionTool.disconnect();
			}
		}

		public boolean tableExists(String tableName)
		{
			String tableExists = "SHOW TABLES LIKE '" + tableName + "'";
			
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
			
			return false;
		}
		
		
		public String getForwardTableName()
		{
			return forwardTableName;
		}

		public void setForwardTableName(String forwardTableName)
		{
			this.forwardTableName = forwardTableName;
		}

		public String getBackwardTableName()
		{
			return backwardTableName;
		}

		public void setBackwardTableName(String backwardTableName)
		{
			this.backwardTableName = backwardTableName;
		}
	}
}
