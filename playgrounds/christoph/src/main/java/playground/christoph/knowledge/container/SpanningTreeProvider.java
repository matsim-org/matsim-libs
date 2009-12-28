/* *********************************************************************** *
 * project: org.matsim.*
 * SpanningTreeProvider.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.christoph.tools.ByteArrayConverter;

/*
 * Administrates the Spanning Trees that are created by using
 * the Dijsktra Node Selection Tool. The Spanning Tree data
 * can be kept in memory (typically huge amount of data!) or
 * read from the DataBase each time.
 * 
 * The Cloneable interface is implemented. Cloned Objects use
 * the same Data arrays what should be no problem because
 * the are used read only.
 */
public class SpanningTreeProvider implements Cloneable {

	private final static Logger log = Logger.getLogger(SpanningTreeProvider.class);
	
	private double[][] forwardCostDoubleArray = null;
	private double[][] backwardCostDoubleArray = null;
	
	private float[][] forwardCostFloatArray = null;
	private float[][] backwardCostFloatArray = null;
	
	private Network network;
	private List<Node> nodes;
	private ReadSpanningTreeFromDB dbReader;
	private ByteArrayConverter bac;
	
	private String forwardTableName = "forwardTable";
	private String backwardTableName = "backwardTable";
		
	/*
	 * Should float precision instead of double precision for the calculation
	 * of the trip costs be used? Saves half of the memory and should not 
	 * change the results significantly.
	 */
	private boolean useFloatPrecision = true;
	
	
	// if we have all Data local in Memory - take it from there, else read it from the Database.
	private boolean localData = false;
	
	
	public SpanningTreeProvider(Network network)
	{
		this.network = network;
	
		nodes = new ArrayList<Node>();
		if (network != null)
		{
			for (Node node : network.getNodes().values()) nodes.add(node);
		}
		
		dbReader = new ReadSpanningTreeFromDB();
		bac = new ByteArrayConverter();
	}
	
	/*
	 * Gets all SpanningTrees from the Database and keeps them in Memory.
	 */
	public void createCostArray()
	{		
		int nodesCount = this.network.getNodes().size();
		
		if (useFloatPrecision)
		{
			forwardCostFloatArray = new float[nodesCount][];
			backwardCostFloatArray = new float[nodesCount][];
			
			int i = 0;
			for (Node node : network.getNodes().values())
			{
				forwardCostFloatArray[i] = bac.toFloatArray(dbReader.readFromDB(node, forwardTableName));
				backwardCostFloatArray[i] = bac.toFloatArray(dbReader.readFromDB(node, backwardTableName));
				
				i++;
				
				if (i % 1000 == 0) log.info("read " + i + " Spanning Tree Cost Arrays from Database");
			}
		}
		// we use double precision
		else
		{
			forwardCostDoubleArray = new double[nodesCount][];
			backwardCostDoubleArray = new double[nodesCount][];
			
			int i = 0;
			for (Node node : network.getNodes().values())
			{
				forwardCostDoubleArray[i] = bac.toDoubleArray(dbReader.readFromDB(node, forwardTableName));
				backwardCostDoubleArray[i] = bac.toDoubleArray(dbReader.readFromDB(node, backwardTableName));
				
				i++;
				
				if (i % 1000 == 0) log.info("read " + i + " Spanning Tree Cost Arrays from Database");
			}
		}
		
		localData = true;
	}
	
	public Map<Node, Double> getTotalSpanningTree(Node startNode, Node endNode)
	{
		if (localData)
		{
			if (useFloatPrecision)
			{
				float[] forwardCosts = this.forwardCostFloatArray[this.nodes.indexOf(startNode)];
				float[] backwardCosts = this.backwardCostFloatArray[this.nodes.indexOf(endNode)];
				
				int nodesCount = this.network.getNodes().size();
				float[] sumCosts = new float[nodesCount];
				for (int i = 0; i < nodesCount; i++)
				{
					sumCosts[i] = forwardCosts[i] + backwardCosts[i];
				}
				
				return createMapFromFloatArray(sumCosts);
			}
			else
			{
				double[] forwardCosts = this.forwardCostDoubleArray[this.nodes.indexOf(startNode)];
				double[] backwardCosts = this.backwardCostDoubleArray[this.nodes.indexOf(endNode)];
				
				int nodesCount = this.network.getNodes().size();
				double[] sumCosts = new double[nodesCount];
				for (int i = 0; i < nodesCount; i++)
				{
					sumCosts[i] = forwardCosts[i] + backwardCosts[i];
				}
				
				return createMapFromDoubleArray(sumCosts);
			}
		}
		
		// get Data directly from the DataBase
		else
		{
			if (useFloatPrecision)
			{
				float[] forwardCosts = bac.toFloatArray(dbReader.readFromDB(startNode, forwardTableName));
				float[] backwardCosts = bac.toFloatArray(dbReader.readFromDB(endNode, backwardTableName));
				
				int nodesCount = this.network.getNodes().size();
				float[] sumCosts = new float[nodesCount];
				for (int i = 0; i < nodesCount; i++)
				{
					sumCosts[i] = forwardCosts[i] + backwardCosts[i];
				}
				
				return createMapFromFloatArray(sumCosts);
			}
			else
			{
				double[] forwardCosts = bac.toDoubleArray(dbReader.readFromDB(startNode, forwardTableName));
				double[] backwardCosts = bac.toDoubleArray(dbReader.readFromDB(endNode, backwardTableName));
				
				int nodesCount = this.network.getNodes().size();
				double[] sumCosts = new double[nodesCount];
				for (int i = 0; i < nodesCount; i++)
				{
					sumCosts[i] = forwardCosts[i] + backwardCosts[i];
				}
				
				return createMapFromDoubleArray(sumCosts);
			}
		}
	}
	
