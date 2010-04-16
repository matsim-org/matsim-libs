/* *********************************************************************** *
 * project: org.matsim.*
 * SubNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.network;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

public class SubNetwork implements Network {

	protected Map<Id, Node> nodes;
	protected Map<Id, Link> links;

	protected Network network;
	public Person person;
	
	protected boolean isInitialized = false;

	public SubNetwork(Network network)
	{
		this.network = network;
	}

	public void initialize()
	{
		nodes = new TreeMap<Id, Node>();
		links = new TreeMap<Id, Link>();
	}

	public void initialize(int nodesCount)
	{
		nodes = new HashMap<Id, Node>((int)(nodesCount * 1.1), 0.95f);
		links = new TreeMap<Id, Link>();
	}

	public boolean isInitialized()
	{
		return isInitialized;
	}

	public void setInitialized()
	{
		this.isInitialized = true;
	}

	public void reset()
	{
//		this.nodes.clear();
//		this.links.clear();
		this.nodes = new HashMap<Id, Node>();
		this.links = new HashMap<Id, Link>();

		this.isInitialized = false;
	}

	@Override
	public NetworkFactory getFactory()
	{
		return null;
	}

	@Override
	public double getCapacityPeriod()
	{
		return network.getCapacityPeriod();
	}

	@Override
	public double getEffectiveLaneWidth()
	{
		return network.getEffectiveLaneWidth();
	}

	@Override
	public Map<Id, Link> getLinks()
	{
		return links;
	}

	@Override
	public Map<Id, Node> getNodes()
	{
		return nodes;
	}

	@Override
	public void addNode(Node node)
	{
		nodes.put(node.getId(), node);
	}

	@Override
	public void addLink(Link link)
	{
		links.put(link.getId(), link);
	}

	@Override
	public Link removeLink(Id linkId) {
		return links.remove(linkId);
	}

	@Override
	public Node removeNode(Id nodeId) {
		return nodes.remove(nodeId);
	}

}
