/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoutingNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.router.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.utils.objectattributes.attributable.Attributes;

public abstract class AbstractRoutingNetwork implements RoutingNetwork {
	
	/*package*/ final Map<Id<Node>, RoutingNetworkNode> nodes = new LinkedHashMap<>();	// needs to be a LinkedHashMap since the order is relevant for the router-preprocessing!
	/*package*/ final Network network;

	public AbstractRoutingNetwork(Network network) {
		this.network = network;
	}
	
	@Override
	public void initialize() {
		// Some classes might override this method and do some additional stuff...
	}
	
	@Override
	public NetworkFactory getFactory() {
		return network.getFactory();
	}

	@Override
	public Map<Id<Node>, RoutingNetworkNode> getNodes() {
		return nodes;
	}

	public void addNode(RoutingNetworkNode nn) {
		this.nodes.put(nn.getId(), nn);
	}
	
	@Override
	public RoutingNetworkNode removeNode(Id<Node> nodeId) {
		return nodes.remove(nodeId);
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
	public Map<Id<Link>, ? extends Link> getLinks() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Link removeLink(Id<Link> linkId) {
		throw new RuntimeException("Not supported operation!");
	}
	@Override
	public void setCapacityPeriod(double capPeriod) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveCellSize(double effectiveCellSize) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public double getEffectiveCellSize() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		return this.network.getAttributes();
	}
}