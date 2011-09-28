/* *********************************************************************** *
 * project: org.matsim.*
 * LookupNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.router.traveltime;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

/**
 * @author cdobler
 */
public class LookupNetwork implements Network {

	private Map<Id, LookupNetworkNode> nodes = new HashMap<Id, LookupNetworkNode>();
	private Map<Id, LookupNetworkLink> links = new HashMap<Id, LookupNetworkLink>();
	private final Network network;
		
	public LookupNetwork(Network network) {
		this.network = network;
	}
	
	@Override
	public NetworkFactory getFactory() {
		return network.getFactory();
	}

	@Override
	public Map<Id, LookupNetworkNode> getNodes() {
		return nodes;
	}

	public void addNode(LookupNetworkNode node) {
		this.nodes.put(node.getId(), node);
	}

	@Override
	public LookupNetworkNode removeNode(Id nodeId) {
		return nodes.remove(nodeId);
	}

	public void addLink(LookupNetworkLink node) {
		this.links.put(node.getId(), node);
	}

	public LookupNetworkLink removeLink(Id linkId) {
		return links.remove(linkId);
	}
	
	@Override
	public void addLink(Link ll) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void addNode(Node nn) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getCapacityPeriod() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getEffectiveLaneWidth() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id, LookupNetworkLink> getLinks() {
		return links;
	}
}