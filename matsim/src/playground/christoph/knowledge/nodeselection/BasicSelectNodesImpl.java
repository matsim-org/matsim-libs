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

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public abstract class BasicSelectNodesImpl implements SelectNodes {

	NetworkLayer network;
 
	public abstract void addNodesToMap(Map<Id, NodeImpl> nodeList);
	
	public Map<Id, NodeImpl> getNodes()
	{
		return new TreeMap<Id, NodeImpl>();
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
	public abstract SelectNodes clone();

}