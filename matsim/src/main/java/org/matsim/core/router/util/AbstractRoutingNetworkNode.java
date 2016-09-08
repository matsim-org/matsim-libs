/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoutingNetworkNode.java
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

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.PreProcessDijkstra.DeadEndData;
import org.matsim.utils.objectattributes.attributable.Attributes;

public abstract class AbstractRoutingNetworkNode implements RoutingNetworkNode {

	private final Node node;
	private final RoutingNetworkLink[] outLinks;
	
	private DeadEndData deadEndData;
	
	/*
	 * We could get the number of out-links from the node. However,
	 * if some of those links should not be part of the routing network
	 * this would fail.
	 */
	/*package*/ AbstractRoutingNetworkNode(Node node, int numOutLinks) {
		this.node = node;
		this.outLinks = new RoutingNetworkLink[numOutLinks];
	}
	
	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public void setOutLinksArray(RoutingNetworkLink[] outLinks) {
        System.arraycopy(outLinks, 0, this.outLinks, 0, outLinks.length);
	}
	
	@Override
	public RoutingNetworkLink[] getOutLinksArray() {
		return this.outLinks;
	}

	@Override
	public void setDeadEndData(DeadEndData deadEndData) {
		this.deadEndData = deadEndData;
	}
	
	@Override
	public DeadEndData getDeadEndData() {
		return this.deadEndData;
	}

	@Override
	public Id<Node> getId() {
		return node.getId();
	}

	@Override
	public Coord getCoord() {
		return node.getCoord();
	}

	@Override
	public boolean addInLink(Link link) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public boolean addOutLink(Link link) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		throw new RuntimeException("Not supported operation!");
	}


	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Attributes getAttributes() {
		return node.getAttributes();
	}
}