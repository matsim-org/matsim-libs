/******************************************************************************
 *project: org.matsim.*
 * PopulateBlocks.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.rost.graph.block.Block;
import playground.rost.graph.block.Blocks;
import playground.rost.graph.populationpoint.PopulationPoint;
import playground.rost.graph.populationpoint.PopulationPointCollection;

public class PopulateBlocks {
	
	public static Map<Node, Integer> nodePopulation = new HashMap<Node, Integer>();
	
	public synchronized static Map<Block, Integer> getPopulation(PopulationPointCollection popPointCollection, Blocks blocks, NetworkImpl network)
	{
		//calc min distance for every node
		for(Node node : network.getNodes().values())
		{
			calcDensityForNode(node, popPointCollection);
		}
		
		Map<Block, Integer> result = new HashMap<Block, Integer>();
		for(Block b : blocks.getBlocks())
		{
			result.put(b, calcDensityForBlock(b));
		}
		return result;
	}
	
	protected static void calcDensityForNode(Node node, PopulationPointCollection popPointCollection)
	{
		double minDistance = Double.MAX_VALUE;
		PopulationPoint bestPoint = null;
		for(PopulationPoint p : popPointCollection.get())
		{
			double dist = GraphAlgorithms.getDistanceMeter(p.point.x, p.point.y, node.getCoord().getX(), node.getCoord().getY());
			if(dist < minDistance)
			{
				minDistance = dist;
				bestPoint = p;
			}
		}
		if(bestPoint != null)
		{
			nodePopulation.put(node, bestPoint.population);
		}
		else
		{
			nodePopulation.put(node, 0);
		}	
	}
	
	protected static int calcDensityForBlock(Block b)
	{
		int averageNodeDensity = 0;
		for(Node n : b.border)
		{
			averageNodeDensity += nodePopulation.get(n);
		}
		averageNodeDensity = averageNodeDensity / b.border.size();
		return averageNodeDensity;
	}


}
