/* *********************************************************************** *
 * project: org.matsim.*
 * BasicSelectNodesImpl.java
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

package playground.christoph.knowledge.nodeselection;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;


public class BasicSelectNodesImpl implements SelectNodes {

	NetworkLayer network;
 
	public void addNodesToMap(Map<Id, Node> nodeList)
	{
		// TODO Auto-generated method stub
	}

	public Map<Id, Node> getNodes()
	{
		// TODO Auto-generated method stub
		//return new ArrayList<Node>();
		return new TreeMap<Id, Node>();
	}

	public NetworkLayer getNetwork() 
	{
		return network;
	}

	public void setNetwork(NetworkLayer network) 
	{
		this.network = network;
	}

	@Override
	public SelectNodes clone()
	{
		BasicSelectNodesImpl clone = new BasicSelectNodesImpl();
		return clone;
	}

}