	public Map<Node, Double> getForwardSpanningTree(Node node)
	{	
		if (localData)
		{
			if (useFloatPrecision)
			{
				float[] costs = this.forwardCostFloatArray[this.nodes.indexOf(node)];
				
				return createMapFromFloatArray(costs);
			}
			else
			{
				double[] costs = this.forwardCostDoubleArray[this.nodes.indexOf(node)];
				
				return createMapFromDoubleArray(costs);
			}
		}
		
		// get Data directly from the DataBase
		else
		{
			if (useFloatPrecision)
			{
				float[] costs = bac.toFloatArray(dbReader.readFromDB(node, forwardTableName));
				
				return createMapFromFloatArray(costs);
			}
			else
			{
				double[] costs = bac.toDoubleArray(dbReader.readFromDB(node, forwardTableName));
				
				return createMapFromDoubleArray(costs);
			}
		}
	}
	
	public Map<Node, Double> getBackwardSpanningTree(Node node)
	{	
		if (localData)
		{
			if (useFloatPrecision)
			{
				float[] costs = this.backwardCostFloatArray[this.nodes.indexOf(node)];
				
				return createMapFromFloatArray(costs);
			}
			else
			{
				double[] costs = this.backwardCostDoubleArray[this.nodes.indexOf(node)];
				
				return createMapFromDoubleArray(costs);
			}
		}
		
		// get Data directly from the DataBase
		else
		{
			if (useFloatPrecision)
			{
				float[] costs = bac.toFloatArray(dbReader.readFromDB(node, backwardTableName));
				
				return createMapFromFloatArray(costs);
			}
			else
			{
				double[] costs = bac.toDoubleArray(dbReader.readFromDB(node, backwardTableName));
				
				return createMapFromDoubleArray(costs);
			}
		}
	}
	
	private Map<Node, Double> createMapFromDoubleArray(double[] costs)
	{		
		Map<Node, Double> map = new HashMap<Node, Double>();
		
		int i = 0;
		for (Node node : network.getNodes().values())
		{
			map.put(node, costs[i]);
			i++;
		}
				
		return map;
	}
	
	private Map<Node, Double> createMapFromFloatArray(float[] costs)
	{		
		Map<Node, Double> map = new HashMap<Node, Double>();
		
		int i = 0;
		for (Node node : network.getNodes().values())
		{
			map.put(node, (double) costs[i]);
			i++;
		}	
		
		return map;
	}
	
	public void useFloatPrecision(boolean value)
	{
		// If the values has changed reset local data if available.
		if (this.useFloatPrecision != value)
		{
			this.forwardCostDoubleArray = null;
			this.backwardCostDoubleArray = null;
			
			this.forwardCostFloatArray = null;
			this.backwardCostFloatArray = null;
			
			this.useFloatPrecision = value;
		}
	}
	
	public boolean useFloatPrecision()
	{
		return this.useFloatPrecision;
	}
	
	public void useLocalData(boolean value)
	{
		if (!value)
		{
			this.forwardCostDoubleArray = null;
			this.backwardCostDoubleArray = null;
			
			this.forwardCostFloatArray = null;
			this.backwardCostFloatArray = null;
		}
		
		this.localData = value;
	}
	
	@Override
	public SpanningTreeProvider clone()
	{
		SpanningTreeProvider stp = new SpanningTreeProvider(this.network);

		stp.forwardCostFloatArray = this.forwardCostFloatArray;
		stp.backwardCostFloatArray = this.backwardCostFloatArray;

		stp.forwardCostDoubleArray = this.forwardCostDoubleArray;
		stp.backwardCostDoubleArray = this.backwardCostDoubleArray;
		
		stp.forwardTableName = this.forwardTableName;
		stp.backwardTableName = this.backwardTableName;

		stp.useFloatPrecision = this.useFloatPrecision;

		stp.localData = this.localData;
			
		return stp;
	}
}
