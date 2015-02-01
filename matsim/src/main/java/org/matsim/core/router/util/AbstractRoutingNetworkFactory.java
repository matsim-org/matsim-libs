/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoutingNetworkFactory.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public abstract class AbstractRoutingNetworkFactory implements RoutingNetworkFactory {

	protected final PreProcessDijkstra preProcessData;
	
	public AbstractRoutingNetworkFactory(PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}
	
	@Override
	public Link createLink(Id<Link> id, Id<Node> fromNodeId, Id<Node> toNodeId) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Link createLink(Id<Link> id, Node fromNode, Node toNode) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public RoutingNetworkNode createNode(Id<Node> id, Coord coord) {
		throw new RuntimeException("Not supported operation!");
	}

}