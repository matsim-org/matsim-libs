/******************************************************************************
 *project: org.matsim.*
 * NodeCost.java
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


package playground.rost.graph.shortestdistances;

import org.matsim.api.core.v01.network.Node;

public class NodeCost implements Comparable<NodeCost> {
	
	public Node node;
	public double cost;
	
	public NodeCost(Node node, double distance)
	{
		this.node = node;
		this.cost = distance;
	}
	
	public int compareTo(NodeCost other)
	{
		if(this.cost < other.cost)
			return -1;
		else if(this.cost == other.cost)
			return  0;
		else return 1;
	}
	
}
